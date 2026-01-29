package com.botifier.becs.graphics.drawing;

import com.botifier.becs.graphics.SpriteBatch;

public abstract class RenderObject {
	
	private volatile boolean drawing = false;
	
	/**
	 * Abstract draw function
	 * @param sb SpriteBatch to use
	 * @return int Number of vertices 
	 */
	abstract int drawFunction(SpriteBatch sb);
	

	protected int preDrawFunction(SpriteBatch sb) { return 0; };
	protected int postDrawFunction(SpriteBatch sb) { return 0; };
	
	/***
	 * Draw this object
	 * @param sb SpriteBatch to use
	 * @return int 
	 */
	public final int draw(SpriteBatch sb) {
		int output = -1;
		drawing = true;
		try { 
			output = preDrawFunction(sb);
			output += drawFunction(sb);
			output += postDrawFunction(sb);
		} finally {
			drawing = false;
		}
		
		return output;
	}
	
	public boolean isDrawing() {
		return drawing;
	}
}
