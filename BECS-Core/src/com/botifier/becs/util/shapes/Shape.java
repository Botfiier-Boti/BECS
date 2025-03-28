package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Objects;

import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;

/**
 * Shape
 *
 * TODO: Document this
 * TODO: Make immutable shapes
 * TODO: Find a way to have less shapes
 *
 * @author Botifier
 */
public abstract class Shape implements IShape, Cloneable, Externalizable {
	
	protected Vector2f center;

	protected float angle;

	/**
	 * Shape constructor
	 * @param x float X
	 * @param y float Y
	 */
	public Shape(float x, float y) {
		center = new Vector2f(x, y);
	}
	
	/**
	 * Read external shape
	 * @param in
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Shape(ObjectInput in) throws ClassNotFoundException, IOException {
		readExternal(in);
	}

	/**
	 * Shape constructor
	 * @param v Vector2f Center
	 */
	public Shape(Vector2f v) {
		center = new Vector2f(v);
	}

	public void setCenter(float x, float y) {
		center.set(x, y);
	}

	public Vector2f getCenter() {
		return center;
	}

	public float getRotation() {
		return angle;
	}

	public void drawImage(Renderer r, Image i) {
		drawImage(r, i, Color.white, false);
	}

	public void drawImage(Renderer r, Image i, Color c) {
		drawImage(r, i, c, false);
	}

	public void moveCenter(Vector2f v) {
		moveCenter(v.x, v.y);
	}

	public void moveCenter(float x, float y) {
		setCenter(center.x+x, center.y+y);
	}

	@Override
	public Shape clone() {
		try {
			return (Shape) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void rotate(float angle) {
		this.angle += angle;
	}

	@Override
	public void setAngle(float angle) {
		this.angle = angle;
	}

	public abstract Vector2f closestTo(Shape s);

	public abstract Vector2f closestTo(Vector2f v);

	public abstract boolean intersects(Shape s);

	public abstract Vector2f getDimensions();

	@Override
	public int hashCode() {
		return Objects.hash(angle, center);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Shape other = (Shape) obj;
		return Float.floatToIntBits(angle) == Float.floatToIntBits(other.angle) && Objects.equals(center, other.center);
	}
}
