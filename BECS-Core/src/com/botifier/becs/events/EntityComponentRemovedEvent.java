package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

/**
 * EntityComponentRemovedEvent
 * Event that is thrown when a component is removed from an entity
 * @param \<T\> Date type stored in the component
 */
public class EntityComponentRemovedEvent<T> extends Event {
	/**
	 * The removed component
	 */
	final EntityComponent<T> removed;
	/**
	 * The entity that lost the component
	 */
	final Entity target;
	/**
	 * The data that was in the component at time of removal
	 */
	final T value;
	
	public EntityComponentRemovedEvent(Entity target, EntityComponent<T> removed) {
		this.removed = removed;
		this.target = target;
		this.value = removed.get();
	}
	
	/**
	 * Returns the removed component
	 * @return EntityComponent\<T\> The removed component
	 */
	public EntityComponent<T> getComponent() {
		return removed;
	}
	
	/**
	 * Returns the value at the time of removal
	 * @return T The value
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * Returns the entity that lost the component
	 * @return Entity The one who lost
	 */
	public Entity getTarget() {
		return target;
	}
	
}
