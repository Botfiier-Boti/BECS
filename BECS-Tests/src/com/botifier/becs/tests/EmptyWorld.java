package com.botifier.becs.tests;

import com.botifier.becs.Game;
import com.botifier.becs.WorldState;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.util.shapes.RotatableRectangle;

public class EmptyWorld extends Game {

	public EmptyWorld() {
		super("Empty World", 480, 480, false, true); 
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		//setTitle("FPS: "+getTimer().getFPS()+ " UPS:"+getTimer().getUPS());

	}

	@Override
	public void draw(Renderer r, WorldState ws, RotatableRectangle rr, float alpha) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub

	}

}
