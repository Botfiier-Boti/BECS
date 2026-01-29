package com.botifier.becs.util.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.*;

import com.botifier.becs.util.annotations.EventHandler;

public class EventManager {
	
	private Executor executor = ForkJoinPool.commonPool();
	
	private ConcurrentHashMap<Class<? extends Event>, List<EventController>> listeners = new ConcurrentHashMap<>();
	private ConcurrentHashMap<EventController, Class<? extends Event>> controllerClasses = new ConcurrentHashMap<>();
	private ConcurrentHashMap<UUID, List<EventListener>> listenerOwners = new ConcurrentHashMap<>();

	/**
	 * Registers an EventListener on this manager
	 * @param listener EventListener To register
	 * @return boolean Whether or not the registration succeeded
	 */
	public boolean registerListener(EventListener listener)  {
		Class<?> cls = listener.getClass();
		Method[] methods = cls.getDeclaredMethods();
		
		Map<Class<?>, List<EventController>> controllers = new ConcurrentHashMap<>();
		Map<EventController, Class<? extends Event>> tempConClasses = new ConcurrentHashMap<>();
		Map<Class<? extends Event>, List<EventController>> tempListeners = new ConcurrentHashMap<>();
		
		for (Method method : methods) {
			EventHandler annot = method.getDeclaredAnnotation(EventHandler.class);
			if (annot != null) {
				Class<? extends Event> eventType = annot.event();
				int priority = annot.priority();
				String target = annot.origin();

				method.setAccessible(true);

				EventController controller = new EventController(listener, method, priority, target);

				tempListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<EventController>()).add(controller);

				List<EventController> listeners = tempListeners.get(eventType);
				listeners.sort(Comparator.comparingInt(EventController::getPriority).reversed());
				
				tempConClasses.put(controller, eventType);
				controllers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<EventController>()).add(controller);
			}
		}

		if (!controllers.isEmpty()) {
			//Updates the event controllers
			try {
				Method setControllers = cls.getSuperclass().getDeclaredMethod("setEventControllers", Map.class);
				setControllers.setAccessible(true);
				setControllers.invoke(listener, controllers);
				setControllers.setAccessible(false);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
				return false;
			}
			
			controllerClasses.putAll(tempConClasses);
			for (Class<? extends Event> key : tempListeners.keySet()) {
				listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<EventController>()).addAll(tempListeners.get(key));
			}
			listenerOwners.computeIfAbsent(listener.getOwner(), k -> new CopyOnWriteArrayList<EventListener>()).add(listener);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Unregisters an EventListener from this manager
	 * @param listener EventListener To unregister
	 */
	public void unregisterListener(EventListener listener) {
		List<EventController> ec = listener.getEventControllers();
		if (ec == null) {
			return;
		}

		for (EventController e : ec) {
			Class<? extends Event> clazz = controllerClasses.remove(e);
			List<EventController> ecs = listeners.getOrDefault(clazz, null);
			if (ecs == null) {
				continue;
			}
			ecs.remove(e);
			if (ecs.size() == 0) {
				listeners.remove(clazz);
			}
		}

		List<EventListener> e = listenerOwners.getOrDefault(listener.getOwner(), null);
		if (e != null) {
			e.remove(listener);
			if (e.size() == 0) {
				listenerOwners.remove(listener.getOwner());
			}
		}
	}
	/**
	 * Executes an event with no particular target
	 * 
	 * The effectiveness of priority ordering depends on pool size.
	 * Single-threaded: Full priority ordering
	 * Small pools: Some ordering
	 * Large pools: High parallelism, low impact
	 * 
	 * @param e Event To use
	 */
	public void executeEvent(Event e) {
		executeEvent(e, null);
	}
	
	/**
	 * Executes an event with no particular target
	 * 
	 * The effectiveness of priority ordering depends on pool size.
	 * Single-threaded: Full priority ordering
	 * Small pools: Some ordering
	 * Large pools: High parallelism, low impact
	 * 
	 * @param e Event To use
	 * @param origin String Origin of the event, for filtering
	 */
	public void executeEvent(Event e, String origin) {
		List<EventController> ecs = listeners.get(e.getClass());
		if (ecs != null) {
			runEvent(ecs, origin, e);
		}
	}
	
	/**
	 * Executes an event on the specified UUIDs and the global listener
	 * 
	 * The effectiveness of priority ordering depends on pool size.
	 * Single-threaded: Full priority ordering
	 * Small pools: Some ordering
	 * Large pools: High parallelism, low impact
	 * 
	 * @param e Event To use
	 * @param origin String Origin of the event, for filtering
	 * @param uuids UUID... UUIDs to execute on
	 */
	public void executeEventOn(Event e, String origin, UUID... uuids) {
		executeEventOn(e, origin, true, uuids);
	}
	
	/**
	 * Executes an event on the specified UUIDs
	 * 
	 * The effectiveness of priority ordering depends on pool size.
	 * Single-threaded: Full priority ordering
	 * Small pools: Some ordering
	 * Large pools: High parallelism, low impact
	 * 
	 * @param e Event To use
	 * @param origin String Origin of the event, for filtering
	 * @param global boolean Whether or not the event should be run on the global listener
	 * @param uuids UUID... UUIDs to execute on
	 */
	public void executeEventOn(Event e, String origin, boolean global, UUID... uuids) {
		if (uuids == null || 
			uuids.length == 0 || 
			(uuids.length == 1 && uuids[0] == null)) {
			executeEvent(e, origin);
			return;
		}
		
		for (UUID uuid : uuids) {
			if (uuid == null)
				continue;
			List<EventListener> listeners = listenerOwners.getOrDefault(uuid, new ArrayList<>());
			if (listeners == null)
				continue;
			for (EventListener el : listeners) {
				List<EventController> ecs = el.getEventControllersOf(e.getClass());
				
				runEvent(ecs, origin, e);
			}
		}
		
		if (global) {
			List<EventListener> listeners = listenerOwners.getOrDefault(new UUID(1, 0), new ArrayList<EventListener>());
			for (EventListener el : listeners) {
				List<EventController> ecs = el.getEventControllersOf(e.getClass());
				
				runEvent(ecs, origin, e);
			}
		}
	}
	
	private void runEvent(List<EventController> ecs, String origin, Event e) {
		Stream<EventController> es = ecs.stream();
		
		List<CompletableFuture<Void>> futures = es
				.parallel()
				.map(ec -> CompletableFuture.runAsync(() -> {
					if (ec.getTarget() == null || ec.getTarget().isBlank() || ec.getTarget().equalsIgnoreCase(origin)) {
						try {
							ec.invoke(e);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
							e1.printStackTrace();
						}
					}
				}, executor))
				.collect(Collectors.toList());
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		allOf.join();
	}

	/**
	 * Completely unregisters all data associated with the specific UUID
	 * @param u UUID to unregister
	 * @return boolean Whether or not something was unregistered
	 */
	public boolean unregisterUUID(UUID u) {
		List<EventListener> listeners = listenerOwners.getOrDefault(u, new ArrayList<EventListener>());
		
		if (listeners.size() != 0) {
			for (EventListener l : listeners) {
				unregisterListener(l);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Check the listener is handling the specified event type
	 * @param clazz Class\<? extends Event\> Event type to check for
	 * @param listener EventListener Listener to check
	 * @return boolean Whether or not the event type is handled
	 */
	public boolean checkHandled(Class<? extends Event> clazz, EventListener listener) {
		List<EventController> li = listeners.get(clazz);
		if (li == null) {
			return false;
		}
		return li.stream().anyMatch(e -> e.getListener().equals(listener));
	}
	
	/**
	 * Sets the Executor for this manager
	 * @param ex Executor To use
	 * @return this
	 */
	public EventManager setExecutor(Executor ex) {
		if (ex == null)
			this.executor = ForkJoinPool.commonPool();
		else
			this.executor = ex;
		return this;
	}
	
	/**
	 * Returns the current Executor
	 * @return Executor The current executor
	 */
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * Returns all of the EventControllers in the specified listener
	 * @param listener EventListener Listener to check
	 * @return List\<EventController\> The controllers in the listener
	 */
	public List<EventController> getControllers(EventListener listener) {
		return listener.getEventControllers();
	}

	/**
	 * Get the type of event that a EventController is handling
	 * @param ec EventController To check
	 * @return Class\<? extends Event\> Event type handled  
	 */
	public Class<? extends Event> getEventType(EventController ec) {
		return this.controllerClasses.getOrDefault(ec, null);
	}

	/**
	 * Gets all of the event types handled by an EventListener
	 * @param listener EventListener To check
	 * @return List\<Class\<? extends Event\>\> The handled event types
	 */
	public List<Class<? extends Event>> getHandled(EventListener listener) {
		List<EventController> li = listener.getEventControllers();
		if (li == null || li.size() == 0) {
			return null;
		}

		List<Class<? extends Event>> classes = new ArrayList<>();
		for (EventController ec : li) {
			classes.add(controllerClasses.getOrDefault(ec, null));
		}

		return classes;
	}
}
