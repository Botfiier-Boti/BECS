package com.botifier.becs.tests;

import java.io.IOException;
import java.io.ObjectInput;

import org.joml.Vector2f;
import org.joml.Vector2fc;

import com.botifier.becs.entity.Entity;
import com.botifier.becs.events.listeners.PhysicsListener;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.shapes.RotatableRectangle;

public class TestPlayer extends Entity {
	protected Vector2f initialPos;
	
	
	public TestPlayer(float x, float y) {
		super("Player");
		initialPos = new Vector2f(x, y);
	}
	
	@Override
	public void init() {
		Image i = new Image("christmasfroggy.png");
		i.setZ(1);
		i.setScale(0.25f);

		RotatableRectangle rr = new RotatableRectangle(initialPos.x, initialPos.y, i.getWidth(), i.getHeight(), 0);
		i.setShape(rr);
		addComponent("Position", new Vector2f(initialPos));
		addComponent("Image", i);
		addComponent("Velocity", new Vector2f(0,0));
		addComponent("PhysicsEnabled", new PhysicsListener());
		addComponent("CollisionShape", rr);
		addComponent("Speed", 10f);
		addComponent("Collidable", true);
		addComponent("Solid", true);
		addComponent("ArrowKeyControlled", true);
	}
	
	

}
