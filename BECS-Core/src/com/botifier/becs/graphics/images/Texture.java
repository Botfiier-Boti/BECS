
/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2014-2017, Heiko Brumme
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

package com.botifier.becs.graphics.images;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Texture {

	/**
	 * List of all loaded textures
	 **/
	private static ConcurrentHashMap<String, Texture> textureLocations = new ConcurrentHashMap<>();

	private final int id;

	private int width;

	private int height;

	private ByteBuffer image;
	
	private String location = "";

	private boolean exists = true;

	private Texture() {
		id = glGenTextures();
	}

	public void bind() {
		glBindTexture(GL_TEXTURE_2D, getId());
	}

	public void setParameter(int name, int value) {
		glTexParameteri(GL_TEXTURE_2D, name, value);
	}

	public void uploadData(int width, int height, ByteBuffer data) {
		uploadData(GL_RGBA8, width, height, GL_RGBA, data);
	}

	public void uploadData(int internalFormat, int width, int height, int format, ByteBuffer data) {
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data);
	}

	public void delete() {
		if (exists) {
			System.out.println("Attempting to delete textures at ID: "+getId()); 
			glDeleteTextures(getId());
			System.out.println("Textures at ID:"+getId()+" deleted.");  
			exists = false;
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		if (width > 0) {
			this.width = width;
		}
	}

	public void setHeight(int height) {
		if (height > 0) {
			this.height = height;
		}
	}

	public static Texture createTexture(int width, int height, ByteBuffer data, String location) {
		Texture t = new Texture();
		t.setWidth(width);
		t.setHeight(height);

		t.bind();
		t.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		t.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		t.uploadData(GL_RGBA8, width, height, GL_RGBA, data);

		t.image = data;
		if (location == null || location.isBlank())
			t.location = "**internal**:"+t.hashCode();
		else
			t.location = location;
		if (!textureLocations.containsKey(t.location)) {
			textureLocations.put(t.location, t);
		}

		return t;
	}
	
	public static Texture createTexture(int width, int height, ByteBuffer data) {
		Texture t = new Texture();
		t.setWidth(width);
		t.setHeight(height);

		t.bind();
		t.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		t.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		t.uploadData(GL_RGBA8, width, height, GL_RGBA, data);

		t.image = data;
		t.location = "**internal**:"+(t.hashCode());
		if (!textureLocations.containsKey(t.location)) {
			textureLocations.put(t.location, t);
		}

		return t;
	}

	public static Texture createColoredTexture(int width, int height, Color c) {
		String locationString = "**internal**:"+c.hashCode();
		ByteBuffer image = null;
		if (textureLocations.containsKey(locationString)) {
			Texture t = textureLocations.get(locationString);
			return t;
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {

			image = BufferUtils.createByteBuffer(width * height * 4);

			for (int y = height-1; y >= 0; y--) {
				for (int x = 0; x < width; x++) {
					image.put((byte) (c.getRed() & 0xFF));
					image.put((byte) (c.getGreen() & 0xFF));
					image.put((byte) (c.getBlue() & 0xFF));
					image.put((byte) (c.getAlpha() & 0xFF));
				}
			}

			image.flip();
		}
		Texture t = createTexture(width, height, image, locationString);
		return t;
	}

	private static Texture loadTexture(BufferedImage bi, String path) {
		ByteBuffer image = null;
		
		if (textureLocations.containsKey(path)) {
			Texture t = textureLocations.get(path);
			return t;
		}
		
		try (MemoryStack stack = MemoryStack.stackPush()) {
			int pixels[] = new int[bi.getWidth() * bi.getHeight()];
			bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), pixels, 0, bi.getWidth());

			image = BufferUtils.createByteBuffer(bi.getWidth() * bi.getHeight() * 4);

			for (int y = bi.getHeight()-1; y >= 0; y--) {
				for (int x = 0; x < bi.getWidth(); x++) {
					int pixel = pixels[y * bi.getWidth() + x];
					image.put((byte) ((pixel >> 16) & 0xFF));
					image.put((byte) ((pixel >> 8) & 0xFF));
					image.put((byte) (pixel & 0xFF));
					image.put((byte) ((pixel >> 24) & 0xFF));
				}
			}

			image.flip();
		}
		Texture t = createTexture(bi.getWidth(), bi.getHeight(), image, path);
		
		return t;
	}

	public static Texture loadTexture(String path) {
		ClassLoader cl = Image.class.getClassLoader();
		Texture t = null;
		try {
			t = loadTexture(ImageIO.read(cl.getResourceAsStream(path)), path);
			t.location = path;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return t;
	}
	
	public static Texture loadTextureExternal(String path) {
		Texture t = null;
		try {
			File f = new File(path);
			InputStream fis = new FileInputStream(f);
			t = loadTexture(ImageIO.read(fis), path);
		} catch (IOException e) {
			e.printStackTrace();
		}


		return t;

	}

	public static void purgeTextures() {
		for (Entry<String, Texture> e : textureLocations.entrySet()) {
			Texture t = e.getValue();
			
			textureLocations.remove(e.getKey());
			t.delete();
		}
	}
	
	public String getOriginLocation() {
		return location;
	}

	public ByteBuffer getBuffer() {
		return image;
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(height, id, width);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Texture other = (Texture) obj;
		return height == other.height && id == other.id && width == other.width;
	}

}
