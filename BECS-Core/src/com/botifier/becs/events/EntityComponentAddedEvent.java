package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

public class EntityComponentAddedEvent<T> extends Event {
	final EntityComponent<T> added;
	final Entity target;
	final T value;
	
	public EntityComponentAddedEvent(Entity target, EntityComponent<T> component) {
		this.added = component;
		this.target = target;
		this.value = component.get();
	}
	
	public EntityComponent<T> getComponent() {
		return added;
	}
	
	public T getValue() {
		return value;
	}
	
	public Entity getTarget() {
		return target;
	}
	
}
