package com.botifier.becs.util.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * EventController
 * 
 * Holds method information
 * 
 * @author Botifier
 */
public class EventController {
	private final EventListener listener;
	private final Method method;
	private final int priority;
	private final String target;

	public EventController(EventListener listener, Method method, int priority, String target) {
		this.listener = listener;
		this.method = method;
		this.priority = priority;
		this.target = target;
	}

	public void invoke(Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		this.method.invoke(listener, event);
	}

	public EventListener getListener() {
		return this.listener;
	}

	public int getPriority() {
		return this.priority;
	}
	
	public String getTarget() {
		return this.target;
	}
	
	@Override
	public String toString() {
		return String.format("Listener: %s\nMethod: %s\nPriority: %s\nTarget: %s", listener.getUUID(), Arrays.toString(method.getGenericParameterTypes()), priority, target);
	}
}
