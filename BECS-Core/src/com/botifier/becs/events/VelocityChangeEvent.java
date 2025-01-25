package com.botifier.becs.events;

import org.joml.Vector2f;

import com.botifier.becs.util.events.Event;

public class VelocityChangeEvent extends Event{
	final Vector2f oldVel;
	final Vector2f newVel;

	public VelocityChangeEvent(Vector2f oldVel, Vector2f newVel) {
		this.oldVel = oldVel;
		this.newVel = newVel;
	}

	public Vector2f getOldVelocity() {
		return oldVel;
	}

	public Vector2f getNewVelocity() {
		return newVel;
	}

}
