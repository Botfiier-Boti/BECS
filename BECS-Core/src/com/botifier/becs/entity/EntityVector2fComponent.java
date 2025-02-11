package com.botifier.becs.entity;

import java.util.function.UnaryOperator;

import org.joml.Vector2f;

import com.botifier.becs.Game;
import com.botifier.becs.events.EntityComponentUpdatedEvent;

public class EntityVector2fComponent extends EntityComponent<Vector2f>{
	
	/**
	 * How much different a new vector can be before it is considered truly updated
	 */
	final float LEEWAY = 0.0001f;
	
	/**
	 * A EntityComponent Wrapper for Vector2f
	 * @param name String name of the component
	 * @param owner Entity owner of the component
	 * @param info Vector2f The vector stored in this component
	 */
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
