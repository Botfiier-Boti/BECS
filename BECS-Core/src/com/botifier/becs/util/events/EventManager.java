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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.botifier.becs.util.annotations.EventHandler;

public class EventManager {
	ConcurrentHashMap<Class<? extends Event>, List<EventController>> listeners = new ConcurrentHashMap<>();
	ConcurrentHashMap<EventController, Class<? extends Event>> controllerClasses = new ConcurrentHashMap<>();
	ConcurrentHashMap<UUID, List<EventListener>> listenerOwners = new ConcurrentHashMap<>();

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
			listeners.putAll(tempListeners);
			listenerOwners.computeIfAbsent(listener.getOwner(), k -> new CopyOnWriteArrayList<EventListener>()).add(listener);
		} else {
			return false;
		}
		return true;
	}

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

	public void executeEvent(Event e) {
		executeEvent(e, null);
	}
	
	public void executeEvent(Event e, String origin) {
		List<EventController> ecs = listeners.get(e.getClass());
		if (ecs != null) {
			runEvent(ecs, origin, e);
		}
	}
	
	public void executeEventOn(Event e, String origin, UUID... uuids) {
		executeEventOn(e, origin, true, uuids);
	}
	
	public void executeEventOn(Event e, String origin, boolean global, UUID... uuids) {
		if (uuids == null || 
			uuids.length == 0 || 
			(uuids.length == 1 && uuids[0] == null)) {
			executeEvent(e, origin);
		}
		
		if (uuids == null)
			uuids = new UUID[0];
		
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
				}))
				.collect(Collectors.toList());
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		allOf.join();
	}

	public boolean checkHandled(Class<? extends Event> clazz, EventListener listener) {
		List<EventController> li = listeners.get(clazz);
		if (li == null) {
			return false;
		}
		return li.stream().anyMatch(e -> e.getListener().equals(listener));
	}

	public List<EventController> getControllers(EventListener listener) {
		return listener.getEventControllers();
	}

	public Class<? extends Event> getEventType(EventController ec) {
		return controllerClasses.getOrDefault(ec, null);
	}

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
