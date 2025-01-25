package com.botifier.becs.graphics.images;

import java.awt.Color;

import com.botifier.becs.graphics.Renderer;

public class SubImage {
	private final float oX;
	private final float oY;
	private final float width;
	private final float height;

	public SubImage(float oX, float oY, float width, float height) {
		this.oX = oX;
		this.oY = oY;
		this.width = width;
		this.height = height;
	}

	public void draw(Renderer r, Texture t, float x, float y, Color c) {
		draw(r, t, x, y, 0, c);
	}

	public void draw(Renderer r, Texture t, float x, float y, float z, Color c) {
		t.bind();
		r.begin();
		r.drawSubImage(x, y, z, t.getWidth(), t.getHeight(), oX, oY, width, height, c);
		r.end();
	}

	public float getOffsetX() {
		return oX;
	}

	public float getOffsetY() {
		return oY;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}
