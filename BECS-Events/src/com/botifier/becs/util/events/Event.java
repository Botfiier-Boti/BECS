package com.botifier.becs.util.events;

/**
 * Event class
 */
public abstract class Event {
	private final long timestamp;
	
	{
		timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
}