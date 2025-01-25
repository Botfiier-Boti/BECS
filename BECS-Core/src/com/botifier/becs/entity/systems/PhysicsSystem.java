package com.botifier.becs.entity.systems;

import static com.botifier.becs.entity.EntityComponentManager.hasComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.glfw.GLFW;

import com.botifier.becs.Game;
import com.botifier.becs.config.PhysicsConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntitySystem;
import com.botifier.becs.entity.systems.physics.PhysicsSystemExtension;
import com.botifier.becs.util.CollisionUtil;
import com.botifier.becs.util.Math2;
import com.botifier.becs.util.ParameterizedRunnable;
import com.botifier.becs.util.SpatialPolygonHolder;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;
import com.botifier.becs.util.shapes.Shape;

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
     * Default snap delay for SnappyComponent
     */
    private static final long SNAP_DELAY = 50;

    private final AtomicBoolean running = new AtomicBoolean(true);
    
    public List<PhysicsSystemExtension> pses = new ArrayList<>();

    private long physicsTick = 0;

    /**
     * Physics System constructor
     */
	public PhysicsSystem(Game g) {
		super(g, "Position", "Velocity", "PhysicsEnabled");
		//Initializes the physics config
		initPhysicsConfig();
	}

	@Override
	public void apply(Entity[] entities) {
		if ((PhysicsConfig.getBoolean(STAGGER_MODE_CONFIG) && !Game.getCurrent().getInput().isKeyPressed(GLFW.GLFW_KEY_SPACE)) || isPaused()) {
			return;
		}
		//pause();
		List<Entity> movedList = new CopyOnWriteArrayList<>();
		//for (int i = 0; i < entities.length; i++)
			//updateEntity(entities[i], movedList);
		Stream<Entity> es = Entity.spatialMap().getAwake().stream().map(e -> Entity.getEntity(e));
		List<CompletableFuture<Void>> futures = es
				.parallel()
				.map(i -> CompletableFuture.runAsync(() -> {
					
					if (running.get())
						updateEntity(i, movedList);

				}))
				.collect(Collectors.toList());
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
		.thenRun(() -> {
			if (running.get())
				Entity.spatialMap().updateEntitiesInParalell(movedList);
		});
		allOf.join();
		//movedList.clear();
		//System.out.println(movedList.size());

		physicsTick++;
	}

	/**
	 * Updates supplied entity
	 * checks for collisions against movedList
	 * @param e Entity To check
	 * @param movedList List\<Entity\> To check against
	 */
	public void updateEntity(Entity e, List<Entity> movedList) {
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");

		Vector2f p = posComponent.get();
		Vector2f v = velComponent.get();

		for (PhysicsSystemExtension pse : pses) {
			pse.preTick(e, movedList);
		}

		if (hasComponent(e, "CollisionShape") && hasComponent(e, "Collidable")) {
				EntityComponent<Shape> shaComponent = e.getComponent("CollisionShape");
				Shape s = shaComponent.get();
				Polygon check = s.toPolygon();
				Polygon collideCheck = check.mergeNoRepeat(check.move(new Vector2f(v.x, v.y)));

				Entity[] entities = Entity.spatialMap().getEntitiesIn(collideCheck, true).toArray(Entity[]::new);
				if (entities.length >= 1) {
					velComponent.set(Math2.round(v.add(handleCollision(e, s, collideCheck, entities)), 2));
				}
		}

		if (hasComponent(e, "Snappy")) {
			handleSnappyMovment(e);
		} else {
			handleNormalMovment(e);
		}

		//Updates the collision shape of entities that have them
		if (hasComponent(e, "CollisionShape") && hasComponent(e, "Collidable")) {
			EntityComponent<Shape> shaComponent = e.getComponent("CollisionShape");
			boolean shapeUpdated = false;
			if (shaComponent != null) {
				Shape s = shaComponent.get();
				p = posComponent.get();
				if (s.getCenter().distance(p) > 0.001f) {
					SpatialPolygonHolder sph = Entity.spatialMap().locate(e);
					if (sph != null &&
					    !sph.matches(Entity.spatialMap().gridifyPolygon(s.toPolygon()).getHashes())) {
						shapeUpdated = true;
					}
				}
				p = posComponent.get();
				Shape mod = s.clone();
				mod.setCenter(p.x, p.y);
				shaComponent.set(mod);
			}
			if (shapeUpdated) {
				movedList.add(e);
			}
	    }

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
	public Vector2f handleCollision(Entity e, Shape s, Polygon collideCheck, Entity[] entities) {
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");

		Vector2fc p = posComponent.get();
		Vector2fc v = velComponent.get();

		//Velocity adjustment
		Vector2f fullVelAdj = new Vector2f(0, 0);
		if (hasComponent(e, "Acceleration")) {
			EntityComponent<Vector2f> accComponent = e.getComponent("Acceleration");
			fullVelAdj.add(accComponent.get());
		}

		//Don't do anything if the entity isn't moving.
		if (v.length() == 0) {
			return (Vector2f) v;
		}

		Polygon move = s.toPolygon();

		entities = Arrays.stream(entities).filter(e2 -> validCollisionEntity(e, e2)).toArray(Entity[]::new);
		Arrays.sort(entities, (a, b) -> {

			EntityComponent<Vector2f> posAComponent = a.getComponent("Position");
			EntityComponent<Vector2f> posBComponent = b.getComponent("Position");
			Vector2f posA = posAComponent.get();
			Vector2f posB = posBComponent.get();

			float distA = p.distance(posA);
			float distB = p.distance(posB);

			return Float.compare(distA, distB);
		});
		
		

		for (Entity e2 : entities) {
			if (!validCollisionEntity(e, e2)) {
				continue;
			}

			Vector2f velAdj = new Vector2f(0);

			EntityComponent<Shape> sha2Component = e2.getComponent("CollisionShape");
			EntityComponent<Vector2f> pos2Component = e2.getComponent("Position");

			//Secondary entity's collision shape
			Shape s2 = sha2Component.get();
			Vector2f p2 = pos2Component.get();

			//Check if there is an intersection and that the primary entity is moving
			CollisionUtil.PolygonOutput pOutput = collideCheck.intersectsSAT(s2.toPolygon());
			if (pOutput != null) {
				Vector2f n = pOutput.getNormal().mul(pOutput.getDepth());
				velAdj.sub(n);
				performInteraction(e, e2);

				//Only actually adjusts if it is solid the magnitude of the modification is not zero
				if (hasComponent(e, "Solid") && hasComponent(e2, "Solid") && velAdj.length() > 0.001f) {
					/*
					if (new Vector2f(v).add(fullVelAdj).length() > dist) {
						float angle = (float) Math.atan2(v.y, v.x);

						Vector2f e1 = s.toPolygon().getEdgePoint(angle);
						Vector2f ex2 = s2.toPolygon().getEdgePoint(angle, p);

						float newAngle = ex2.angle(e1);
						Vector2f moveBack = new Vector2f(newAngle).mul(p.distance(e1));

						//ex2.sub(moveBack);
						System.out.println(ex2.x+", "+ex2.y);
						p.set(ex2);
						v.set(0);
						//fullVelAdj.set(0);
						fullVelAdj.add(velAdj);
					}*/
					fullVelAdj.add(velAdj);

					float mag = v.length();
					float mag2 = fullVelAdj.length();

					//Reduces fullVejAdj's magnitude if it is too high
					//Prevents bouncing backwards, sort of.
					if (mag2 > mag) {
						fullVelAdj.normalize(mag);
					}

					move = s.toPolygon().move(new Vector2f(velComponent.get()).add(fullVelAdj));
					collideCheck = s.toPolygon().mergeNoRepeat(move);
				}
			}
		}
		float mag = v.length();
		float mag2 = fullVelAdj.length();

		//Reduces fullVejAdj's magnitude if it is too high
		//Prevents bouncing backwards, sort of.
		if (mag2 > mag) {
			fullVelAdj.normalize(mag);
		}
		fullVelAdj = Math2.round(fullVelAdj, 2);
		return fullVelAdj;
	}

	private boolean validCollisionEntity(Entity e, Entity e2) {
		if ((e2 == null) || (e2 == e) || (e2.getUUID() == e.getUUID())) {
			return false;
		}
		if (!hasComponent(e2, "CollisionShape") || !hasComponent(e2, "Collidable")
			|| !hasComponent(e2, "Position")) {
			return false;
		}
		if ((!hasComponent(e2, "Solid") || !((boolean)(e2.getComponent("Solid").get()))) && !hasComponent(e2, "Interactable")) {
			return false;
		}
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
	 * @param e Entity To use
	 */
	private void handleSnappyMovment(Entity e) {
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");
		EntityComponent<Long> snappyComponent = e.getComponent("Snappy");

		Vector2f p = posComponent.get();
		Vector2f v = velComponent.get();
		//Handles snap based movement
		long nextMove = snappyComponent.get();

		p.x  = (int)(p.x/32)*32;
		p.y =  (int)(p.y/32)*32;
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
			v.x = 32;
		}
		if (v.y < 32 && v.y > 0) {
			v.y = 32;
		}
		if (v.x > -32 && v.x < 0) {
			v.x = -32;
		}
		if (v.y > -32 && v.y < 0) {
			v.y = -32;
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
	private boolean performInteraction(Entity primary, Entity secondary) {
		if (!hasComponent(primary, "Interactable") || !hasComponent(secondary, "Interactable")) {
			return false;
		}
		EntityComponent<ParameterizedRunnable<Entity>> pr = primary.getComponent("Interactable");
		EntityComponent<ParameterizedRunnable<Entity>> sc = secondary.getComponent("Interactable");
		if (pr.get() != null) {
			pr.get().run(primary, secondary);
		}
		if (sc.get() != null) {
			sc.get().run(primary, secondary);
		}
		return true;
	}

	/**
	 * Finalizes normal movement
	 * @param e Entity to finalize
	 */
	private void handleNormalMovment(Entity e) {
		EntityComponent<Vector2f> posComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velComponent = e.getComponent("Velocity");

		Vector2fc cP = posComponent.get();
		Vector2fc cV = velComponent.get();

		Vector2f v = new Vector2f(cV);
		Vector2f p = new Vector2f(cP);
		
		//Sets velocity to zero if it has a magnitude below 0.05f
		if (Math.abs(v.x()) < 0.05f) {
			v.set(0, v.y());
			velComponent.set(v);
		}
		if (Math.abs(v.y()) < 0.05f) {
			v.set(v.x(), 0);
			velComponent.set(v);
		}

		if (v.length() == 0) {
			Entity.spatialMap().sleepEntity(e);
			return;
		}
		//Normal movement
		posComponent.set(p.add(v));

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
			velComponent.set(Math2.round(v.mul(PhysicsConfig.getFloat(SMOOTHING_FACTOR_CONFIG)), 2));
		}
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
		PhysicsConfig.putIfAbsent(GRAVITY_CONFIG, false);
		PhysicsConfig.putIfAbsent(GRAVITY_LEVEL_CONFIG, 0.98f);
		PhysicsConfig.putIfAbsent(PRECISE_MODE_CONFIG, true);
		PhysicsConfig.putIfAbsent(STAGGER_MODE_CONFIG, false);
		PhysicsConfig.putIfAbsent(SMOOTHING_FACTOR_CONFIG, 0.75f);
		System.out.println(PhysicsConfig.listValues());
	}

	/**
	 * Returns the current physics tick
	 * Good for cooldowns
	 * @return long Current tick
	 */
	public long getPhysicsTick() {
		return physicsTick;
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
		running.set(false);
	}
}
