package com.botifier.becs.graphics.drawing;

import com.botifier.becs.graphics.SpriteBatch;

public abstract class RenderObject {
	
	private boolean drawing = false;
	
	/**
	 * Abstract draw function
	 * @param sb SpriteBatch to use
	 * @return int Number of vertices 
	 */
	abstract int drawFunction(SpriteBatch sb);
	
	/***
	 * Draw this object
	 * @param sb SpriteBatch to use
	 * @return int 
	 */
	public int draw(SpriteBatch sb) {
		int output = -1;
		drawing = true;
		output = drawFunction(sb);
		drawing = false;
		
		return output;
	}
	
	public boolean isDrawing() {
		return drawing;
	}
}
