package com.botifier.becs.entity.systems.physics;

import java.util.List;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.systems.PhysicsSystem;

public abstract class PhysicsSystemExtension {

	private PhysicsSystem origin;

	public PhysicsSystemExtension(PhysicsSystem origin) {
		this.origin = origin;
	}

	public abstract void preTick(Entity e, List<Entity> moved);

	public abstract void postTick(Entity e, List<Entity> moved);


	public PhysicsSystem getOrigin() {
		return origin;
	}
}
