package com.botifier.becs.entity.systems.physics;

import java.util.List;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.systems.PhysicsSystem;

/**
 * PhysicsSystemExtension
 * @author Botifier
 */
public abstract class PhysicsSystemExtension {

	/**
	 * The PhysicsSystem that this extends
	 */
	private PhysicsSystem origin;

	/**
	 * Creates a new PhysicsSystemExtension
	 * @param origin The PhysicsSysem to extend
	 */
	public PhysicsSystemExtension(PhysicsSystem origin) {
		this.origin = origin;
	}

	/**
	 * Run before an entity tick
	 * @param e
	 * @param moved
	 */
	public abstract void preTick(Entity e, List<Entity> moved);

	/**
	 * Run after an entity tick
	 * @param e
	 * @param moved
	 */
	public abstract void postTick(Entity e, List<Entity> moved);


	/**
	 * Gets the target PhysicsSysetm
	 * @return PhysicsSystem, the target this extends
	 */
	public PhysicsSystem getOrigin() {
		return origin;
	}
}
