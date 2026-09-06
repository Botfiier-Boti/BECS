package com.botifier.becs.entity;

import org.joml.Vector2fc;

public class EntityVector2fcComponent extends EntityComponent<Vector2fc>{
	
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
	public EntityVector2fcComponent(String name, Entity owner, Vector2fc info) {
		super(name, owner, info);
	}

	@Override
	public boolean shouldFireEvent(Vector2fc v1, Vector2fc v2) {
		return !v1.equals(v2, LEEWAY);
	}
	
}
