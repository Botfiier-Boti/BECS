package com.botifier.becs.util;


import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
public class SpatialPolygonHolder {
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
		Set<Vector2f> validHashes = ConcurrentHashMap.newKeySet();

		RotatableRectangle rr = p.getBoundingBox();

		long maxX = Math.floorDiv((long) rr.getMaxX(), cellSize)+1;
		long minX = Math.floorDiv((long) rr.getMinX(), cellSize)-1;
		long maxY = Math.floorDiv((long) rr.getMaxY(), cellSize)+1;
		long minY = Math.floorDiv((long) rr.getMinY(), cellSize)-1;

		for (long y = minY; y <= maxY; y++) {
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
					validHashes.add(SpatialEntityMap.getLocation(cellMinX, cellMinY, cellSize));
				}
			}
		}
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
		return new SpatialPolygonHolder(owner, hashes, cellSize);
	}
}
