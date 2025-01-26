/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2015-2017, Heiko Brumme
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.botifier.becs.graphics.text;

import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;
import static java.awt.Font.TRUETYPE_FONT;

import java.awt.Color;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.system.MemoryUtil;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.graphics.shader.ShaderProgram;

//Exact location obtained: https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/text/Font.java
//TODO: Replace this with something better
public class Font {

	private java.awt.Font font;

	private Map<Character, FontGlyph> glyphs = new HashMap<>();

	private final Texture texture;

	private int size = 16;

	private boolean antiAliasing = false;

	public Font() {
		this(new java.awt.Font(MONOSPACED, PLAIN, 16));
	}

	public Font(int size) {
		this(new java.awt.Font(MONOSPACED, PLAIN, size));
	}

	public Font(String name, int size) {
		this(new java.awt.Font(name, PLAIN, size), false);
	}

	public Font(String name, int type, int size) {
		this(new java.awt.Font(name, type, size), false);
	}

	public Font(String location) throws FontFormatException, IOException {
		this(java.awt.Font.createFont(TRUETYPE_FONT, new File(location)), false);
	}

	public Font(java.awt.Font f) {
		this(f, false);
	}

	public Font(java.awt.Font f, boolean antiAlias) {
		font = f;
		texture = createFontTexture(f, antiAlias);
	}

	private Texture createFontTexture(java.awt.Font f, boolean antiAliasing) {
		int iWidth = 0;
		int iHeight = 0;

		for (int b = 32; b < 256; b++) {
			if (b == 127) {
				continue;
			}
			char c = (char) b;
			BufferedImage bi = createGlyphImage(f, c, antiAliasing, Color.WHITE);
			if (bi == null) {
				continue;
			}

			iWidth += bi.getWidth();
			iHeight = Math.max(iHeight, bi.getHeight());
		}

		size = iHeight;

		BufferedImage i = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = i.createGraphics();

		int x = 0;

		for (int b = 32; b < 256; b++) {
			if (b == 127) {
				continue;
			}
			char c = (char) b;
			BufferedImage bi = createGlyphImage(f, c, antiAliasing, Color.WHITE);
			if (bi == null) {
				continue;
			}

			int cW = bi.getWidth();
			int cH = bi.getHeight();

			FontGlyph gl = new FontGlyph(cW, cH, x, i.getHeight() - cH, 0);
			g.drawImage(bi, x, 0, null);
			x += cW;
			glyphs.put(c, gl);
		}

		AffineTransform t = AffineTransform.getScaleInstance(1f, -1f);
		t.translate(0, -i.getHeight());
		AffineTransformOp op = new AffineTransformOp(t, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

		i = op.filter(i, null);

		int w = i.getWidth();
		int h = i.getHeight();

		int pixel[] = new int[w * h];
		i.getRGB(0, 0, w, h, pixel, 0, w);

		ByteBuffer b = MemoryUtil.memAlloc(w * h * 4);
		for (int y = 0; y < h; y++) {
			for (int e = 0; e < w; e++) {
				int pix = pixel[y * w + e];
				//R
				b.put(((byte) ((pix >> 16) & 0xFF)));
				//G
				b.put((byte) ((pix >> 8) & 0xFF));
				//B
				b.put((byte) (pix & 0xFF));
				//A
				b.put((byte) ((pix >> 24) & 0xFF));
			}
		}
		b.flip();

		Texture tex = Texture.createTexture(w, h, b);
		MemoryUtil.memFree(b);
		return tex;
	}

	private BufferedImage createGlyphImage(java.awt.Font f, char c, boolean antiAliasing, Color color) {
		BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g =  bi.createGraphics();
		if (antiAliasing) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(f);
		FontMetrics metrics = g.getFontMetrics();
		g.dispose();

		int cWidth = metrics.charWidth(c);
		int cHeight = metrics.getHeight();
		if (cWidth <= 0) {
			return null;
		}
		bi = new BufferedImage(cWidth, cHeight, BufferedImage.TYPE_INT_ARGB);
		g = bi.createGraphics();
		if (antiAliasing) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(f);
		g.setPaint(color);
		g.drawString(String.valueOf(c), 0, metrics.getAscent());
		g.dispose();
		return bi;
	}

	public int getWidth(CharSequence cs) {
		int width = 0;
		int lW = 0;

		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);

			if (c == '\n') {
				width = Math.max(width, lW);
				lW = 0;
			}
			if (c == '\r') {
				continue;
			}

			FontGlyph g = glyphs.get(c);
			lW += g.width;
		}

		width = Math.max(lW, width);
		return width;
	}

	public int getHeight(CharSequence cs) {
		int height = 0;
		int lineHeight = 0;

		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			if (c == '\n') {
				height += lineHeight;
				lineHeight = 0;
				continue;
			}
			if (c == '\r') {
				continue;
			}

			FontGlyph g = glyphs.get(c);
			if (g != null) {
				lineHeight = Math.max(lineHeight, g.height);
			}
		}

		height += lineHeight;
		return height;
	}


	public void draw(Renderer r, CharSequence cs, float x, float y, Color c) {
		draw(r, cs, x, y, c, null);
	}

	public void draw(Renderer r, CharSequence cs, float x, float y, Color c, ShaderProgram sp) {
		texture.bind();
		r.begin(sp);
		drawNoBegin(r, cs, x, y, c);
		r.end();
	}

	public void drawNoBegin(Renderer r, CharSequence cs, float x, float y, Color c) {
		int tH = getHeight(cs);

		float dX = x;
		float dY = y;
		if (tH > size) {
			dY += tH - size;
		}

		for (int i = 0; i < cs.length(); i++) {
			char ch = cs.charAt(i);
			if (ch == '\n') {
				dY -= size;
				dX = x;
				continue;
			}
			if (ch == '\r') {
				continue;
			}
			FontGlyph g = glyphs.get(ch);
			if (g == null) {
				continue;
			}
			r.drawTextureRegion(texture, dX, dY, 1, g.x, g.y, g.width, g.height, c);
			dX += g.width;
		}
	}

	public void delete() {
		glyphs.clear();
		texture.delete();
	}

	public void setAntiAlias(boolean value) {
		antiAliasing = value;
	}

	public boolean isAntiAlias() {
		return antiAliasing;
	}

	public Map<Character, FontGlyph> getGlyphs() {
		return glyphs;
	}

	public java.awt.Font getFont() {
		return font;
	}

}
