package com.botifier.becs.events.listeners;

import java.util.UUID;

import org.joml.Vector2f;

import com.botifier.becs.Game;
import com.botifier.becs.events.EntityComponentRemovedEvent;
import com.botifier.becs.events.EntityComponentUpdatedEvent;
import com.botifier.becs.util.annotations.EventHandler;
import com.botifier.becs.util.events.EventListener;
import com.botifier.becs.util.render.Camera;

public class CameraListener extends EventListener {

	private Camera c;
	
	public CameraListener(Camera c, UUID entity) {
		super(entity);
		this.c = c;
	}
	
	@EventHandler(event = EntityComponentRemovedEvent.class, origin = "Position")
	public void onPositionComponentRemoved(EntityComponentRemovedEvent<Vector2f> e) {
		c.setFollowEntity(null);
		Game.getCurrent().getEventManager().unregisterListener(this);
	}
	

	@EventHandler(event = EntityComponentUpdatedEvent.class, origin = "Position")
	public void onMove(EntityComponentUpdatedEvent<Vector2f> e) {
		c.setCenter(e.getNewValue());
	}
	
}
