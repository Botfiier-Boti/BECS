package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

public class EntityComponentRemovedEvent<T> extends Event {
	final EntityComponent<T> removed;
	final Entity target;
	
	public EntityComponentRemovedEvent(Entity target, EntityComponent<T> removed) {
		this.removed = removed;
		this.target = target;
	}
	
	public EntityComponent<T> getComponent() {
		return removed;
	}
	
	public Entity getTarget() {
		return target;
	}
	
}
