package com.botifier.becs.graphics.text;

//I don't know if this in particular needs to be cited because this is stupidly simple
//Same creator as Font @Heiko Brumme
public class FontGlyph {

	public final int width;
	public final int height;
	public final int x;
	public final int y;
	public final float advance;

	public FontGlyph(int width, int height, int x, int y, float advance) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		this.advance = advance;
	}

}
