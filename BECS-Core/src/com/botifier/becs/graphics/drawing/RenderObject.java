package com.botifier.becs.graphics.drawing;

import com.botifier.becs.graphics.Renderer;

public interface RenderObject {
	
	/**
	 * Abstract draw function
	 * @param sb SpriteBatch to use
	 * @return int Number of vertices 
	 */
	public int drawFunction(Renderer r);
	
	default int preDrawFunction(Renderer r) { return 0; };
	default int postDrawFunction(Renderer r) { return 0; };
	
	/***
	 * Draw this object
	 * @param sb SpriteBatch to use
	 * @return int 
	 */
	default int draw(Renderer r) {
		int output = preDrawFunction(r);
		output += drawFunction(r);
		output += postDrawFunction(r);
		return output;
	}
	
}
