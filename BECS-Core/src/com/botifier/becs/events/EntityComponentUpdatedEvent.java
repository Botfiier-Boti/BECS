package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

public class EntityComponentUpdatedEvent<T> extends Event {
	final EntityComponent<T> updated;
	final T oldValue;
	final T newValue;
	
	public EntityComponentUpdatedEvent(EntityComponent<T> updated, T oldValue, T newValue) {
		this.updated = updated;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public T getOldValue() {
		return oldValue;
	}
	
	public T getNewValue() {
		return newValue;
	}
	
	public EntityComponent<T> getComponent() {
		return updated;
	}
	
}
