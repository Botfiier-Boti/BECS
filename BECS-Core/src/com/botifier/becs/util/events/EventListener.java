package com.botifier.becs.util.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * EventListener
 * 
 * TODO: Document this
 * 
 * @author Botifier
 */
public abstract class EventListener {

	private final UUID owner;
	private final UUID selfUUID;
	private Map<Class<?>, List<EventController>> controllers;
	private EventManager em = null;
	
	public EventListener() {
		this(new UUID(1, 0));
	}
	
	public EventListener(UUID owner) {
		this(owner, UUID.randomUUID());
	}
	
	protected EventListener(UUID owner, UUID uuid) {
		this.owner = owner;
		this.selfUUID = uuid;
	}
	
	protected void setEventControllers(Map<Class<?>, List<EventController>> ec) {
		controllers = ec; 
	}
	
	public void register(EventManager em) {
		if (!this.em.equals(em))
			unregister();
		this.em = em;
		this.em.registerListener(this);
	}
	
	public void unregister() {
		this.em.unregisterListener(this);
	}
	
	public UUID getOwner() {
		return this.owner;
	}
	
	public UUID getUUID() {
		return this.selfUUID;
	}
	
	public List<EventController> getEventControllersOf(Class<?> clazz) {
		return this.controllers.getOrDefault(clazz, new ArrayList<>());
	}
	
	public List<EventController> getEventControllers() {
		List<EventController> result = new ArrayList<EventController>();
		this.controllers.forEach((k, v) -> {
			result.addAll(v);
		});
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(selfUUID);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventListener other = (EventListener) obj;
		return Objects.equals(selfUUID, other.selfUUID);
	}
}
