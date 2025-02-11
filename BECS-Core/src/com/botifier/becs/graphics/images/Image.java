package com.botifier.becs.graphics.images;

import java.awt.Color;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.UUID;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.util.ResourceManager;
import com.botifier.becs.util.shapes.Shape;

/**
 * Image class
 * @author Botifier
 */
public class Image {

	/**
	 * Default white pixel
	 */
	public static final Image WHITE_PIXEL = new Image(Texture.createColoredTexture(1, 1, Color.white));

	/**
	 * Image id
	 */
	private final UUID id;

	/**
	 * Image position
	 */
	private Vector2f pos;

	/**
	 * Image offset
	 */
	private Vector2f offset;

	/**
	 * Shape that this image will render to
	 * Also uses positional data of
	 */
	private Shape shape;

	/**
	 * Color filter of the image
	 * white leaves image as is
	 */
	private Color c = Color.white;

	/**
	 * Image texture
	 */
	private Texture texture;

	/**
	 * Image scalar
	 */
	private float scale = 1;

	/**
	 * Image width
	 */
	private float width;

	/**
	 * Image height
	 */
	private float height;

	/**
	 * Z position of the image
	 * could update everything to Vector3f but eh
	 */
	private float z = 0;

	/**
	 * Texture file location
	 */
	private String location;

	/**
	 * ShaderProgram for this image in particular
	 */
	private String sp = null;
	
	/**
	 * ShaderProgram buffer
	 */
	private ShaderProgram buffer = null;

	/**
	 * Image constructor
	 * @param location String Texture location
	 */
	public Image(String location) {
		this(Texture.loadTexture(location));
	}

	/**
	 * Image constructor
	 * @param location String Texture location
	 * @param width float Width
	 * @param height float Height
	 * @param tx float Offset x
	 * @param ty float Offset y
	 */
	public Image(String location, float width, float height, float tx, float ty) {
		this(Texture.loadTexture(location), width, height, tx, ty);
	}

	/**
	 * Image constructor
	 * @param t Texture Just a texture
	 */
	public Image(Texture t) {
		this(t, t.getWidth(), t.getHeight(), 0, 0);
	}

	/**
	 * Image Constructor
	 * @param t Texture To use
	 * @param width float Width
	 * @param height float Height
	 * @param tx float Offset x
	 * @param ty float Offset y
	 */
	public Image(Texture t, float width, float height, float tx, float ty) {
		this.setTexture(t);
		this.width = width;
		this.height = height;
		this.offset = new Vector2f(tx, ty);
		this.id = UUID.randomUUID();
	}

	/**
	 * Image Copy Constructor
	 * Deep copy
	 * @param i Image to steal from
	 */
	public Image(Image i) {
		this(i.getTexture(), i.width, i.height, i.offset.x, i.offset.y);
		this.pos = i.pos;
		this.setScale(i.getScale());
		this.setShaderProgram(i.getShaderProgramId());
		if (i.getShape() != null) {
			this.setShape(i.getShape().clone());
		}
	}

	/**
	 * Destroys the texture
	 */
	public void destroy() {
		texture.delete();
	}

	/**
	 * Draws image
	 * @param renderer Renderer to use
	 */
	public void draw(Renderer renderer) {
		if (shape != null) {
			shape.drawImage(renderer, this);
			return;
		}
		draw(renderer, pos.x, pos.y);
	}

	/**
	 * Draws image without begin and end wrapper
	 * Used for batching
	 * @param renderer Renderer To use
	 */
	public void drawNoBegin(Renderer renderer) {
		drawNoBegin(renderer, pos.x, pos.y, z, c);
	}

	/**
	 * Draws Image at location
	 * @param renderer Renderer To use
	 * @param x float X
	 * @param y float Y
	 */
	public void draw(Renderer renderer, float x, float y) {
		draw(renderer,x,y, z, c, ResourceManager.getShaderProgram(sp));
	}

	/**
	 * Draws Image at location with color filter
	 * @param renderer Renderer To use
	 * @param x float X
	 * @param y float Y
	 * @param z float Z
	 * @param c Color Color filter
	 */
	public void draw(Renderer renderer, float x, float y, float z, Color c) {
		draw(renderer,x,y, z,c, ResourceManager.getShaderProgram(sp));
	}

	/**
	 * Draws Image at location with color filter using a specific shader
	 * @param renderer Renderer To use
	 * @param x float X
	 * @param y float Y
	 * @param z float Z
	 * @param c Color Color filter
	 * @param sp ShaderProgram To use
	 */
	public void draw(Renderer renderer, float x, float y, float z, Color c, ShaderProgram sp) {
		renderer.begin(sp);
		drawNoBegin(renderer, x, y, z, c);
	    renderer.end();
	}
	/**
	 * Draws image without begin and end wrapper
	 * Used for batching
	 * @param renderer Renderer To use
	 * @param x float X
	 * @param y float Y
	 * @param z float Z
	 * @param c Color Color filter
	 */
	public void drawNoBegin(Renderer renderer, float x, float y, float z, Color c) {
		texture.bind();
	    renderer.drawImage(this, x, y, z, c);
	}

	/**
	 * Draws a subsection of the image
	 * @param renderer Renderer To use
	 * @param pos Vector2f location
	 * @param z float Z
	 * @param x float x position within the texture
	 * @param y float y position within the texture
	 * @param width float width of the image within the texture
	 * @param height float height of the image within the texture
	 * @param scale float Scalar
	 * @param c Color Color filter
	 */
	public void drawSection(Renderer renderer, Vector2f pos, float z, float x, float y, float width, float height, float scale, Color c) {
		texture.bind();
		renderer.drawSubImage(pos.x, pos.y, z, width, height, x, y, getWidth()*scale, getHeight()*scale, c);
	}

	/**
	 * Batch draws image
	 * @param renderer Renderer To use
	 * @param toRender Vector2f[] Array of positions
	 */
	public void drawBatched(Renderer renderer, Vector2f[] toRender) {
		texture.bind();
		renderer.begin();

		Arrays.stream(toRender).forEach(loc -> {
			renderer.drawImage(this, loc.x, loc.y, 0, Color.white);
		});
		renderer.end();
	}

	/**
	 * Batch draws image using a 2D array of positions
	 * @param renderer Renderer To use
	 * @param toRender Vector2f[][] Positions
	 */
	public void drawBatched(Renderer renderer, Vector2f[][] toRender) {
		texture.bind();
		renderer.begin();
		for (Vector2f[] element : toRender) {
			for (int j = 0; j < toRender[0].length; j++) {
				Vector2f loc = element[j];
				if (loc != null) {
					renderer.drawImage(this, loc.x, loc.y, 0, Color.white);
				}
			}
		}
		renderer.end();
	}

	/**
	 * Draws batched images with depth
	 * @param renderer Renderer To use
	 * @param toRender Vector3f[] Positions
	 */
	public void drawBatchedWithDepth(Renderer renderer, Vector3f[] toRender) {
		texture.bind();
		renderer.begin();

		for (Vector3f loc : toRender) {
			renderer.drawImage(this, loc.x, loc.y, loc.z, Color.white);
		}
		renderer.end();
	}

	/**
	 * Reloads the texture
	 */
	public void reload() {
		texture.delete();
		texture = Texture.loadTexture(location);
	}

	/**
	 * Binds the texture
	 */
	public void bind() {
		texture.bind();
	}

	/**
	 * Resets Image properties
	 */
	public void reset() {
		this.scale = 1;
		this.width = texture.getWidth();
		this.height = texture.getHeight();
		this.pos = new Vector2f();
		this.shape = null;
		this.sp = null;
		this.offset = new Vector2f();
		this.z = 0;
	}

	/**
	 * Sets the Z position
	 * @param z float Z
	 */
	public void setZ(float z) {
		this.z = z;
	}
	/**
	 * Sets the ShaderProgram
	 * @param sp ShaderProgram To use
	 */
	public void setShaderProgram(String sp) {
		this.sp = sp;
	}

	/**
	 * Sets the texture
	 * @param t Texture To use
	 */
	public void setTexture(Texture t) {
		this.texture = t;
	}

	/**
	 * Sets the shape
	 * @param shape Shape To use
	 */
	public void setShape(Shape shape) {
		this.shape = shape;
	}

	/**
	 * Sets the scalar
	 * @param scale float Scalar to use
	 */
	public void setScale(float scale) {
		this.scale = scale;
	}

	/**
	 * Sets the width
	 * @param width float Width to use
	 */
	public void setWidth(float width) {
		this.width = width;
	}

	/**
	 * Sets the height
	 * @param height float Height to use
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/**
	 * Sets the position
	 * @param x float X
	 * @param y float Y
	 */
	public void setPosition(float x, float y) {
		this.pos.set(x, y);
	}

	/**
	 * Sets the offset
	 * @param x float X
	 * @param y float Y
	 */
	public void setOffset(float x, float y) {
		this.offset.set(x, y);
	}

	/**
	 * Sets the color
	 * @param c Color To use
	 */
	public void setColor(Color c) {
		this.c = c;
	}

	/**
	 * Returns the offset
	 * @return Vector2f Offset
	 */
	public Vector2f getOffset() {
		return this.offset;
	}

	/**
	 * Returns the image id
	 * @return UUID id
	 */
	public UUID getUUID() {
		return id;
	}

	/**
	 * Returns the non-scaled width
	 * @return float Width
	 */
	public float getInternalWidth() {
		return width;
	}

	/**
	 * Returns the non-scaled height
	 * @return float Height
	 */
	public float getInternalHeight() {
		return height;
	}

	/**
	 * Returns the width
	 * @return float Width
	 */
	public float getWidth() {
		return width * scale;
	}

	/**
	 * Returns the height
	 * @return float Height
	 */
	public float getHeight() {
		return height * scale;
	}

	/**
	 * Returns the Z position
	 * @return float Z position
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Returns the ShaderProgram
	 * @return ShaderProgram
	 */
	public ShaderProgram getShaderProgram() {
		if (buffer == null && sp != null)
			buffer = ResourceManager.getShaderProgram(sp);
		return buffer;
	}

	public String getShaderProgramId() {
		return sp;
	}
	
	/**
	 * Returns the texture
	 * @return Texture
	 */
	public Texture getTexture() {
		return texture;
	}

	/**
	 * Returns the color
	 * @return Color
	 */
	public Color getColor() {
		return c;
	}

	/**
	 * Returns the position
	 * @return Vector2f Position
	 */
	public Vector2f getPosition() {
		return pos;
	}

	/**
	 * Returns the shape
	 * @return Shape
	 */
	public Shape getShape() {
		return shape;
	}

	/**
	 * Returns the scalar
	 * @return float Scalar
	 */
	public float getScale() {
		return scale;
	}

}
