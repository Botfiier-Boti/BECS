package com.botifier.becs.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Vector2f;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntityComponentManager;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.Shape;

/**
 * SpatialEntityMap
 * 
 * A spatial map for entities
 * 
 * TODO: Document this
 * TODO: Optimize this
 * TODO: Ensure that deadlocks don't occur
 * TODO: Create sub maps for fast queries on specific areas
 * TODO: Make queries that return sub maps
 *  
 * 
 * @author Botifier
 */
public class SpatialEntityMap {
	private int cellSize;
	private final Map<Vector2f, Set<Entity>> grid;
	private final Map<UUID, SpatialPolygonHolder> entityLocations;
	private final Map<UUID, SpatialPolygonHolder> sleepingEntities;

    /**
     * SpatialEntityMap constructor
     * @param cellSize int Size of the map cells
     */
	public SpatialEntityMap(int cellSize) {
		this.cellSize = cellSize;
		this.grid = new ConcurrentHashMap<>();
		this.entityLocations = new ConcurrentHashMap<>();
		this.sleepingEntities = new ConcurrentHashMap<>();
	}

	private SpatialEntityMap(SpatialEntityMap origin, Set<Vector2f> hashesToCopy) {
		this(origin.cellSize);

		hashesToCopy.parallelStream().forEach(hash -> {
	        Set<Entity> sourceSet = origin.grid.get(hash);
	        if (sourceSet != null) {
	            Set<Entity> clonedSet = sourceSet.stream()
	                                             .map(Entity::falseClone)
	                                             .collect(Collectors.toSet());
	            clonedSet.forEach(clonedEntity -> {
	                this.entityLocations.put(clonedEntity.getUUID(), origin.entityLocations.get(clonedEntity.getUUID()));
	            });
	            this.grid.put(hash, clonedSet);
	        }
	    });
	}

	private SpatialEntityMap(int cellSize, Map<Vector2f, Set<Entity>> grid, Map<UUID, SpatialPolygonHolder> ent) {
		this(cellSize);

		grid.entrySet().parallelStream().forEach(e -> {
			Set<Entity> entitySet = new HashSet<>(e.getValue().size()); // Pre-size if possible.

	        for (Entity entity : e.getValue()) {
	            Entity faker = entity.falseClone();
	            entitySet.add(faker);

	            // Fetch and put only if the SpatialPolygonHolder exists.
	            SpatialPolygonHolder holder = ent.get(faker.getUUID());
	            if (holder != null) {
	                this.entityLocations.put(faker.getUUID(), holder);
	            }
	        }
			this.grid.put(e.getKey(), entitySet);
		});
	}

	private SpatialEntityMap(int cellSize, Map<Vector2f, Set<Entity>> grid, Map<UUID, SpatialPolygonHolder> ent, Set<Vector2f> hashesToCopy) {
		this(cellSize);

		hashesToCopy.forEach(h -> {
			Set<Entity> en = ConcurrentHashMap.newKeySet();
			Set<Entity> use = grid.get(h);
			if (use != null) {
				use.forEach(e -> {
					Entity faker = e.falseClone();
					if (faker == null) {
						return;
					}
					SpatialPolygonHolder sph = ent.get(faker.getUUID());
					if (sph == null) {
						return;
					}
					en.add(faker);
					this.entityLocations.put(faker.getUUID(), sph);
				});
			}
			this.grid.put(h, en);
		});
	}

	/**
	 * Generates a vector based on x and y positions scaled to the cellSize
	 * @param x float To use
	 * @param y float To use
	 * @return Vector2f Chunk location
	 */
	public Vector2f getLocation(float x, float y) {
		return getLocation(x, y, getCellSize());
	}

	/**
	 * Adds the entity to the map
	 * If the entity is already in the map re-add it
	 * @param e Entity To add
	 * @return boolean Whether or not the Entity was successfully added
	 */
	public boolean addEntity(Entity e) {
		int state = 0;
		if (e == null) {
			return false;
		}
		if (contains(e)) {
			state = removeEntity(e);
		}
		EntityComponent<Shape> s = e.getComponent("CollisionShape");
		if (s == null) {
			return false;
		}

		Polygon poly = s.get().toPolygon();
		SpatialPolygonHolder sph = new SpatialPolygonHolder(e, poly, getCellSize());

		for (Vector2f key : sph.getHashes()) {
			grid.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet(16)).add(e);
		}
		
		if (state == 1) {
			entityLocations.put(e.getUUID(), sph);
		}
		else
			sleepingEntities.put(e.getUUID(), sph);
		
		return true;
	}

	/**
	 * Removes an entity from the map
	 * @param e Entity To remove
	 * @return boolean Whether or not the removal succeeded
	 */
	public int removeEntity(Entity e) {
		if (!contains(e)) {
			return 0;
		}
		SpatialPolygonHolder sph = locate(e);
		if (sph == null) {
			return 0;
		}
		for (Vector2f key : sph.getHashes()) {
			Set<Entity> l = grid.get(key);
			if (l != null) {
				l.remove(e);
				if (l.isEmpty()) {
					grid.remove(key);
				}
			}
		}

		try {
			entityLocations.remove(e.getUUID());
		} catch (NullPointerException ne) {
			sleepingEntities.remove(e.getUUID());
			return 2;
		}
		return 1;
	}
	
	public boolean sleepEntity(Entity e) {
		if (e == null)
			return false;
		if (!isAwake(e.getUUID()))
			return false;
			
		SpatialPolygonHolder sph = locateActive(e);
		if (sph == null)
			return false;
		
		sleepingEntities.put(e.getUUID(), sph);
		entityLocations.remove(e.getUUID());
		return true;
	}

	public boolean wakeEntity(Entity e) {
		if (e == null)
			return false;
		if (isAwake(e.getUUID()))
			return false;
		
		SpatialPolygonHolder sph = locateSleeping(e);
		if (sph == null)
			return false;
		entityLocations.put(e.getUUID(), sph);
		sleepingEntities.remove(e.getUUID());
		return true;
	}

	/**
	 * Locates an entity in the map
	 * @param e Entity To locate
	 * @return SpatialPolygonHolder The area that the entity exists
	 */
	public SpatialPolygonHolder locate(Entity e) {
		return entityLocations.getOrDefault(e.getUUID(), 
				sleepingEntities.getOrDefault(e.getUUID(), null));
	}
	
	public SpatialPolygonHolder locateActive(Entity e) {
		return entityLocations.getOrDefault(e.getUUID(), null);
	}
	
	public SpatialPolygonHolder locateSleeping(Entity e) {
		return sleepingEntities.getOrDefault(e.getUUID(), null);
	}

	/**
	 * Updates entities in parallel using threads
	 * @param movedList List\<Entity\> entities to update
	 */
	public void updateEntitiesInParalell(List<Entity> movedList) {
		movedList.parallelStream().forEach(this::update);
		/*List<CompletableFuture<Void>> futures = movedList.parallelStream()
				.map(en -> CompletableFuture.runAsync(() ->
						{
							update(en);
						}, forkJoinPool)
					)
				.collect(Collectors.toList());
		CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		all.join();*/
	}

	/**
	 * Removes and re-adds an Entity to update its location in the map
	 * @param e Entity To update
	 * @return boolean Whether or not the entity was successfully re-added
	 */
	public boolean update(Entity e) {
		if (e == null ) {
			return false;
		}
		EntityComponent<Shape> s = e.getComponent("CollisionShape");
		if (s == null) {
			return false;
		}

		Polygon poly = s.get().toPolygon();
		SpatialPolygonHolder sph = new SpatialPolygonHolder(e, poly, getCellSize());
		SpatialPolygonHolder old = locate(e);

		Set<Vector2f> safeHashes = new HashSet<>();


		sph.getHashes().parallelStream().forEach(key -> {
			grid.compute(key, (k, v) -> {
				if (v == null) {
					Set<Entity> set = ConcurrentHashMap.newKeySet(16);
					set.add(e);
					return set;
				}
				if (v.contains(e)) {
					safeHashes.add(key);
				}
				return v;
			});
		});

		old.getHashes().parallelStream().filter(k -> {
			if (safeHashes.contains(k)) {
				return false;
			}
			return true;
		}).forEach(k -> {
			Set<Entity> l = grid.get(k);
			if (l != null) {
				l.remove(e);
				if (l.isEmpty()) {
					grid.remove(k);
				}
			}
		});

		try {
			entityLocations.put(e.getUUID(), sph);
		} catch (NullPointerException ex) {
			sleepingEntities.put(e.getUUID(), sph);
		}
		

		return addEntity(e);
	}

	/**
	 * Returns the map of chunks
	 * @return Map\<Vector2f, List\<Entity\>\> map of chunks
	 */
	public Map<Vector2f, Set<Entity>> getGrid() {
		return grid;
	}

	/**
	 * Gets all entities in the chunk that x and y are in
	 * @param x float To check
	 * @param y float To check
	 * @return List\<Entity\> entities in the related chunk
	 */
	public Set<Entity> getEntitiesNear(float x, float y) {
		Vector2f location = getLocation(x, y);
		return grid.getOrDefault(location, ConcurrentHashMap.newKeySet(16));
	}

	/**
	 * Gets all entities in chunks overlapping the supplied polygon
	 * @param p Polygon To check
	 * @return Set\<Entity\> of all entities in the overlapping region
	 */
	public Set<Entity> getEntitiesIn(Polygon p) {
		return getEntitiesIn(p, false);
	}

	public Set<Entity> getEntitiesIn(Polygon p, boolean collide, Set<Vector2f> outputHashes) {
		Set<Vector2f> validHashes = outputHashes;

		if (outputHashes == null) {
			validHashes = gridifyPolygon(p).getHashes();
		}
		Stream<Entity> validEntities = validHashes.parallelStream().map(grid::get)
																   .filter(Objects::nonNull)
														           .flatMap(Set::stream);
		
		

		if (collide)
			validEntities = validEntities.filter(e -> EntityComponentManager.hasComponent(e, "Collidable"));
		return validEntities.collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
	}

	public Set<Entity> getEntitiesIn(Polygon p, boolean collide) {
		return getEntitiesIn(p, collide, null);
	}

	/**
	 * Creates a SpatialPolygonHolder using supplied polygon
	 * @param p Polygon To use
	 * @return SpatialPolygonHolder based on polygon
	 */
	public SpatialPolygonHolder gridifyPolygon(Polygon p) {
		SpatialPolygonHolder sph = new SpatialPolygonHolder(null, p, cellSize);
		return sph;
	}


	/**
	 * Checks if an entity is in the spatial map
	 * @param e Entity To check
	 * @return boolean Whether or not the entity is in the map
	 */
	public boolean contains(Entity e) {
		return contains(e.getUUID());
	}
	
	public boolean isSleeping(Entity e) {
		if (e == null)
			return false;
		return isSleeping(e.getUUID());
	}
	
	public boolean isAwake(Entity e) {
		if (e == null)
			return false;
		return isAwake(e.getUUID());
	}
	
	
	public boolean contains(UUID uuid) {
		return isSleeping(uuid)|| isAwake(uuid);
	}
	
	public boolean isSleeping(UUID uuid) {
		return sleepingEntities.containsKey(uuid);
	}
	
	public boolean isAwake(UUID uuid) {
		return entityLocations.containsKey(uuid);
	}

	public Set<UUID> getAwake() {
		return entityLocations.keySet();
	}
	
	public Set<UUID> getAsleep() {
		return sleepingEntities.keySet();
	}
	
	/**
	 * Changes the cell size of the SpatialMap
	 * Clears and re-adds entities to the map
	 * @param cellSize Int New cell size
	 */
	public void resize(int cellSize) {
		if (this.cellSize == cellSize) {
			return;
		}
		List<Entity> hold = new ArrayList<>(entityLocations.values()
														   .stream()
														   .map(SpatialPolygonHolder::getOwner)
														   .collect(Collectors.toList()));

		grid.clear();
		entityLocations.clear();
		this.cellSize = cellSize;

		hold.forEach(this::addEntity);
	}

	/**
	 * Gets a vector based on cellSize
	 * @param x float X to use
	 * @param y float Y to use
	 * @param cellSize int Cell size to use
	 * @return Vector2f scaled based on cellSize
	 */
	public static Vector2f getLocation(float x, float y, int cellSize) {
		long cX = Math.floorDiv((long)x, cellSize);
		long cY = Math.floorDiv((long)y, cellSize);

		return new Vector2f(cX, cY);
	}

	public int getCellSize() {
		return cellSize;
	}


	public SpatialEntityMap falseClone() {
		SpatialEntityMap sem = new SpatialEntityMap(this.cellSize, this.grid, this.entityLocations);

		return sem;
	}

	public SpatialEntityMap falseClone(Polygon area) {
		Set<Vector2f> validHashes = gridifyPolygon(area).getHashes();
		SpatialEntityMap sem = new SpatialEntityMap(this.cellSize, this.grid, this.entityLocations, validHashes);
		return sem;
	}

	public SpatialEntityMap miniCopy(Polygon area) {
		Set<Vector2f> validHashes = gridifyPolygon(area).getHashes();
		SpatialEntityMap sem = new SpatialEntityMap(this, validHashes);
		return sem;
	}
}
