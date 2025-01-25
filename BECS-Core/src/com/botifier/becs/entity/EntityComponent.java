package com.botifier.becs.entity;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import com.botifier.becs.*;
import com.botifier.becs.events.*;

/**
 *
 * Base class for Entity Components
 * @author Botifier
 *
 * @param \<T\> Type of component
 */
public class EntityComponent<T> implements Cloneable {

	/**
	 * The name of the Component
	 */
	private final String name;
	
	private final Entity owner;

	/**
	 * The information stored within the component
	 */
	protected final AtomicReference<T> information;
	
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
		return information.getPlain();
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
	

	public T update(UnaryOperator<T> updater) {
		T old = get();
		T result = information.updateAndGet(updater);
		if (!result.equals(get()))
			Game.getCurrent().getEventManager().executeEventOn(new EntityComponentUpdatedEvent<T>(this, old, result),
																   getName(),
																   getOwnerUUID());
		return result;
	}
	
	public Entity getOwner() {
		return owner;
	}
	
	public UUID getOwnerUUID() {
		return owner != null ? owner.getUUID() : null;
	}

	@Override
	public EntityComponent<T> clone() {
		return new EntityComponent<>(name, owner, information.get());
	}


}