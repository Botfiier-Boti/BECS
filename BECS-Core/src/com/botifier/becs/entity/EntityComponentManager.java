package com.botifier.becs.entity;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.joml.Vector2f;
import org.joml.Vector2fc;

import com.botifier.becs.Game;
import com.botifier.becs.events.*;
import com.botifier.becs.events.listeners.PhysicsListener;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.EntityRunnable;
import com.botifier.becs.util.shapes.Shape;

public class EntityComponentManager {
	/**
	 * Maps list of owners and name of component
	 */
	private static final ConcurrentHashMap<String, CopyOnWriteArraySet<Entity>> componentMap = new ConcurrentHashMap<>();
	/**
	 * Maps name of component to class type of information within
	 */
	private static final ConcurrentHashMap<String, Class<?>> nameMap = new ConcurrentHashMap<>();
	
	/**
	 * Map of overrides for custom component types
	 */
	private static final ConcurrentHashMap<Class<?>, Class<? extends EntityComponent<?>>> overrideMap = new ConcurrentHashMap<>();
	
	/**
	 * Initializes basic components
	 */
	public static synchronized void init() {
		overrideMap.put(Vector2f.class, EntityVector2fComponent.class);
		
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
	public static void createComponent(String name,  Class<?> dataType) {
		if (dataType == null) {
			throw new IllegalArgumentException("dataType cannot be null!");
		}
		if (nameMap.containsKey(name.toLowerCase())) {
			throw new IllegalArgumentException(String.format("Component of name '%s' already exists.", name));
		}

		nameMap.put(name.toLowerCase(), dataType);
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
		
		Class<?> type = nameMap.getOrDefault(componentName.toLowerCase(), null);
		
		if (type == null)
			throw new IllegalArgumentException(String.format("No type mapping for for component %s", lower));
		
		if (type.isAssignableFrom(en.get().getClass()) 
				|| getWrapperClass(type).isAssignableFrom(en.get().getClass())
				|| getPrimitiveClass(type).isAssignableFrom(en.get().getClass()))
			throw new ClassCastException(String.format("%s is not compatible with %s", en.get().getClass().getSimpleName(), type.getSimpleName()));
		
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
		Class<? extends EntityComponent<?>> clazz = overrideMap.computeIfPresent(nameMap.get(componentName.toLowerCase()), (a, b) -> {
			return b;
		});
		
		
		Class<? > type = nameMap.getOrDefault(componentName.toLowerCase(), null);
		if (!type.isAssignableFrom(data.getClass()) 
				&& !getWrapperClass(type).isAssignableFrom(data.getClass())
				&& !getPrimitiveClass(type).isAssignableFrom(data.getClass()))
			throw new ClassCastException(String.format("%s is not compatible with %s", data.getClass().getSimpleName(), type.getSimpleName()));
		
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
		return nameMap.get(name.toLowerCase());
	}

	/**
	 * Returns the wrappers of primitives classes
	 * TODO: Move this somewhere else
	 * @param primitiveType
	 * @return
	 */
	private static Class<?> getWrapperClass(Class<?> primitiveType) {
	    if (primitiveType == int.class) return Integer.class;
	    if (primitiveType == long.class) return Long.class;
	    if (primitiveType == float.class) return Float.class;
	    if (primitiveType == double.class) return Double.class;
	    if (primitiveType == boolean.class) return Boolean.class;
	    if (primitiveType == char.class) return Character.class;
	    if (primitiveType == byte.class) return Byte.class;
	    if (primitiveType == short.class) return Short.class;
	    return primitiveType;
	}
	
	/**
	 * Returns the primitive version of primitive wrapper classes
	 * TODO: Move this somewhere else
	 * @param primitiveType Class<?> Wrapper class
	 * @return Class<?> The primitive version of the wrapper
	 */
	private static Class<?> getPrimitiveClass(Class<?> primitiveType) {
	    if (primitiveType == Integer.class) return int.class;
	    if (primitiveType == Long.class) return long.class;
	    if (primitiveType == Float.class) return float.class;
	    if (primitiveType == Double.class) return double.class;
	    if (primitiveType == Boolean.class) return boolean.class;
	    if (primitiveType == Character.class) return char.class;
	    if (primitiveType == Byte.class) return byte.class;
	    if (primitiveType == Short.class) return short.class;
	    return primitiveType;
	}
}


