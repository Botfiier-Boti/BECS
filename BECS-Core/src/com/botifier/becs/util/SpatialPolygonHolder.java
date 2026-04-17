package com.botifier.becs.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.joml.Intersectionf;
import org.joml.Vector2L;
import org.joml.Vector2Lc;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.util.maps.SpatialEntityMap;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;

/**
 * SpatialPolygonHolder Rasterized polygons
 * 
 * TODO: Make this a compute shader TODO: Optimize this TODO: Finish documenting
 * this
 * 
 * @author Botifier
 */
public class SpatialPolygonHolder implements Cloneable {
	final int cellSize;
	final Entity owner;
	/**
	 * A concurrent safe set
	 */
	Set<Vector2Lc> hashes;

	/**
	 * SpatialPolygon constructor
	 * 
	 * @param owner    Entity Owner of this polygon
	 * @param p        Polygon To base off of
	 * @param cellSize int Size of the cells to base off of
	 */
	public SpatialPolygonHolder(Entity owner, Polygon p, int cellSize) {
		this(owner, gridifyPolygon(p, cellSize), cellSize);
	}

	private SpatialPolygonHolder(Entity owner, Set<Vector2Lc> hashes, int cellSize) {
		this.owner = owner;
		this.cellSize = cellSize;
		this.hashes = hashes;
	}

	/**
	 * Rasterizes specified polygon
	 * 
	 * @param p Polygon to rasterize
	 * @return Set/<Vector2f/> The rasterized points
	 */
	private static Set<Vector2Lc> gridifyPolygon(Polygon p, int cellSize) {
		RotatableRectangle rr = p.getBoundingBox();

		final long maxX = Math.floorDiv((long) rr.getMaxX(), cellSize) + 1;
		final long minX = Math.floorDiv((long) rr.getMinX(), cellSize) - 1;
		final long maxY = Math.floorDiv((long) rr.getMaxY(), cellSize) + 1;
		final long minY = Math.floorDiv((long) rr.getMinY(), cellSize) - 1;

		final long cellCount = (maxX - minX + 1) * (maxY - minY + 1);

		return rasterizePolygon(p, minX, maxX, minY, maxY, cellSize, cellCount > 16);
	}
	
	private static Set<Vector2Lc> rasterizePolygon(Polygon p, long minX, long maxX, long minY, long maxY, int cellSize, boolean parallel) {
		final Vector2f[] polyPoints = p.getPoints();
		final float halfCell = cellSize * 0.5f;

		return LongStream.rangeClosed(minY, maxY).parallel().mapToObj(y -> {
			final Vector2f[] cellPoints = new Vector2f[] { new Vector2f(), new Vector2f(), new Vector2f(),
					new Vector2f() };
			
			Set<Vector2L> validXHashes = new HashSet<>();
			
			final float cellCenterY = y * cellSize;
			final float cellMinY = cellCenterY - halfCell;
			final float cellMaxY = cellCenterY + halfCell;
			
			for (long x = minX; x <= maxX; x++) {

				final float cellCenterX = x * cellSize;
				final float cellMinX = cellCenterX - halfCell;
				final float cellMaxX = cellCenterX + halfCell;

				cellPoints[0].set(cellMinX, cellMinY);
                cellPoints[1].set(cellMaxX, cellMinY);
                cellPoints[2].set(cellMaxX, cellMaxY);
                cellPoints[3].set(cellMinX, cellMaxY);	

				if (Intersectionf.testPolygonPolygon(polyPoints, cellPoints)) {
					validXHashes.add(SpatialEntityMap.getLocation(cellMinX, cellMinY, cellSize));
				}
			}
			return validXHashes;
		}).flatMap(Set::stream).collect(Collectors.toSet());
	}

	public Entity getOwner() {
		return owner;
	}

	/**
	 * Check if the rasterization matches
	 * 
	 * @param match Set/<Vector2f/> Of points
	 * @return boolean Whether or not they match
	 */
	public boolean matchesF(Set<Vector2fc> match) {
		return matches(match.stream().map(v -> new Vector2L((long) v.x(), (long) v.y())).collect(Collectors.toSet()));
	}

	/**
	 * Check if the rasterization matches
	 * 
	 * @param match Set/<Vector2L/> Of points
	 * @return boolean Whether or not they match
	 */
	public boolean matches(Set<Vector2Lc> match) {
		return hashes.equals(match);
	}

	/**
	 * Returns the rasterized locations
	 * 
	 * @return Set/<Vector2f/> The locations
	 */
	public Set<Vector2Lc> getHashes() {
		return hashes;
	}

	@Override
	public SpatialPolygonHolder clone() {
		return new SpatialPolygonHolder(owner, new HashSet<>(hashes), cellSize);
	}
}
