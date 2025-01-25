package com.botifier.becs.entity;

import java.util.function.UnaryOperator;

import org.joml.Vector2f;
import org.joml.Vector2fc;

import com.botifier.becs.Game;
import com.botifier.becs.events.EntityComponentUpdatedEvent;

public class EntityVector2fComponent extends EntityComponent<Vector2f>{
	
	final float LEEWAY = 0.0001f;
	
	public EntityVector2fComponent(String name, Entity owner, Vector2f info) {
		super(name, owner, info);
	}

	@Override
	public void set(Vector2f info) {
		if (!info.equals(get(), LEEWAY)) {
			Game.getCurrent().getEventManager().executeEventOn(new EntityComponentUpdatedEvent<Vector2f>(this, get(), info),
																   getName(),
																   getOwnerUUID());
		}
		information.set(info);
	}
	
	@Override
	public Vector2f update(UnaryOperator<Vector2f> updater) {
		Vector2f old = get();
		Vector2f result = information.updateAndGet(updater);
		if (!result.equals(get(), LEEWAY))
			Game.getCurrent().getEventManager().executeEventOn(new EntityComponentUpdatedEvent<Vector2f>(this, old, result),
																   getName(),
																   getOwnerUUID());
		return result;
	}
}
