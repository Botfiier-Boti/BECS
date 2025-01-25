package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.Math2;

public class Circle extends Shape {

	/**
	 * The resolution when drawn/Polygonized
	 */
	private final int POLYGON_RESOLUTION = 360;

	/**
	 * The radius
	 */
	protected float radius;

	/**
	 * Circle constructor
	 * @param x float Center x
	 * @param y float Center y
	 * @param radius float Radius
	 */
	public Circle(float x, float y, float radius) {
		super(x, y);
		this.radius = radius;
	}

	/**
	 * Circle constructor
	 * @param v Vector2f Center
	 * @param radius float Radius
	 */
	public Circle(Vector2f v, float radius) {
		super(v);
		this.radius = radius;
	}

	/**
	 * Circle copy constructor
	 * @param c Circle To steal from
	 */
	public Circle(Circle c) {
		super(c.center);
		this.radius = c.radius;
	}
	
	public Circle(ObjectInput in) throws ClassNotFoundException, IOException {
		super(in);
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	@Override
	public boolean contains(Vector2f v) {
		return contains(v.x, v.y);
	}

	@Override
	public boolean contains(float x, float y) {
		float dist = center.distance(x, y);
		if (dist > radius) {
			return false;
		}
		return true;
	}

	@Override
	public void draw(Renderer r, Color c) {
		Image.WHITE_PIXEL.bind();
		r.begin();
			r.drawCircle(center.x, center.y, radius, c, 100, 1);
		r.end();
	}

	@Override
	public void drawRaw(Renderer r, Color c) {
		r.drawFilledCircle(center.x, center.y, radius, c, 100);
	}

	@Override
	public void drawFilled(Renderer r, Color c) {
		Image.WHITE_PIXEL.bind();
		r.begin();
			r.drawFilledCircle(center.x, center.y, radius, c, 100);
		r.end();
	}

	@Override
	public void drawImage(Renderer r, Image i, Color c, boolean flipped) {
		i.bind();
		r.begin();
			r.drawFilledCircle(center.x, center.y, radius, c, 100);
		r.end();
	}

	@Override
	public Vector2f closestTo(Shape s) {
		return closestTo(s.center);
	}

	@Override
	public boolean intersects(Shape s) {
		if (s instanceof Polygon) {
			return this.toPolygon().intersects((Polygon)s);
		}
		Vector2f c = s.closestTo(this);
		if (c.distance(center) <= radius) {
			return true;
		}

		float dX = c.x - center.x;
		float dY = c.y - center.y;

		return dX * dX + dY * dY <= radius * radius;
	}

	@Override
	public Vector2f getDimensions() {
		return new Vector2f(radius*2, radius*2);
	}

	@Override
	public Vector2f closestTo(Vector2f v) {
		float angle = Math2.calcAngle(center, v);

		float x = (float) (Math.cos(angle) * radius);
		float y = (float) (Math.sin(angle) * radius);
		return new Vector2f(center.x+x, center.y+y);
	}

	@Override
	public void drawImageNoBegin(Renderer r, Image i, Color c) {
		// TODO Auto-generated method stub
		i.bind();
			r.drawFilledCircle(center.x, center.y, radius, c, 100);
	}

	@Override
	public Circle clone() {
		return new Circle(this);
	}

	@Override
	public Polygon toPolygon() {
		Set<Vector2f> points = new HashSet<>();

		float angle = 0;
		final float MOVE_ANGLE = (float) (2*Math.PI/POLYGON_RESOLUTION);
		for (int i = 0; i < POLYGON_RESOLUTION; i++) {
			double x = center.x+Math.cos(angle)*radius;
			double y = center.y+Math.sin(angle)*radius;

			points.add(new Vector2f((float) x, (float) y));
			angle += MOVE_ANGLE;
		}

		Polygon hold = Polygon.createPolygon(points.toArray(new Vector2f[0]));

		return hold;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(center, radius);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj) || (getClass() != obj.getClass())) {
			return false;
		}
		Circle other = (Circle) obj;
		return Float.floatToIntBits(radius) == Float.floatToIntBits(other.radius) && this.getCenter().equals(other.getCenter());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(getCenter());
		out.writeFloat(radius);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		center = (Vector2f) in.readObject();
		radius = in.readFloat();
	}

}
