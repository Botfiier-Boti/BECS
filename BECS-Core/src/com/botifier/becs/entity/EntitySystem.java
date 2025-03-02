package com.botifier.becs.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.botifier.becs.Game;

public abstract class EntitySystem {

	/**
	 * Whether or not the system is paused
	 * A system can ignore this if it feels like it.
	 * Nothing is forcing it
	 */
	private boolean paused = false;

	/**
	 * Array of required component names
	 */
	private String[] requiredComponents;
	
	/**
	 * The owner of this system
	 */
	private Game g;

	/**
	 * EntitySystem constructor
	 */
	public EntitySystem(Game g) {
		requiredComponents = new String[0];
		this.g = g;
	}

	/**
	 * EntitySystem constructor with requirements
	 * @param required String... Components to require
	 */
	@SafeVarargs
	public EntitySystem(Game g, String... required) {
		this(g);
		requiredComponents = required;
	}

	/**
	 * Applies the system to array of entities
	 * @param entities Entity[] Array to apply to
	 */
	public abstract void apply(Entity[] entities);

	/**
	 * Run when game is closing
	 * If you want to dereference the system, it is recommended to run this first.
	 */
	public abstract void destroy();

	/**
	 * Returns a set of entities that have the required components
	 * @return Set\<Entity\> Valid entities
	 */
	public Set<Entity> getValidEntities() {
		if (requiredComponents == null || requiredComponents.length == 0) {
			return Entity.getEntities();
		}
		Set<Entity> hold = new HashSet<>();
		
		Set<Entity> entities = EntityComponentManager.getEntitiesWithComponent(requiredComponents[0]);
		hold = entities.stream().parallel().filter(e -> {
			return e.hasComponent(requiredComponents);
		}).collect(Collectors.toSet());
		return hold;

	}

	/**
	 * Returns an array of the required components
	 * @return String[] The required components
	 */
	public String[] getRequiredComponents() {
		return requiredComponents;
	}

	/**
	 * Whether or not the system should be paused
	 * Doesn't matter if it isn't programmed in apply
	 * @return boolean
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Returns the game that this system belongs to
	 * @return Game
	 */
	public Game getGame() {
		return g;
	}
	
	/**
	 * Should pause the system
	 * Unless someone forgets to add pausing to apply
	 */
	public void pause() {
		paused = true;
	}

	/**
	 * Should resume the system
	 */
	public void resume() {
		paused = false;
	}
}
