package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.joml.Intersectionf;
import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.Math2;

public class Line extends Shape {
	Vector2f p1, p2;

	public Line(Vector2f p1, Vector2f p2) {
		super(Math2.getMidpoint(p1, p2));
		init(new Vector2f(p1), new Vector2f(p2));
	}

	public Line(float x1, float y1, float x2, float y2) {
		super(Math2.getMidpoint(x1, y1, x2, y2));
		init(new Vector2f(x1, y1), new Vector2f(x2, y2));
	}
	
	public Line(ObjectInput in) throws ClassNotFoundException, IOException {
		super(in);
	}

	private void init(Vector2f p1, Vector2f p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public void setPoint1(float x, float y) {
		p1.x = x;
		p1.y = y;
		center = Math2.getMidpoint(p1, p2);
	}

	public void setPoint2(float x, float y) {
		p2.x = x;
		p2.y = y;
		center = Math2.getMidpoint(p1, p2);
	}

	public Vector2f getPoint1() {
		return p1;
	}

	public Vector2f getPoint2() {
		return p2;
	}

	public Vector2f lengthDirec() {
		float xLen = Math.abs(p2.x - p1.x);
		float yLen = Math.abs(p2.y - p1.y);
		return new Vector2f(xLen, yLen);
	}

	public float length() {
		return p2.distance(p1);
	}

	public List<Vector2f> getPointsOfIntersection(Shape s) {
		if (s instanceof Line) {
			Vector2f intersect = getPointOfIntersection((Line) s);
			return intersect != null ? new ArrayList<>(Arrays.asList(intersect)) : null;
		}
		Polygon test = s.toPolygon();
		List<Vector2f> vectors = new ArrayList<>();
		for (Line l : test.getEdges()) {
			Vector2f hold = l.getPointOfIntersection(this);
			if (hold != null) {
				vectors.add(hold);
			}
		}
		return vectors;
	}

	public Vector2f getPointOfIntersection(Line l) {
		Vector2f hold = new Vector2f();
		boolean intersects = Intersectionf.intersectLineLine(p1.x, p1.y, p2.x, p2.y, l.p1.x, l.p1.y, l.p2.x, l.p2.y, hold);
		return intersects ? hold : null;
	}

	public Vector2f getDirection() {
		return new Vector2f(p2).sub(p1).normalize();
	}

	@Override
	public boolean intersects(Shape s) {
		return intersects(s, new Vector2f());
	}

	public boolean intersects(Shape s, Vector2f point) {
		if (s instanceof Line) {
			Line l = (Line)s;
			boolean result = Intersectionf.intersectLineLine(p1.x, p1.y, p2.x, p2.y, l.p1.x, l.p1.y, l.p2.x, l.p2.y, point);
			return result;
		}

		boolean intersection = false;
		List<Line> edges = s.toPolygon().getEdges();
		for (Line l : edges) {
			intersection = l.intersects(this);
			if (intersection) {
				return intersection;
			}
		}
		return intersection;
	}

	@Override
	public boolean contains(Vector2f v) {
		return contains(v.x, v.y);
	}

	@Override
	public boolean contains(float x, float y) {
		Vector2f bigger = new Vector2f();
		Vector2f smaller = new Vector2f();
		p1.max(p2, bigger);
		p1.min(p2, smaller);

		boolean xB = x <= bigger.x && x >= smaller.x;
		boolean yB = y <= bigger.y && y >= smaller.y;

		return xB && yB;
	}

	@Override
	public void draw(Renderer r, Color c) {
		r.begin();
			drawRaw(r, c);
		r.end();
	}

	@Override
	public void drawRaw(Renderer r, Color c) {
		r.drawLine(p1.x, p1.y, p2.x, p2.y, c, 4);
	}

	@Override
	public void rotate(float angle) {
		setAngle(angle);
	}

	@Override
	public void setAngle(float angle) {
		super.setAngle(angle);
		p1 = Math2.rotatePoint(p1, angle);
		p2 = Math2.rotatePoint(p2, angle);
	}

	@Override
	public void drawFilled(Renderer r, Color c) {
		draw(r, c);
	}

	@Override
	public void drawImage(Renderer r, Image i, Color c, boolean flipped) {
		draw(r, c);
	}

	@Override
	public Vector2f getDimensions() {
		return new Vector2f(Math2.xDist(p1, p2), Math2.yDist(p1, p2));
	}

	@Override
	public Vector2f closestTo(Shape s) {
		if (s == null) {
			return null;
		}

		//I want to do this with switch. Its not possible in java 17.
		if (s instanceof Line) {
			Line other = (Line) s;

			Vector2f tDir = this.getDirection();
			Vector2f oDir = other.getDirection();

			float dot = Math.abs(tDir.dot(oDir));

			if (Math.abs(dot - 1) < 0.0001f) {
				if (contains(other.p1) || contains(other.p2) ||
					other.contains(p1) || other.contains(p2)) {
					return getCenter();
				}
			}

			Vector2f thisClosest1 = closestTo(other.p1);
			Vector2f thisClosest2 = closestTo(other.p2);
			Vector2f otherClosest1 = other.closestTo(p1);
			Vector2f otherClosest2 = other.closestTo(p2);

			float d1 = thisClosest1.distance(other.p1);
			float d2 = thisClosest2.distance(other.p2);
			float d3 = otherClosest1.distance(p1);
			float d4 = otherClosest2.distance(p2);

			float minDist = Math2.min(d1, d2, d3, d4);

			if (minDist == d1) {
				return thisClosest1;
			}
			if (minDist == d2) {
				return thisClosest2;
			}
			if (minDist == d3) {
				return p1;
			}
			return p2;
		}

		//Everything else
		Vector2f[] points = s.toPolygon().getPoints();
		if (points.length == 0) {
			return null;
		}

		Vector2f cPoint = closestTo(points[0]);
		float minDist = cPoint.distance(points[0]);

		for (int i = 1; i < points.length; i++) {
			Vector2f crClose = closestTo(points[i]);
			float crDist = crClose.distance(points[i]);

			if (crDist < minDist) {
				minDist = crDist;
				cPoint.set(crClose);
			}
		}

		return cPoint;
	}

	@Override
	public Vector2f closestTo(Vector2f v) {
		Vector2f lineVec = new Vector2f(p2).sub(p1);
		Vector2f pVec = new Vector2f(v).sub(p1);

		float lenSq = lineVec.lengthSquared();

		if (lenSq == 0 ) {
			return new Vector2f(p1);
		}

		float t = pVec.dot(lineVec) / lenSq;

		t = Math.max(0, Math.min(1, t));

		return new Vector2f(p1).add(new Vector2f(lineVec).mul(t));
	}

	@Override
	public void drawImageNoBegin(Renderer r, Image i, Color c) {
		i.bind();
		drawRaw(r, c);
	}

	@Override
	public Line clone() {
		return new Line(p1, p2);
	}

	@Override
	public Polygon toPolygon() {
		throw new UnsupportedOperationException("Cannot turn a line into a polygon.");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(p1, p2);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Line other = (Line) obj;
		return Objects.equals(p1, other.p1) && Objects.equals(p2, other.p2);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(p1);
		out.writeObject(p2);		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		Vector2f p1 = (Vector2f) in.readObject();
		Vector2f p2 = (Vector2f) in.readObject();
		this.init(p1, p2);
		this.center = Math2.getMidpoint(p1, p2);
	}

}
