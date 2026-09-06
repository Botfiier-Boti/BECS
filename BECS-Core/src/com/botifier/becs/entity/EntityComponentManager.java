package com.botifier.becs.entity;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

import org.joml.Vector2fc;

import com.botifier.becs.Game;
import com.botifier.becs.entity.util.ComponentKey;
import com.botifier.becs.events.*;
import com.botifier.becs.events.listeners.PhysicsListener;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.EntityRunnable;
import com.botifier.becs.util.maps.StalingMap;
import com.botifier.becs.util.maps.immutable.ImmutableHashMap;
import com.botifier.becs.util.memory.ExceptionFormatter;
import com.botifier.becs.util.shapes.Shape;

public class EntityComponentManager {
	/**
	 * Maps list of owners and name of component
	 */
	private static final Map<CharSequence, CopyOnWriteArraySet<Entity>> componentMap = new StalingMap<>();
	/**
	 * Maps name of component to class type of information within
	 */
	private static final Map<CharSequence, ComponentKey<?>> nameMap = new StalingMap<>();
	
	/**
	 * Map of overrides for custom component types
	 */
	private static final Map<Class<?>, Class<? extends EntityComponent<?>>> overrideMap = new StalingMap<>();
	
	/**
	 * Initializes basic components
	 */
	public static synchronized void init() {
		overrideMap.put(Vector2fc.class, EntityVector2fcComponent.class);
		
		createComponent("ArrowKeyControlled", boolean.class);
		createComponent("BooleanDirection", boolean.class);
		createComponent("PhysicsEnabled", PhysicsListener.class);
		createComponent("Collidable", boolean.class);
		createComponent("CollisionShape", Shape.class);
		createComponent("GravityAffected", boolean.class);
		createComponent("Color", Color.class);
		createComponent("IgnoreWith", String.class);
		createComponent("Image", Image.class);
		createComponent("Interactable", EntityRunnable.class);
		createComponent("Position", Vector2fc.class);
		createComponent("Rotation", float.class);
		createComponent("Snappy", long.class);
		createComponent("Solid", boolean.class);
		createComponent("Speed", float.class);
		createComponent("Velocity", Vector2fc.class);
		createComponent("Acceleration", Vector2fc.class);
		createComponent("Trailer", Vector2fc.class);
	}

	/**
	 * Creates a new component
	 * @param name Name of component
	 * @param dataType Class Type of the information that will be stored within
	 */
	public static <T> void createComponent(String name,  Class<T> dataType) {
		if (dataType == null) {
			throw ECMExceptionCache.getError(ECMErrorTemplate.NULL_DATATYPE);
		}
		if (nameMap.containsKey(name.toLowerCase())) {
			throw new IllegalArgumentException(String.format("Component of name '%s' already exists.", name));
		}

		nameMap.put(name.toLowerCase(), new ComponentKey<T>(name.toLowerCase(), dataType));
		
		//Ensure the class is loaded into memory
		try {
			if (!dataType.isPrimitive())
				Class.forName(dataType.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Grants an entity specified component
	 * @param <T> Object extending EntityComponent
	 * @param e Entity to give
	 * @param component Component instance to use
	 */
	private static <T> void addComponent(Entity e, EntityComponent<T> component) {
		if (e == null || component == null) {
			return;
		}
		componentMap.computeIfAbsent(component.getName().toLowerCase(), k -> new CopyOnWriteArraySet<Entity>()).add(e);
		e.components.put(component.getName().toLowerCase(), component);
		
		
		Game.getCurrent().getEventManager().executeEventOn(new EntityComponentAddedEvent<T>(e, component),
														   component.getName(),
														   e.getUUID());
	}
	
	
	/**
	 * Removes a component from an entity
	 * @param <T> Type of information in the component
	 * @param e
	 * @param componentName
	 * @return
	 */
	public static <T> EntityComponent<T> removeComponent(Entity e, String componentName) {
		if (e == null || componentName == null)
			return null;
		String lower = componentName.toLowerCase();

		EntityComponent<?> en = e.components.getOrDefault(lower, null);
		
		if (en == null)
			return null;
		ComponentKey<?> type = nameMap.getOrDefault(componentName.toLowerCase(), null);
		
		if (type == null)
			throw new IllegalArgumentException(String.format("No type mapping for for component %s", lower));
		
		if (!type.isCompatibleType(en.getDataType()))
			throw new ClassCastException(String.format("%s is not compatible with %s", en.get().getClass().getSimpleName(), type.type().getSimpleName()));
		
		e.components.remove(lower);
		componentMap.get(lower).remove(e);

		@SuppressWarnings("unchecked")
		EntityComponent<T> ent =  new EntityComponent<T>(lower, e, (T) en.get());
		Game.getCurrent().getEventManager().executeEventOn(new EntityComponentRemovedEvent<T>(e, ent),
				   en.getName(),
				   e.getUUID());

		return ent;
	}

	/**
	 * Gives an entity a component based on name
	 * @param <Z> Data Type
	 * @param e Entity to give
	 * @param componentName Name of the component
	 * @param data Information to store within
	 * @return The component that was added
	 */
	@SuppressWarnings("unchecked")
	public static <Z> EntityComponent<Z> giveComponent(Entity e,  String componentName, Z data) {
		if (!nameMap.containsKey(componentName.toLowerCase())) {
			throw new NullPointerException(String.format("Component of name '%s' does not exist.", componentName));
		}
		
		EntityComponent<Z> component = null;
		Class<? extends EntityComponent<?>> clazz = overrideMap.computeIfPresent(nameMap.get(componentName.toLowerCase()).type(), (a, b) -> {
			return b;
		});
		
		
		ComponentKey<? > type = nameMap.getOrDefault(componentName.toLowerCase(), null);
		if (!type.isCompatibleType(data.getClass()))
			throw new ClassCastException(String.format("%s is not compatible with %s", data.getClass().getSimpleName(), type.type().getSimpleName()));
		
		if (clazz == null) {
				component = new EntityComponent<Z>(componentName, e, data);
		} else {
			try {
				component = (EntityComponent<Z>) clazz.getDeclaredConstructors()[0].newInstance(componentName, e, data);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e1) {
				e1.printStackTrace();
			}
		}
		addComponent(e, component);
		return component;
	}

	/**
	 * Finds all entities with componentName component
	 * @param componentName Name of the component
	 * @return ArrayList of entities
	 */
	public static Set<Entity> getEntitiesWithComponent(String componentName) {
		Set<Entity> entities = componentMap.get(componentName.toLowerCase());
		return entities != null ? Collections.unmodifiableSet(entities) : Collections.emptySet();
	}
	
	/**
	 * Returns the number of entities with a component, -1 means it doesnt exist.
	 * @param componentName Name of the component
	 * @return int Number of entities
	 */
	public static int getNumberOfEntitiesWithComponent(String componentName) {
		Set<Entity> entities = componentMap.get(componentName.toLowerCase());
		
		return entities != null ? entities.size() : -1;
	}
	
	public static <T> EntityComponent<T> getComponent(Entity e, ComponentKey<T> key) {
		@SuppressWarnings("unchecked")
		EntityComponent<T> comp = (EntityComponent<T>) e.components.get(key.name());
		return comp;
	}

	/**
	 * Checks if an entity has specified component
	 * @param <T> T of type EntityComponent
	 * @param e Entity to check
	 * @param componentName Name of component to check
	 * @return Whether or not entity has the component according to the map
	 */
	public static <T extends EntityComponent<?>> boolean hasComponent(Entity e, String componentName) {
		return e.hasComponent(componentName);
	}


	/**
	 * Returns the data-type of the named component's information
	 * @param name Component name to check
	 * @return The class type
	 */
	public static Class<?> getComponentDataType(String name) {
		return getComponentKey(name).type();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> ComponentKey<T> getComponentKey(String name){
		return (ComponentKey<T>) nameMap.get(name.toLowerCase());
	}

	//Experimental caching of errors
	
	private enum ECMErrorTemplate {
		NULL_DATATYPE("dataType cannot be null", () -> new IllegalArgumentException()),
		COMPONENT_ALREADY_EXISTS("Component of name '%s' already exists.", () -> new IllegalArgumentException());
		
		private final String templateMessage;
		private final Supplier<? extends RuntimeException > supplier;
		
		ECMErrorTemplate(String templateMessage, Supplier<? extends RuntimeException > supplier) { 
			this.templateMessage = templateMessage; 
			this.supplier = supplier;
		}
		
	}
	
	private static final class ECMExceptionCache {
		
		
		final static ImmutableHashMap<ECMErrorTemplate, ThreadLocal<RuntimeException >> exceptionCache;
		
		static {
			Map<ECMErrorTemplate, ThreadLocal<RuntimeException >> tempMap = new HashMap<>();

			for (ECMErrorTemplate template : ECMErrorTemplate.values()) {
				tempMap.put(template, ThreadLocal.withInitial(template.supplier));
			}
			
			exceptionCache = ImmutableHashMap.from(tempMap);
		}
		
		private static final RuntimeException  getThrowable(ECMErrorTemplate template) {
			return exceptionCache.get(template).get();
		}
		
		public static RuntimeException  getError(ECMErrorTemplate template, Object... params) {
			return ExceptionFormatter.formatMessage(getThrowable(template), template.templateMessage, params);
		}
		
	}
	
}


