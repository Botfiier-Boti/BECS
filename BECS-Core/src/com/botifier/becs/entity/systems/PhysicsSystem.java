package com.botifier.becs.entity.systems;

import static com.botifier.becs.entity.EntityComponentManager.hasComponent;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.glfw.GLFW;

import com.botifier.becs.Game;
import com.botifier.becs.config.ObjectConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntitySystem;
import com.botifier.becs.entity.systems.physics.PhysicsSystemExtension;
import com.botifier.becs.util.CollisionUtil;
import com.botifier.becs.util.Math2;
import com.botifier.becs.util.ParameterizedRunnable;
import com.botifier.becs.util.SpatialPolygonHolder;
import com.botifier.becs.util.maps.SpatialEntityMap;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;
import com.botifier.becs.util.shapes.Shape;

/**
 * PhysicsSystem
 * 
 * TODO: Improve documentation formatting
 * TODO: Optimize this
 * 
 * @author Botifier
 */
public class PhysicsSystem extends EntitySystem {
    
	/**
	 * Config name of gravity
	 */
	private static final String GRAVITY_CONFIG = "gravity";
	/**
	 * Config name of the gravity level
	 */
    private static final String GRAVITY_LEVEL_CONFIG = "gravity_level";
    /**
     * Config name of the precise mode option
     */
    private static final String PRECISE_MODE_CONFIG = "precise_mode";
    /**
     * Config name of the stagger mode option
     */
    private static final String STAGGER_MODE_CONFIG = "stagger_mode";
    /**
     * Config name of the smoothing factor
     */
    private static final String SMOOTHING_FACTOR_CONFIG = "smoothing_factor";
    /**
     * Config name of the physics epsilon
     */
    private static final String PHYSICS_EPSILON_CONFIG = "epsilon";
    /**
     * Default snap delay for SnappyComponent
     */
    private static final long SNAP_DELAY = 50;

    /**
     * Whether or not the physics system is current running
     * Stored atomically
     */
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    /**
     * List of physics extensions
     */
    public List<PhysicsSystemExtension> pses = new ArrayList<>();

    /**
     * The current physics tick
     * Great for timing
     */
    private AtomicLong physicsTick = new AtomicLong(0);


    /**
     * Physics System constructor
     */
	public PhysicsSystem(Game g) {
		super(g, "Position", "Velocity", "PhysicsEnabled");
		//Initializes the physics config
		initPhysicsConfig();
	}
	
	@Override
	public CompletableFuture<Void> apply(Entity[] entities) {
		if ((getConfig().getBoolean(STAGGER_MODE_CONFIG) && !Game.getCurrent().getInput().isKeyPressed(GLFW.GLFW_KEY_SPACE)) || isPaused()) {
			return CompletableFuture.completedFuture(null);
		}
		
		if (!running.get())
			return CompletableFuture.completedFuture(null);
		
		try (ExecutorService ex = Executors.newCachedThreadPool()){
			final SpatialEntityMap sem = Entity.spatialMap();
		    
			List<Entity> awakeEntities = Arrays.stream(entities)
			        .filter(e -> sem.isAwake(e))
			        .collect(Collectors.toList());
			
			final int BATCH_SIZE = 10000; // Adjust based on your performance needs
			
			//Gets the epsilon
			final float EPSILON = getConfig().getFloat(PHYSICS_EPSILON_CONFIG);
		    
		    // Split entities into batches and process in parallel
		    List<CompletableFuture<Void>> futures = IntStream.range(0, (awakeEntities.size() + BATCH_SIZE - 1) / BATCH_SIZE)
		    	.parallel()
		        .mapToObj(i -> {
		            int startIndex = i * BATCH_SIZE;
		            int endIndex = Math.min(startIndex + BATCH_SIZE, awakeEntities.size());
		            return awakeEntities.subList(startIndex, endIndex);
		        })
		        .map(batch -> CompletableFuture.runAsync(() -> {
		            if (!running.get()) return;
		            
		            List<Entity> batchMovedList = new ArrayList<>();
		            for (Entity entity : batch) {
		                updateEntity(entity, batchMovedList, EPSILON);
		            }
		            // Update spatial map for this batch immediately
		            if (!batchMovedList.isEmpty()) {
		                Entity.spatialMap().updateEntitiesInSequence(batchMovedList);
		            }
		        }, ex))
		        .collect(Collectors.toList());

			//Proceed to the next physics tick
			physicsTick.incrementAndGet();

		    // Wait for all futures to complete
		    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		}
	}

	/**
	 * Updates supplied entity
	 * checks for collisions against movedList
	 * @param e Entity To check
	 * @param movedList List\<Entity\> To check against
	 */
	public void updateEntity(final Entity e, final List<Entity> movedList, final float EPSILON) {
		//Obtain the Position and Velocity components
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");
		
		if (velComponent == null || posComponent == null) {
			System.out.println("Somehow a component is missing.");
			System.out.println(e.getComponents());
			return;
		}
		
		//Obtain the data from the components
		Vector2f p = posComponent.get();
		Vector2f v = new Vector2f(velComponent.get());

		//Perform pre ticks from the physics extenstions
		for (PhysicsSystemExtension pse : pses) {
			pse.preTick(e, movedList);
		}

		//Checks if the Entity is both Collidable and has a CollisionShape
		EntityComponent<Shape> shaComponent = e.getComponent("CollisionShape");
		boolean collidable = e.getComponentValueOrDefault("Collidable", false);
		if (shaComponent != null && collidable) {
			//Obtains the entity's shape
			Shape s = shaComponent.get();
			//Converts the shape into a polygon
			Polygon check = s.toPolygon();
			//Creates a Polygon representing all possible locations that the entity could be based on velocity
			Polygon collideCheck = check.mergeNoRepeat(check.move(new Vector2f(v.x, v.y)));
			
			//Obtain all nearby entities

			Entity[] entities = Entity.spatialMap().getEntitiesIn(collideCheck, true).toArray(Entity[]::new);
			
			//If there is at least 1 entity perform collision handling and then update velocity
			if (entities.length >= 1) 
				v.set(Math2.round(v.add(handleCollision(e, s, collideCheck, entities, EPSILON)), 2));
		} else {
			Vector2f accel = e.getComponentValueOrDefault("Acceleration", new Vector2f());
			v.set(Math2.round(new Vector2f(v).add(accel), 2));
		}

		boolean moved = handleNormalMovment(e, v, EPSILON);

		//Updates the collision shape of entities that have them
		if (shaComponent != null && moved) {
			//Gets the entity's shape component
			//Tracking if the shape has been updated
			boolean shapeUpdated = false;
			
			//Check if the shapeComponent is null, sort of redundant
			if (shaComponent != null) {
				//Obtains the shape if it isn't null
				Shape s = shaComponent.get();
				
				//Updates p to the latest position
				p = posComponent.get();
				
				//Checks if the center of the shape's distance is greater than a leniency value
				if (moved) {
					//Locates the entity's locations in the map
					SpatialPolygonHolder sph = Entity.spatialMap().locate(e);
					
					//If the locations aren't null and the hashes match, the entity had moved
					if (sph != null &&
					    !sph.matches(Entity.spatialMap().gridifyPolygon(s.toPolygon()).getHashes()))
						shapeUpdated = true;
					
				}
				
				//Update the p variable to the latest position
				p = posComponent.get();
				//Clone the shape
				Shape mod = s.clone();
				//Move the shape to p
				mod.setCenter(p.x, p.y);
				//Update the shape component
				shaComponent.set(mod);
			}
			//Updates the moved list if the entity moved
			if (shapeUpdated) {
				movedList.add(e);
			}
	    }

		//Runs the post ticks of the physics system extensions
		for (PhysicsSystemExtension pse : pses) {
			pse.postTick(e, movedList);
		}
	}

	/**
	 * Performs collision resolution for the supplied entity
	 * TODO: add InteractableComponent support
	 * @param e Entity to use
	 * @param entities Entities to check against
	 */
	public Vector2f handleCollision(final Entity e, final Shape s, final Polygon collideCheck, final Entity[] entities, final float EPSILON) {
		
		//Obtains the entity's position and velocity components
		final EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");
		final EntityComponent<Vector2f> posComponent = e.getComponent("Position");

		//Obtains the information inside them so it can't mutate on us
		Vector2fc p = posComponent.get();
		Vector2fc v = velComponent.get();

		//Velocity adjustment variable
		Vector2f fullVelAdj = new Vector2f(0, 0);
		
		//If the entity has an acceleration, add it to the velocity adjustment
		if (hasComponent(e, "Acceleration")) {
			EntityComponent<Vector2f> accComponent = e.getComponent("Acceleration");
			fullVelAdj.add(accComponent.get());
		}

		//Don't do anything if the entity isn't moving.
		if (v.length() == 0 && fullVelAdj.length() < 0) {
			return (Vector2f) v;
		}

		//Internalize the collideCheck
		Polygon collider = collideCheck.clone();
		
		//Convert the entity's supplied shape to a polygon
		Polygon move = s.toPolygon();

		//Filter out all invalid targets from the entities array
		Entity[] entitiesInternal = Arrays.stream(entities).filter(e2 -> validCollisionEntity(e, e2)).toArray(Entity[]::new);
		
		//Sort the entities array by distance from the entity
		Arrays.sort(entitiesInternal, (a, b) -> {

			EntityComponent<Vector2f> posAComponent = a.getComponent("Position");
			EntityComponent<Vector2f> posBComponent = b.getComponent("Position");
			Vector2f posA = posAComponent.get();
			Vector2f posB = posBComponent.get();

			float distA = p.distanceSquared(posA);
			float distB = p.distanceSquared(posB);

			return Float.compare(distA, distB);
		});
		
		

		//Start checking entities for collision
		for (Entity e2 : entitiesInternal) {
			//Local adjustment
			Vector2f velAdj = new Vector2f(0);

			EntityComponent<Shape> e2ShapeComponent = e2.getComponent("CollisionShape");

			Shape secondaryCollisionShape = e2ShapeComponent.get();

			//Check if there is an intersection and that the primary entity is moving
			CollisionUtil.PolygonOutput pOutput = secondaryCollisionShape.toPolygon().intersectsSAT(collider);
			if (pOutput != null) {
				float dist = secondaryCollisionShape.closestTo(p).distance(p);
				
				if (v.length() > dist) {
					Vector2f use = new Vector2f(v).add(fullVelAdj);
					
					fullVelAdj.sub(use.sub(use.normalize(dist, new Vector2f()), new Vector2f()));
					
					
					move = s.toPolygon().move(new Vector2f(velComponent.get()).add(fullVelAdj));
					//Update the predictive shape
					collider = s.toPolygon().mergeNoRepeat(move);
					pOutput = secondaryCollisionShape.toPolygon().intersectsSAT(collider);
					
					if (pOutput == null)
						continue;
				}
				
				Vector2f n = pOutput.getNormal().mul(pOutput.getDepth());
				velAdj.sub(n);
				performInteraction(e, e2);

				//Only actually adjusts if it is solid and the magnitude of the modification is not zero
				if (e2.getComponentValueOrDefault("Solid", false) &&
					velAdj.length() > EPSILON) {
					
					//Add the adjustment to the full adjustment
					fullVelAdj.add(velAdj.negate());

					//Calculate magnitudes
					float mag = v.length();
					float mag2 = fullVelAdj.length();

					//Reduces fullVejAdj's magnitude if it is too high
					//Prevents bouncing backwards, sort of.
					if (mag2 > mag) {
						fullVelAdj.normalize(mag);
					}

					//Update the location shape
					move = s.toPolygon().move(new Vector2f(velComponent.get()).add(fullVelAdj));
					//Update the predictive shape
					collider = s.toPolygon().mergeNoRepeat(move);
				}
			}
		}
		
		//Calculate magnitudes
		float mag = v.length();
		float mag2 = fullVelAdj.length();

		//Reduces fullVejAdj's magnitude if it is too high
		//Prevents bouncing backwards, sort of.
		if (mag2 > mag) {
			fullVelAdj.normalize(mag);
		}
		
		//Round the adjustment
		fullVelAdj = Math2.round(fullVelAdj, 2);
		return fullVelAdj;
	}

	private boolean validCollisionEntity(final Entity e, final Entity e2) {
		//If the target entity is the null or the same as the origin, it isn't valid
		if ((e2 == null) || (e2 == e) || (e2.getUUID() == e.getUUID())) {
			return false;
		}
		//If the target entity lacks the proper collision components, it isn't valid
		if (!hasComponent(e2, "CollisionShape") || !hasComponent(e2, "Collidable")
			|| !hasComponent(e2, "Position")) {
			return false;
		}
		
		//If the target entity isn't solid, its not valid unless, it is interactable
		if ((!hasComponent(e2, "Solid") || !((boolean)(e2.getComponent("Solid").get()))) && !hasComponent(e2, "Interactable")) {
			return false;
		}
		
		//If the origin has the IgnoreWith component, check if the entity should be ignored
		if (hasComponent(e, "IgnoreWith")) {
			//Breaking down the ignore list because it is stored in a single string
			//Not an array
			//Is this a good idea? Probably not.
			EntityComponent<String> checkCom = e.getComponent("IgnoreWith");
			String[] ignores = checkCom.get().split(",");
			boolean skip = false;
			for (String ignore : ignores) {
				ignore = ignore.trim();
				if (hasComponent(e2, ignore)) {
					skip = true;
					break;
				}
			}
			if (skip) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Handles the movement of entity's with the snappy component
	 * TODO: Update this
	 * @param e Entity To use
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void handleSnappyMovment(Entity e) {
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");
		EntityComponent<Long> snappyComponent = e.getComponent("Snappy");

		Vector2f p = posComponent.get();
		Vector2f v = velComponent.get();
		//Handles snap based movement
		long nextMove = snappyComponent.get();

		p.set((int)(p.x/32)*32, (int)(p.y/32)*32);
		
		if (nextMove <= getPhysicsTick()) {
			if (hasComponent(e, "CollisionShape")) {
				EntityComponent<Shape> shaComponent = e.getComponent("CollisionShape");
				RotatableRectangle rr = (RotatableRectangle) shaComponent.get();
				rr.setCenter(p.x + v.x + rr.getHeight() / 2, p.y + v.y + rr.getHeight() / 2);
			}
			p.add(v.x, v.y);
			snappyComponent.set(getPhysicsTick() + SNAP_DELAY);
		}

		if (v.x < 32 && v.x > 0) {
			v.setComponent(0, 32); // x
		}
		if (v.y < 32 && v.y > 0) {
			v.setComponent(1, 32); // y
		}
		if (v.x > -32 && v.x < 0) {
			v.setComponent(0, -32); // x
		}
		if (v.y > -32 && v.y < 0) {
			v.setComponent(0, -32); // y
		}

		if (v.length() > 0) {
			v.mul(0);
		}

	}


	/**
	 * Performs an interaction between entities with interactable components
	 * @param primary Entity To interact
	 * @param secondary Entity To also interact
	 * @return
	 */
	private boolean performInteraction(final Entity primary, final Entity secondary) {
		if (!hasComponent(primary, "Interactable") || !hasComponent(secondary, "Interactable")) {
			return false;
		}
		
		//Obtains the interactable components
		final EntityComponent<ParameterizedRunnable<Entity>> pr = primary.getComponent("Interactable");
		final EntityComponent<ParameterizedRunnable<Entity>> sc = secondary.getComponent("Interactable");
		
		//Runs primary interaction if it isn't null
		if (pr.get() != null) {
			pr.get().run(primary, secondary);
		}
		//Runs secondary interaction if it isn't null
		if (sc.get() != null) {
			sc.get().run(primary, secondary);
		}
		return true;
	}

	/**
	 * Finalizes normal movement
	 * @param e Entity to finalize
	 */
	private boolean handleNormalMovment(final Entity e, final Vector2f v, final float EPSILON) {
		
		//Obtains the entity's position and velocity components
		final EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		final EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");

		//Obtains the values of the position and velocity components
		Vector2fc cP = posComponent.get();

		Vector2f p = new Vector2f(cP);
		
		//Sets velocity to zero if it has a magnitude below the EPSILON
		if (Math.abs(v.x()) < EPSILON) {
			v.set(0, v.y());
			velComponent.set(v);
		}
		if (Math.abs(v.y()) < EPSILON) {
			v.set(v.x(), 0);
			velComponent.set(v);
		}

		//Put the entity to sleep if it isn't moving
		if (v.length() == 0 && !e.hasComponent("acceleration")) {
			Entity.spatialMap().sleepEntity(e);
			return false;
		}
		//Normal movement
		p.add(v);
		posComponent.set(Math2.round(p, 2));
		
		//Tracks the boolean facing direction of the entity
		if (hasComponent(e, "BooleanDirection")) {
			EntityComponent<Boolean> boolComponent = e.getComponent("BooleanDirection");
			boolean b = boolComponent.get();
			if (!b) {
				if (v.x > 0) {
					b = true;
				}
			} else if (b) {
				if (v.x < 0) {
					b = false;
				}
			}
			boolComponent.set(b);
		}

		//So entities slide into place instead of abruptly stopping
		if (v.length() > 0) {
			final Vector2f multiplied = v.mul(getConfig().getFloat(SMOOTHING_FACTOR_CONFIG));
			final Vector2f rounded = Math2.round(multiplied, 2);
			velComponent.set(rounded);
		}
		return true;
	}

	/**
	 * Adds a PhysicsSystemExtension to the physics system
	 * @param pse
	 */
	public void addPhysicsExtension(PhysicsSystemExtension pse) {
		pses.add(pse);
	}

	/**
	 * Initializes the Physics config
	 */
	public void initPhysicsConfig() {
		
		ObjectConfig conf = ObjectConfig.loadOrCreateFromFile("physics-config.json");
		getGame().addConfig("PhysicsConfig", conf);
		
		getConfig().putIfAbsent(GRAVITY_CONFIG, false);
		getConfig().putIfAbsent(GRAVITY_LEVEL_CONFIG, 0.98f);
		getConfig().putIfAbsent(PRECISE_MODE_CONFIG, true);
		getConfig().putIfAbsent(STAGGER_MODE_CONFIG, false);
		getConfig().putIfAbsent(SMOOTHING_FACTOR_CONFIG, 0.75f);
		getConfig().putIfAbsent(PHYSICS_EPSILON_CONFIG, 0.001f);
		System.out.println(getConfig());
	}

	public ObjectConfig getConfig() {
		return getGame().<ObjectConfig>getConfig("PhysicsConfig");
	}
	
	/**
	 * Returns the current physics tick
	 * Good for cooldowns
	 * @return long Current tick
	 */
	public long getPhysicsTick() {
		return physicsTick.get();
	}

	/**
	 * Gets all of the physics system extensions
	 * @return
	 */
	public List<PhysicsSystemExtension> getExtensions() {
		return pses;
	}

	@Override
	public void destroy() {
		System.out.println("Writing Physics Config...");
		getConfig().writeFile("physics-config.json");
		running.set(false);
	}
}
