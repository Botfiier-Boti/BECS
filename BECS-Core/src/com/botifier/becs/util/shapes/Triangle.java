package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.joml.Vector2f;
import org.joml.primitives.Intersectionf;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;

public class Triangle extends Shape {

	public Vector2f p1;
	public Vector2f p2;
	public Vector2f p3;

	public Triangle(Vector2f center, Vector2f p1, Vector2f p2, Vector2f p3) {
		super (center);
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}

	public Triangle(float x, float y, float x1, float y1, float x2, float y2, float x3, float y3) {
		super(x, y);
		this.p1 = new Vector2f(x1, y1);
		this.p2 = new Vector2f(x2, y2);
		this.p3 = new Vector2f(x3, y3);
	}

	@Override
	public boolean contains(Vector2f v) {
		return Intersectionf.testPointTriangle(v, p1, p2, p3);
	}

	@Override
	public boolean contains(float x, float y) {
		return Intersectionf.testPointTriangle(x, y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
	}

	@Override
	public void draw(Renderer r, Color c) {

	}

	@Override
	public void drawFilled(Renderer r, Color c) {

	}

	@Override
	public void drawImage(Renderer r, Image i, Color c, boolean flipped) {

	}

	@Override
	public void drawRaw(Renderer r, Color c) {

	}

	@Override
	public void drawImageNoBegin(Renderer r, Image i, Color c) {

	}

	@Override
	public Polygon toPolygon() {
		return null;
	}

	@Override
	public Vector2f closestTo(Shape s) {
		return null;
	}

	@Override
	public Vector2f closestTo(Vector2f v) {
		return null;
	}

	@Override
	public boolean intersects(Shape s) {
		return false;
	}

	@Override
	public Vector2f getDimensions() {
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

}
