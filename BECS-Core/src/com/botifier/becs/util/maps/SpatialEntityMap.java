package com.botifier.becs.util.maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector2L;
import org.joml.Vector2Lc;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.util.SpatialPolygonHolder;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.Shape;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	public static final int BATCH_SIZE = 32;
	public static final int ENTITY_STATE_MISSING = 0;
	public static final int ENTITY_STATE_REMOVED_FROM_AWAKE = 1;
	public static final int ENTITY_STATE_REMOVED_FROM_SLEEPING = 2;
	
	private int cellSize;
	private final Map<Vector2Lc, Set<UUID>> grid;
	private final Map<UUID, SpatialPolygonHolder> entityLocations;
	private final Map<UUID, SpatialPolygonHolder> sleepingEntities;

    /**
     * SpatialEntityMap constructor
     * @param cellSize int Size of the map cells
     */
	public SpatialEntityMap(int cellSize) {
		this.cellSize = cellSize;
		this.grid = new StalingMap<>();
		this.entityLocations = new StalingMap<>();
		this.sleepingEntities = new StalingMap<>();
	}

	private SpatialEntityMap(SpatialEntityMap origin, Set<Vector2Lc> hashesToCopy) {
		this(origin.cellSize);

		hashesToCopy.parallelStream().forEach(hash -> {
	        Set<UUID> sourceSet = origin.grid.get(hash);
	        if (sourceSet != null) {
	            this.grid.put(hash, sourceSet);
	        }
	    });
	}

	private SpatialEntityMap(int cellSize, Map<Vector2Lc, Set<UUID>> grid, Map<UUID, SpatialPolygonHolder> ent) {
		this(cellSize);

		this.grid.putAll(grid);
	}

	private SpatialEntityMap(int cellSize, Map<Vector2Lc, Set<UUID>> grid, Map<UUID, SpatialPolygonHolder> ent, Set<Vector2Lc> hashesToCopy) {
		this(cellSize);

		hashesToCopy.forEach(h -> {
			Set<UUID> en = ConcurrentHashMap.newKeySet();
			Set<UUID> use = grid.get(h);
			if (use != null) {
				use.forEach(e -> {
					Entity faker = Entity.getEntity(e);
					if (faker == null) {
						return;
					}
					SpatialPolygonHolder sph = ent.get(faker.getUUID());
					if (sph == null) {
						return;
					}
					en.add(faker.getUUID());
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
	public Vector2L getLocation(float x, float y) {
		return getLocation(x, y, getCellSize());
	}

	/**
	 * Adds the entity to the map
	 * If the entity is already in the map re-add it
	 * @param e Entity To add
	 * @return boolean Whether or not the Entity was successfully added
	 */
	public boolean addEntity(Entity e) {
		int state = ENTITY_STATE_MISSING;
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

		for (Vector2Lc key : sph.getHashes()) {
			grid.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet(16)).add(e.getUUID());
		}
		
		if (state == ENTITY_STATE_REMOVED_FROM_AWAKE) {
			entityLocations.put(e.getUUID(), sph);
		}
		else
			sleepingEntities.put(e.getUUID(), sph);
		
		return true;
	}
	
	/**
	 * Adds an entity to the map
	 * If the entity is already in the map re-add it
	 * @param e Entity To add
	 * @param state int The state to add it in, 0 is to add it sleeping, 1 is to add it awake, correlates to constants ENTITY_STATE_MISSING and ENTITY_STATE_REMOVED_FROM_AWAKE
	 * @return boolean Whether or not the Entity was successfully added
	 */
	public boolean addEntity(Entity e, int state) {
		if (e == null) {
			return false;
		}
		if (contains(e)) {
			removeEntity(e);
		}
		EntityComponent<Shape> s = e.getComponent("CollisionShape");
		if (s == null) {
			return false;
		}

		Polygon poly = s.get().toPolygon();
		SpatialPolygonHolder sph = new SpatialPolygonHolder(e, poly, getCellSize());

		for (Vector2Lc key : sph.getHashes()) {
			grid.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet(16)).add(e.getUUID());
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
	 * 
	 * any return state > 1 is a success
	 * @param e Entity To remove
	 * @return int State based on the existence of the entity in the map, 0 the entity wasn't there, 1 entity was awake and removed, 2 entity was asleep and removed
	 */
	public int removeEntity(Entity e) {
		if (!contains(e)) {
			return ENTITY_STATE_MISSING;
		}
		SpatialPolygonHolder sph = locate(e);
		if (sph == null) {
			return ENTITY_STATE_MISSING;
		}
		for (Vector2Lc key : sph.getHashes()) {
			Set<UUID> l = grid.get(key);
			if (l != null) {
				l.remove(e.getUUID());
				if (l.isEmpty()) {
					grid.remove(key);
				}
			}
		}

		try {
			entityLocations.remove(e.getUUID());
		} catch (NullPointerException ne) {
			sleepingEntities.remove(e.getUUID());
			return ENTITY_STATE_REMOVED_FROM_SLEEPING;
		}
		return ENTITY_STATE_REMOVED_FROM_AWAKE;
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
	public void updateEntitiesInParallel(List<Entity> movedList) {
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
	
	public void updateEntitiesInBatches(List<Entity> movedList, int batchSize) {
		if (movedList.isEmpty())
			return;
		
		List<List<Entity>> moved = movedList.size() > batchSize
								 ? Lists.partition(movedList, batchSize)
								 : List.of(movedList);
		
		moved.parallelStream().forEach(l -> l.forEach(this::update));
	}
	
	public void updateEntitiesInSequence(List<Entity> movedList) {
		movedList.forEach(this::update);
	}

	/**
	 * Removes and re-adds an Entity to update its location in the map
	 * @param e Entity To update
	 * @return boolean Whether or not the entity was successfully re-added
	 */
	public boolean update(Entity e) {
		if (e == null )
			return false;
		
		EntityComponent<Shape> s = e.getComponent("CollisionShape");
		if (s == null)
			return false;

		Polygon poly = s.get().toPolygon();
		SpatialPolygonHolder sph = new SpatialPolygonHolder(e, poly, getCellSize());
		SpatialPolygonHolder old = locate(e);

		Set<Vector2Lc> safeHashes = sph.getHashes().parallelStream()
									  			  .filter(key -> {
									  				  Set<UUID> existing = grid.get(key);
									  				  return existing != null && existing.contains(e.getUUID());
									  			  })
									  			  .collect(Collectors.toSet());
		

		old.getHashes().parallelStream().filter(key -> !safeHashes.contains(key))
										.forEach(key -> {
											grid.computeIfPresent(key, (k, v) -> {
												v.remove(e.getUUID());
												return v.isEmpty() ? null : v;
											});
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
	public Map<Vector2Lc, Set<UUID>> getGrid() {
		return grid;
	}

	/**
	 * Gets all entities in the chunk that x and y are in
	 * @param x float To check
	 * @param y float To check
	 * @return List\<Entity\> entities in the related chunk
	 */
	public Set<Entity> getEntitiesNear(float x, float y) {
		Vector2L location = getLocation(x, y);
		return grid.getOrDefault(location, new HashSet<UUID>()).stream()
				   .collect(Collectors.toConcurrentMap(k -> Entity.getEntity(k), en -> Boolean.TRUE, (a, b) -> a))
				   .keySet();
	}

	/**
	 * Gets all entities in chunks overlapping the supplied polygon
	 * @param p Polygon To check
	 * @return Set\<Entity\> of all entities in the overlapping region
	 */
	public Set<Entity> getEntitiesIn(@Nonnull Polygon p) {
		return getEntitiesIn(p, false);
	}

	public Set<Entity> getEntitiesIn(@Nonnull Polygon p, boolean collide) {
		return getEntitiesIn(p, collide, null);
	}

	public Set<Entity> getEntitiesIn(@Nonnull Polygon p, boolean collide, @Nullable Set<Vector2Lc> outputHashes) {
		return getEntitiesIn(p, 
				             Sets.newConcurrentHashSet(),
							 collide ?  e -> e.hasComponent("Collidable") : null, 
						     outputHashes);
	}
	
	public Set<Entity> getEntitiesIn(@Nonnull Polygon p, @Nonnull Set<Entity> outputEntities, @Nullable Predicate<Entity> filter, @Nullable Set<Vector2Lc> outputHashes) {
		Set<Vector2Lc> validHashes = outputHashes;
		
		if (outputHashes == null)
			validHashes = gridifyPolygon(p).getHashes();
		
		Stream<UUID> validEntities = null;
		
		if (validHashes.size() < BATCH_SIZE) {
			validEntities = validHashes.stream()
					    			   .map(grid::get)
					    			   .filter(Objects::nonNull)
					    			   .flatMap(Set::stream);
		} else {	
			validEntities = Lists.partition(new ArrayList<>(validHashes), BATCH_SIZE)
								 .parallelStream()
								 .flatMap(batch -> batch.stream())
								 .map(grid::get)
								 .filter(Objects::nonNull)
								 .flatMap(Set::stream);
		}
		
		
		UUID[] uuids = validEntities.distinct().toArray(UUID[]::new);
		Entity[] entities = Entity.getEntities(uuids);
		
		if (filter != null)
			entities = Arrays.stream(entities).filter(filter).toArray(Entity[]::new);
		
		outputEntities.addAll(Arrays.asList(entities));
		
		return outputEntities;
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
		List<Entity> awake = new ArrayList<>(entityLocations.values()
														   .stream()
														   .map(SpatialPolygonHolder::getOwner)
														   .collect(Collectors.toList()));
		List<Entity> sleeping = new ArrayList<>(sleepingEntities.values()
                .stream()
                .map(SpatialPolygonHolder::getOwner)
                .collect(Collectors.toList()));

		grid.clear();
		entityLocations.clear();
		this.cellSize = cellSize;
	    sleepingEntities.clear();

	    awake.forEach(e -> addEntity(e, ENTITY_STATE_REMOVED_FROM_AWAKE));
	    sleeping.forEach(e -> addEntity(e, ENTITY_STATE_MISSING));
	}

	/**
	 * Gets a vector based on cellSize
	 * @param x float X to use
	 * @param y float Y to use
	 * @param cellSize int Cell size to use
	 * @return Vector2f scaled based on cellSize
	 */
	public static Vector2L getLocation(float x, float y, int cellSize) {
		long cX = Math.floorDiv((long)x, cellSize);
		long cY = Math.floorDiv((long)y, cellSize);

		return new Vector2L(cX, cY);
	}

	public int getCellSize() {
		return cellSize;
	}


	public SpatialEntityMap falseClone() {
		SpatialEntityMap sem = new SpatialEntityMap(this.cellSize, this.grid, this.entityLocations);

		return sem;
	}

	public SpatialEntityMap falseClone(Polygon area) {
		Set<Vector2Lc> validHashes = gridifyPolygon(area).getHashes();
		SpatialEntityMap sem = new SpatialEntityMap(this.cellSize, this.grid, this.entityLocations, validHashes);
		return sem;
	}

	public SpatialEntityMap miniCopy(Polygon area) {
		Set<Vector2Lc> validHashes = gridifyPolygon(area).getHashes();
		SpatialEntityMap sem = new SpatialEntityMap(this, validHashes);
		return sem;
	}
}
