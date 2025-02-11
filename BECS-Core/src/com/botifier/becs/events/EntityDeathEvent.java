package com.botifier.becs.events;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.util.events.Event;

public class EntityDeathEvent extends Event {
	private final Entity e;
	
	public EntityDeathEvent(Entity e) {
		this.e = e;
	}
	
	public Entity getEntity() {
		return e;
	}
}
