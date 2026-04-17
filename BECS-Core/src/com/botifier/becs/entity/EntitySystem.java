package com.botifier.becs.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.botifier.becs.Game;
import com.google.common.collect.Sets;

public abstract class EntitySystem {

	private static ThreadLocal<ExecutorService> ex = ThreadLocal.withInitial(() -> new ForkJoinPool(4));
	
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
		requiredComponents = makeLower(required);
	}

	/**
	 * Support function for minor performance boost
	 * @param strings
	 * @return
	 */
	private final String[] makeLower(String... strings) {
		String[] lowerCopy = new String[strings.length];
		
		for (int i = 0; i < lowerCopy.length; i++) {
			lowerCopy[i] = strings[i].toLowerCase();
		}
		
		return lowerCopy;
	}
	
	/**
	 * Applies the system to array of entities
	 * 
	 * This method should check isPaused() if pausing is desired.
	 * @param entities Entity[] Array to apply to
	 */
	public abstract CompletableFuture<Void> apply(Entity[] entities);

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
		
		String bestStarter = requiredComponents[0];
	    int minSize = EntityComponentManager.getEntitiesWithComponent(bestStarter).size();

	    for (int i = 1; i < requiredComponents.length; i++) {
	    	String comp = requiredComponents[i];
	    	
	        int size = EntityComponentManager.getNumberOfEntitiesWithComponent(comp);
	        if (size < minSize) {
	            minSize = size;
	            bestStarter = comp;
	        }
	    }
		
		Set<Entity> entities = EntityComponentManager.getEntitiesWithComponent(bestStarter);
		
		try {
			Future<Set<Entity>> future = ex.get().submit(() -> entities.parallelStream()
					   .filter(e -> e.hasComponentPrelower(requiredComponents))
					   .collect(Collectors.toCollection(Sets::newConcurrentHashSet)));
			return future.get();
		} catch (Exception e) {
			return Sets.newConcurrentHashSet();
		}
		
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
	 * Returns the game instance that this system belongs to
	 * @return Game
	 */
	public Game getGame() {
		return g;
	}
	
	/**
	 * Pauses the system
	 * 
	 * The effects of pausing depends on the implementation of apply
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
