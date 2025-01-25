package com.botifier.becs.events.listeners;

import com.botifier.becs.util.events.EventListener;
import com.botifier.becs.util.shapes.Shape;

import org.joml.Vector2f;
import org.joml.Vector2fc;

import com.botifier.becs.entity.*;
import com.botifier.becs.events.*;
import com.botifier.becs.util.annotations.EventHandler;

public class WorldListener extends EventListener {

	@EventHandler(event = EntityComponentAddedEvent.class, origin = "CollisionShape")
	public void onComponentAdded(EntityComponentAddedEvent<Shape> e) {
		Entity en = e.getTarget();
		
		if (en.hasComponent("Position")) {
			EntityComponent<Shape> s = e.getComponent();
			EntityComponent<Vector2f> pC = en.getComponent("Position");
			Vector2f p = pC.get();

			Shape sh = s.get();
			sh.setCenter(p.x, p.y);
			e.getComponent().set(sh);

			Entity.spatialMap().addEntity(en);
		}
	}
	
	@EventHandler(event = EntityComponentUpdatedEvent.class, origin="Velocity")
	public void onVectorUpdate(EntityComponentUpdatedEvent<Vector2fc> e) {
		if (!e.getNewValue().equals(new Vector2f(), 0.001f)) {
			Entity.spatialMap().wakeEntity(e.getComponent().getOwner());
		}
	}
}
