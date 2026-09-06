package com.botifier.becs.graphics.drawing;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.SpriteBatch;

public record RectangleObject (float x, float y, float width, float height, float rotation) implements RenderObject {

	@Override
	public int drawFunction(Renderer r) {
		return 0;
	}

}
