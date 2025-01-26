package com.botifier.becs.graphics.images;

import java.awt.Color;

import com.botifier.becs.graphics.Renderer;

/**
 * SubImage class
 * @author Botifier
 */
public class SubImage {
	private final float oX;
	private final float oY;
	private final float width;
	private final float height;

	/**
	 * SubImage constructor
	 * @param oX float Internal x
	 * @param oY float Internal y
	 * @param width float Internal width
	 * @param height float Internal Height
	 */
	public SubImage(float oX, float oY, float width, float height) {
		this.oX = oX;
		this.oY = oY;
		this.width = width;
		this.height = height;
	}

	/**
	 * Draw the sub image using specified texture
	 * @param r Renderer To use
	 * @param t Texture To use
	 * @param x float To use
	 * @param y float To use
	 * @param c Color To use
	 */
	public void draw(Renderer r, Texture t, float x, float y, Color c) {
		draw(r, t, x, y, 0, c);
	}

	/**
	 * Draw the sub image using specified texture
	 * @param r Renderer To use
	 * @param t Texture To use
	 * @param x float To use
	 * @param y float To use
	 * @param z float To use
	 * @param c Color To use
	 */
	public void draw(Renderer r, Texture t, float x, float y, float z, Color c) {
		t.bind();
		r.begin();
		r.drawSubImage(x, y, z, t.getWidth(), t.getHeight(), oX, oY, width, height, c);
		r.end();
	}

	/**
	 * Gets the x offset
	 * @return float The x offset
	 */
	public float getOffsetX() {
		return oX;
	}

	/**
	 * Gets the y offset
	 * @return float The y offset
	 */
	public float getOffsetY() {
		return oY;
	}

	/**
	 * Gets the sub width
	 * @return float Sub width
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Gets the sub height
	 * @return float Sub height
	 */
	public float getHeight() {
		return height;
	}
}
