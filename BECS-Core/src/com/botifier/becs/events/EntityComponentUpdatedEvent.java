package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

/**
 * EntityComponentUpdatedEvent
 * Run whenever an entity component is updated
 * @param \<T\> Type of information inside the component
 */
public class EntityComponentUpdatedEvent<T> extends Event {
	/**
	 * The updated component
	 */
	final EntityComponent<T> updated;
	/**
	 * The old value of the component
	 */
	final T oldValue;
	/**
	 * The value at the time of event
	 */
	final T newValue;
	
	/**
	 * EntityComponentUpdatedEvent constructor
	 * @param updated EntityComponent\<T\> The updated component
	 * @param oldValue T The old value
	 * @param newValue T The value at the time of the event
	 */
	public EntityComponentUpdatedEvent(EntityComponent<T> updated, T oldValue, T newValue) {
		this.updated = updated;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	/**
	 * Returns the old value of the component
	 * @return T The old value
	 */
	public T getOldValue() {
		return oldValue;
	}
	
	/**
	 * Returns the value of the component at the time of the event
	 * @return T the new value
	 */
	public T getNewValue() {
		return newValue;
	}
	
	/**
	 * Returns the entity component that was updated
	 * @return EntityComponent\<T\> The updated component
	 */
	public EntityComponent<T> getComponent() {
		return updated;
	}
	
}
