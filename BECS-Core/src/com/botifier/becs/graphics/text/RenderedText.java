package com.botifier.becs.graphics.text;

import java.awt.Color;

import com.botifier.becs.graphics.Renderer;

public class RenderedText {
	private CharSequence cs;
	private float x, y;

	public RenderedText(CharSequence cs, float x, float y) {
		this.cs = cs;
		this.x = x;
		this.y = 0;
	}

	public CharSequence getText() {
		return cs;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public void draw(Renderer r, Font f, Color c) {
		f.draw(r, cs, x, y, c);
	}

	public void drawNoBegin(Renderer r, Font f, Color c) {
		f.drawNoBegin(r, cs, x, y, c);
	}
}
