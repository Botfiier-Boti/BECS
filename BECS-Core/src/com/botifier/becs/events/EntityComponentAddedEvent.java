package com.botifier.becs.events;

import com.botifier.becs.entity.*;
import com.botifier.becs.util.events.*;

/**
 * EntityComponentAddedEvent
 * Event that is thrown whenever an EntityComponent is added to an entity
 * @param \<T\> Type that the component uses
 */
public class EntityComponentAddedEvent<T> extends Event {
	/**
	 * The component that was added
	 */
	final EntityComponent<T> added;
	/**
	 * The entity that got the component
	 */
	final Entity target;
	/**
	 * The initial value stored within the component
	 */
	final T value;
	
	/**
	 * EntityComponentAddedEvent constructor
	 * @param target Entity the target entity
	 * @param component EntityComponent\<T\> The added component
	 */
	public EntityComponentAddedEvent(Entity target, EntityComponent<T> component) {
		this.added = component;
		this.target = target;
		this.value = component.get();
	}
	
	/**
	 * Returns the added component
	 * @return EntityComponent\<T\> The added component
	 */
	public EntityComponent<T> getComponent() {
		return added;
	}
	
	/**
	 * Returns the initial value of the component
	 * @return T The value
	 */
	public T getValue() {
		return value;
	}
	
	/**
	 * Returns the entity that got this component
	 * @return Entity The target
	 */
	public Entity getTarget() {
		return target;
	}
	
}
