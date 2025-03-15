
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

import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
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
import static org.lwjgl.system.MemoryStack.stackPush;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Texture {

	/**
	 * List of all loaded textures
	 **/
	private static ConcurrentHashMap<String, Texture> textureLocations = new ConcurrentHashMap<>();

	/**
	 * The texture id
	 */
	private final int id;

	/**
	 * The width of the texture
	 */
	private int width;

	/**
	 * The height of the texture
	 */
	private int height;

	/**
	 * The texture data
	 */
	private ByteBuffer image;

	/**
	 * Where the texture came from
	 */
	private String location = "";

	/**
	 * Whether or not the buffer was purged
	 */
	private boolean exists = true;

	/**
	 * Texture constructor
	 */
	private Texture() {
		id = glGenTextures();
	}

	/**
	 * Binds the texture
	 */
	public void bind() {
		glBindTexture(GL_TEXTURE_2D, getId());
	}

	/**
	 * Sets specified texture parameter
	 * 
	 * @param name  int Parameter id
	 * @param value int Value
	 */
	public void setParameter(int name, int value) {
		glTexParameteri(GL_TEXTURE_2D, name, value);
	}

	/**
	 * Uploads texture data Uses RGBA8
	 * 
	 * @param width  int Texture width
	 * @param height int Texture height
	 * @param data   ByteBuffer Texture data
	 */
	public void uploadData(int width, int height, ByteBuffer data) {
		uploadData(GL_RGBA8, width, height, GL_RGBA, data);
	}

	/**
	 * Uploads texture data
	 * 
	 * @param internalFormat int Internal texture format
	 * @param width          int Texture width
	 * @param height         int Texture height
	 * @param format         int Texture format
	 * @param data           ByteBuffer Texture data
	 */
	public void uploadData(int internalFormat, int width, int height, int format, ByteBuffer data) {
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data);
	}

	/**
	 * Destroys this texture
	 */
	public void delete() {
		if (exists) {
			System.out.println("Attempting to delete textures at ID: " + getId());
			glDeleteTextures(getId());
			System.out.println("Textures at ID:" + getId() + " deleted.");
			exists = false;
		}
	}

	public void write(File target) throws IOException {
		bind();
	    
	    // Ensure we're working with the correct size buffer
	    int bufferSize = width * height * 4;
	    ByteBuffer buffer = BufferUtils.createByteBuffer(bufferSize);
	    
	    // Read the texture data with proper alignment
	    GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);  // Ensure 1-byte alignment
	    GL11.glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
	    
	    // Unbind texture
	    glBindTexture(GL_TEXTURE_2D, 0);
	    
	    // Create an image with the correct color model
	    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    
	    // Rewind the buffer to read from the start
	    buffer.rewind();
	    
	    // Read pixel data with correct byte ordering
	    int[] pixels = new int[width * height];
	    for (int y = 0; y < height; y++) {
	        for (int x = 0; x < width; x++) {
	            // Calculate buffer position for vertically flipped coordinates
	            int bufferPos = ((height - 1 - y) * width + x) * 4;
	            
	            // Get RGBA values
	            int r = buffer.get(bufferPos) & 0xFF;
	            int g = buffer.get(bufferPos + 1) & 0xFF;
	            int b = buffer.get(bufferPos + 2) & 0xFF;
	            int a = buffer.get(bufferPos + 3) & 0xFF;
	            
	            // Store in pixel array with correct orientation
	            pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
	        }
	    }
	    
	    // Set the pixel data directly
	    image.setRGB(0, 0, width, height, pixels, 0, width);
	    
	    // Write using PNG format to preserve transparency
	    ImageIO.write(image, "png", target);
	}

	/**
	 * Returns the texture's width
	 * 
	 * @return int Texture width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the texture's height
	 * 
	 * @return int Texture height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Changes the texture's width
	 * 
	 * @param width int Width to use
	 */
	public void setWidth(int width) {
		if (width > 0) {
			this.width = width;
		}
	}

	/**
	 * Changes the texture's height
	 * 
	 * @param height int Height to use
	 */
	public void setHeight(int height) {
		if (height > 0) {
			this.height = height;
		}
	}

	/**
	 * Creates a texture using a supplied byte buffer
	 * 
	 * @param width    int Texture width
	 * @param height   int Texture height
	 * @param data     ByteBuffer Texture data
	 * @param location String Texture origin; can be null
	 * @return Texture
	 */
	public static Texture createTexture(int width, int height, ByteBuffer data, String location) {
		Texture t = new Texture();
		t.setWidth(width);
		t.setHeight(height);

		t.bind();
		t.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		t.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		t.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		t.image = data;
		t.uploadData(GL_RGBA8, width, height, GL_RGBA, data);

		if (location == null || location.isBlank())
			t.location = "**internal**:" + t.hashCode();
		else
			t.location = location;

		Texture old = textureLocations.putIfAbsent(t.location, t);
		return old != null ? old : t;
	}

	/**
	 * Creates a texture using a supplied byte buffer
	 * 
	 * @param width  int Texture width
	 * @param height int Texture height
	 * @param data   ByteBuffer Texture data
	 * @return Texture
	 */
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
		t.location = "**internal**:" + (t.hashCode());

		Texture old = textureLocations.putIfAbsent(t.location, t);
		return old != null ? old : t;
	}

	/**
	 * Creates a texture of a single color
	 * 
	 * @param width  int Texture width
	 * @param height int Texture height
	 * @param c      Color To use
	 * @return Texture
	 */
	public static Texture createColoredTexture(int width, int height, Color c) {
		String locationString = "**internal**:" + c.hashCode();
		ByteBuffer image = null;
		if (textureLocations.containsKey(locationString)) {
			Texture t = textureLocations.get(locationString);
			return t;
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {

			image = stack.calloc(width * height * 4);

			for (int y = height - 1; y >= 0; y--) {
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

	/**
	 * 
	 * @param bi   BufferedImage To convert
	 * @param path String Original path
	 * @return Texture
	 */
	private static Texture loadTexture(BufferedImage bi, String path) {
		ByteBuffer image = null;

		if (textureLocations.containsKey(path)) {
			Texture t = textureLocations.get(path);
			return t;
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			int pixels[] = new int[bi.getWidth() * bi.getHeight()];
			bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), pixels, 0, bi.getWidth());

			image = stack.calloc(bi.getWidth() * bi.getHeight() * 4);

			for (int y = bi.getHeight() - 1; y >= 0; y--) {
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

	/**
	 * Loads a texture from an internal path
	 * 
	 * @param path String Path to check
	 * @return Texture
	 */
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

	/**
	 * Loads a texture from an external path
	 * 
	 * @param path String Path to check
	 * @return Texture
	 */
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

	/**
	 * Deletes all of the loaded textures
	 */
	public static void purgeTextures() {
		for (Entry<String, Texture> e : textureLocations.entrySet()) {
			Texture t = e.getValue();

			textureLocations.remove(e.getKey());
			t.delete();
		}
	}

	/**
	 * Gets the origin of the texture
	 * 
	 * @return String The origin
	 */
	public String getOriginLocation() {
		return location;
	}

	/**
	 * Gets the texture's buffer
	 * 
	 * @return ByteBuffer The buffer
	 */
	public ByteBuffer getBuffer() {
		return image;
	}
	
	public GLFWImage asGLFWImage() {
		try (MemoryStack stack = stackPush()) {
			GLFWImage image = GLFWImage.malloc(stack);
			image.set(getWidth(), getHeight(), getBuffer());
			return image;
		}
	}

	/**
	 * Gets the texture's id
	 * 
	 * @return int The texture's id
	 */
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
