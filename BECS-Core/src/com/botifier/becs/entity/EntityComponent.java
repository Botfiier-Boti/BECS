package com.botifier.becs.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import com.botifier.becs.*;
import com.botifier.becs.events.*;

/**
 *
 * Base class for Entity Components
 * Stores information atomically
 * @author Botifier
 *
 * @param \<T\> Type of component
 */
public class EntityComponent<T> implements Cloneable {

	/**
	 * The name of the component
	 */
	private final String name;
	
	/**
	 * The component's owner
	 */
	private final Entity owner;

	/**
	 * The information stored within the component
	 */
	protected final AtomicReference<T> information;
	
	/**
	 * The class type of the stored information
	 */
	protected final Class<T> type;

	/**
	 * Component constructor
	 * @param name Name of the component
	 * @param info Info stored within the component
	 */
	@SuppressWarnings("unchecked")
	public EntityComponent(String name, Entity owner, T info) {
		information = new AtomicReference<>();
		set(info);
		this.name = name;
		this.owner = owner;
		this.type = (Class<T>) info.getClass();
	}
	

	/**
	 * Returns the name of the component
	 * @return Component's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the information within the component
	 * @return Information within the component of type T
	 */
	public T get() {
		return information.get();
	}

	/**
	 * Sets the information within the component
	 * @param info Information to use
	 */
	public void set(T info) {
		if (!info.equals(get()))
			Game.getCurrent().getEventManager().executeEventOn(new EntityComponentUpdatedEvent<T>(this, get(), info),
																   getName(),
																   getOwnerUUID());
		information.set(info);
	}
	

	/**
	 * Updates the stored information with a UnaryOperator
	 * @param updater UnaryOperator\<T\> Update operator
	 * @return T The stored information
	 */
	public T update(UnaryOperator<T> updater) {
		T old = get();
		T result = information.updateAndGet(updater);
		if (!result.equals(get()))
			Game.getCurrent().getEventManager().executeEventOn(new EntityComponentUpdatedEvent<T>(this, old, result),
																   getName(),
																   getOwnerUUID());
		return result;
	}
	
	/**
	 * Returns the owner of this component
	 * @return Entity The owner
	 */
	public Entity getOwner() {
		return owner;
	}
	
	/**
	 * Returns the component owner's UUID
	 * @return UUID Owner's UUID
	 */
	public UUID getOwnerUUID() {
		return owner != null ? owner.getUUID() : null;
	}

	public Class<T> getDataType() {
		return type;
	}
	@SuppressWarnings("unchecked")
	@Override
	public EntityComponent<T> clone() {
		try {
			return (EntityComponent<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}


}