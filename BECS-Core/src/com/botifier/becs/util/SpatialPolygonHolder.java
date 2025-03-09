package com.botifier.becs.util;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.joml.Intersectionf;
import org.joml.Vector2f;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;


/**
 * SpatialPolygonHolder
 * Rasterized polygons
 * 
 * TODO: Make this a compute shader
 * TODO: Optimize this
 * TODO: Finish documenting this
 * 
 * @author Botifier
 */
public class SpatialPolygonHolder implements Cloneable {
	final int cellSize;
	final Entity owner;
	/**
	 * A concurrent safe set
	 */
	Set<Vector2f> hashes;

	/**
	 * SpatialPolygon constructor
	 * @param owner Entity Owner of this polygon
	 * @param p Polygon To base off of
	 * @param cellSize int Size of the cells to base off of
	 */
	public SpatialPolygonHolder(Entity owner, Polygon p, int cellSize) {
		this(owner, gridifyPolygon(p, cellSize), cellSize);
	}

	private SpatialPolygonHolder(Entity owner, Set<Vector2f> hashes, int cellSize) {
		this.owner = owner;
		this.cellSize = cellSize;
		this.hashes = hashes;
	}

	/**
	 * Rasterizes specified polygon
	 * @param p Polygon to rasterize
	 * @return Set/<Vector2f/> The rasterized points
	 */
	private static Set<Vector2f> gridifyPolygon(Polygon p, int cellSize) {
		Set<Vector2f> validHashes = null;

		RotatableRectangle rr = p.getBoundingBox();

		final long maxX = Math.floorDiv((long) rr.getMaxX(), cellSize)+1;
		final long minX = Math.floorDiv((long) rr.getMinX(), cellSize)-1;
		final long maxY = Math.floorDiv((long) rr.getMaxY(), cellSize)+1;
		final long minY = Math.floorDiv((long) rr.getMinY(), cellSize)-1;

		validHashes = LongStream.rangeClosed(minY, maxY).parallel().mapToObj(y -> {
			Set<Vector2f> validXHashes = new HashSet<>();
			for (long x = minX; x <= maxX; x++) {

				float cellMinX = x * cellSize - cellSize/2;
				float cellMaxX = cellMinX + cellSize;
				float cellMinY = y * cellSize - cellSize/2;
				float cellMaxY = cellMinY + cellSize;

				Polygon cellPolygon = Polygon.createPolygon(
						new Vector2f(cellMinX, cellMinY),
						new Vector2f(cellMaxX, cellMinY),
						new Vector2f(cellMaxX, cellMaxY),
						new Vector2f(cellMinX, cellMaxY));

				if (Intersectionf.testPolygonPolygon(p.getPoints(), cellPolygon.getPoints())) {
					validXHashes.add(SpatialEntityMap.getLocation(cellMinX, cellMinY, cellSize));
				}
			}
			return validXHashes;
		}).flatMap(s -> s.stream()).collect(Collectors.toSet());
		
		return validHashes;
	}

	public Entity getOwner() {
		return owner;
	}

	/**
	 * Check if the rasterization matches
	 * @param match Set/<Vector2f/> Of points
	 * @return boolean Whether or not they match
	 */
	public boolean matches(Set<Vector2f> match) {
		return hashes.equals(match);
	}

	/**
	 * Returns the rasterized locations
	 * @return Set/<Vector2f/> The locations
	 */
	public Set<Vector2f> getHashes() {
		return hashes;
	}

	@Override
	public SpatialPolygonHolder clone() {
		return new SpatialPolygonHolder(owner, new HashSet<>(hashes), cellSize);
	}
}
