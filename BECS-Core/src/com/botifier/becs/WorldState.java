package com.botifier.becs;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.util.SpatialEntityMap;
import com.botifier.becs.util.shapes.Polygon;

/**
 * Class to hold state of the world for rendering
 */
public class WorldState {
	/**
	 * The spatial map copied
	 */
	public SpatialEntityMap sem;

	/**
	 * Empty WorldState constructor
	 * just references Entity.spatialMap
	 */
	public WorldState() {
		sem = Entity.spatialMap();
	}

	/**
	 * WorldState constructor
	 * @param p Polygon For clipping
	 * @param fake boolean Whether or not the spatial map should be copied
	 */
	public WorldState(Polygon p, boolean fake) {
		if (fake) {
			sem = Entity.spatialMap().falseClone(p);
		} else {
			sem = Entity.spatialMap();
		}
	}
}
