package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.joml.Math;
import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.util.Math2;

/**
 * RotatableRectangle
 * 
 * TODO: Document this
 * TODO: Optimize this
 * 
 * @author Botifier
 */
public class RotatableRectangle extends Shape {
	private float width, height;

	private Vector2f tr, tl, br, bl;
	private Vector2f dim = new Vector2f();

	public RotatableRectangle(float cx, float cy, float width, float height) {
		this(new Vector2f(cx, cy), width, height, 0);
	}

	public RotatableRectangle(float cx, float cy, float width, float height, float rotation) {
		this(new Vector2f(cx, cy), width, height, rotation);
	}

	public RotatableRectangle(Vector2f center, float width, float height) {
		this(center, width, height, 0);
	}

	public RotatableRectangle(Vector2f center, float width, float height, float rotation) {
		super(center);
		init(width, height, rotation);
	}

	public RotatableRectangle(RotatableRectangle r) {
		this(new Vector2f(r.getCenter()), r.getWidth(), r.getHeight(), r.getRotation());
	}
	
	public RotatableRectangle(ObjectInput in) throws ClassNotFoundException, IOException {
		super(in);
	}

	private void init(float width, float height, float rotation) {
		this.width = width/2;
		this.height = height/2;
		this.setAngle(rotation);
		calcRectangle();
	}

	@Override
	public void setCenter(float x, float y) {
		super.setCenter(x, y);
		calcRectangle();
	}

	public void setCenter(Vector2f center) {
		setCenter(center.x, center.y);
	}

	public void setWidth(float width) {
		this.width = width/2;
		calcRectangle();
	}

	public void setHeight(float height) {
		this.height = height/2;
		calcRectangle();
	}

	public void calcRectangle() {
		tr = Math2.rotatePoint(new Vector2f(width, height), angle);
		tl = Math2.rotatePoint(new Vector2f(-width, height), angle);
		br = Math2.rotatePoint(new Vector2f(width, -height), angle);
		bl = Math2.rotatePoint(new Vector2f(-width, -height), angle);
		dim.set(width, height);
	}

	@Override
	public void drawRaw(Renderer r, Color c) {
		r.drawLine(getTopRight().x, getTopRight().y, getTopLeft().x, getTopLeft().y, c, 2);
		r.drawLine(getTopRight().x, getTopRight().y, getBottomRight().x, getBottomRight().y, c, 2);
		r.drawLine(getBottomRight().x, getBottomRight().y, getBottomLeft().x, getBottomLeft().y, c, 2);
		r.drawLine(getTopLeft().x, getTopLeft().y, getBottomLeft().x, getBottomLeft().y, c, 2);
	}

	@Override
	public void drawImage(Renderer r, Image i) {
		drawImage(r,i,Color.white,false,true);
	}

	@Override
	public void drawImage(Renderer r, Image i, Color c) {
		drawImage(r,i,c,false,true);
	}

	@Override
	public void drawImage(Renderer r, Image i, Color c, boolean flipped) {
		drawImage(r,i,c,flipped,true);
	}

	private void drawImage(Renderer r, Image i, Color c, boolean flipped, boolean beginEnd) {
		i.bind();
		float z = i.getZ();
		if (beginEnd) {
			r.begin(i.getShaderProgram());
		}
			r.drawRotatedRectangle(this, z, c, flipped);
		if (beginEnd) {
			r.end();
		}
	}

	@Override
	public void drawImageNoBegin(Renderer r, Image i, Color c) {
		drawImage(r,i,c,false,false);

	}
	
	public void drawSubImage(Renderer r, Image i, Color c, float tx, float ty, float tWidth, float tHeight) {
		i.bind();
		Texture t = i.getTexture();
		float s1 = tx / t.getWidth();
		float t1 = ty / t.getHeight();
		float s2 = (tx + tWidth) / t.getWidth();
		float t2 = (ty + tHeight) / t.getHeight();
		
		float[] texCoords = {
				s1, t1, // Tri1 Bottom-Left
				s1, t2, // Tri1 Top-Left
				s2, t2, // Tri1 Top-Right
				s1, t1, // Tri2 Bottom-Left
				s2, t2, // Tri2 Top-Right
				s2, t1, // tri2 Bottom-Right
		};
		
		r.drawRotatedRectangleWithTexCoords(this, texCoords, i.getZ(), c, false);
	}

	public void drawFilled(Renderer r) {
		drawFilled(r, Color.white);
	}

	@Override
	public void drawFilled(Renderer r, Color c) {
		drawImage(r,Image.WHITE_PIXEL, c, false, true);
	}

	@Override
	public void draw(Renderer r, Color c) {
		Image.WHITE_PIXEL.bind();
		r.begin();
			drawRaw(r, c);
		r.end();
	}

	// based on https://gist.github.com/jackmott/021bb1bd1135df71c389b42b8b44cc30
	private int createScalar(Vector2f corner, Vector2f axis) {
		float aNum = (corner.x * axis.x) + (corner.y * axis.y);
		float aDen = (axis.x * axis.x) + (axis.y * axis.y);
		float div = aNum / aDen;

		Vector2f projCorn = new Vector2f(div * axis.x, div*axis.y);
		return (int) projCorn.dot(axis);
	}
	// also based on https://gist.github.com/jackmott/021bb1bd1135df71c389b42b8b44cc30
	private boolean isAxisCollision(RotatableRectangle rr, Vector2f axis) {
		int[] tScalar = {
			createScalar(rr.getTopLeft(), axis),
			createScalar(rr.getTopRight(), axis),
			createScalar(rr.getBottomLeft(), axis),
			createScalar(rr.getBottomRight(), axis)
		};

		int[] oScalar = {
			createScalar(getTopLeft(), axis),
			createScalar(getTopRight(), axis),
			createScalar(getBottomLeft(), axis),
			createScalar(getBottomRight(), axis)
		};

		int aMin = Math2.min(tScalar);
		int aMax = Math2.max(tScalar);
		int bMin = Math2.min(oScalar);
		int bMax = Math2.max(oScalar);


		return (bMin <= aMax && bMax >= aMax) || (aMin <= bMax && aMax >= bMax);
	}

	@Override
	public void rotate(float rads) {
		super.rotate(rads);
		calcRectangle();
	}

	@Override
	public void setAngle(float rotation) {
		super.setAngle(rotation);
		calcRectangle();
	}

	public float getWidth() {
		return width * 2;
	}

	public float getHeight() {
		return height * 2;
	}

	public Vector2f getTrueTopRight() {
		return getTrueCorner(true, true);
	}

	public Vector2f getTrueTopLeft() {
		return getTrueCorner(false, true);
	}

	public Vector2f getTrueBottomRight() {
		return getTrueCorner(true, false);
	}

	public Vector2f getTrueBottomLeft() {
		return getTrueCorner(false, false);
	}

	private Vector2f getTrueCorner(boolean right, boolean top) {
		float x = right ? Math2.max(tr.x, tl.x, br.x, bl.x) : Math2.min(tr.x, tl.x, br.x, bl.x);
		float y = top ? Math2.max(tr.y, tl.y, br.y, bl.y) : Math2.min(tr.y, tl.y, br.y, bl.y);

		return new Vector2f(center.x + x,  center.y + y);
	}

	public Vector2f getTopRight() {
		return new Vector2f(center.x + tr.x, center.y + tr.y);
	}

	public Vector2f getTopLeft() {
		return new Vector2f(center.x + tl.x, center.y + tl.y);
	}

	public Vector2f getBottomRight() {
		return new Vector2f(center.x + br.x, center.y + br.y);
	}

	public Vector2f getBottomLeft() {
		return new Vector2f(center.x + bl.x, center.y + bl.y);
	}

	@Override
	public Vector2f getCenter() {
		return new Vector2f(center);
	}

	public float getMinX() {
		return getTrueTopLeft().x;
	}

	public float getMaxX() {
		return getTrueTopRight().x;
	}

	public float getMinY() {
		return getTrueBottomLeft().y;
	}

	public float getMaxY() {
		return getTrueTopLeft().y;
	}

	//https://stackoverflow.com/questions/4061576/finding-points-on-a-rectangle-at-a-given-angle
	private Vector2f closestToVec(Vector2f v) {

		/*Vector2f l = new Vector2f(getTrueTopLeft().x, center.y);
		Vector2f r = new Vector2f(getTrueTopRight().x, center.y);
		Vector2f t = new Vector2f(center.x, getTrueTopLeft().y);
		Vector2f b = new Vector2f(center.x, getTrueBottomLeft().y);

		float lD = v.distance(l);
		float rD = v.distance(r);
		float bD = v.distance(t);
		float tD = v.distance(b);

		float min = Math2.min(lD, rD, bD, tD);*/
		Vector2f vec = new Vector2f();
		if (angle % 90 == 0) {
			Vector2f v2 = new Vector2f(v);
			v2.sub(center);

			vec.x = Math.max(tl.x, Math.min(v2.x, br.x));
			vec.y = Math.max(bl.y, Math.min(v2.y, tr.y));

			vec.add(center);
		} else {

			float angle = Math2.calcAngle(center, v) - getRotation();
			float width = this.width;
			float height = this.height;
			float rAtan = Math.atan2(height, width);
			int reg = 0;

			if (angle > -rAtan &&  angle <= rAtan) {
				reg = 1;
			} else if (angle > rAtan && angle% Math.PI <= (Math.PI - rAtan) ) {
				reg = 2;
			} else if (angle > Math.PI - rAtan || angle <= -(Math.PI - rAtan)) {
				reg = 3;
			} else {
				reg = 4;
			}


			vec = Math2.rotatePoint(calcRegionVec(angle, reg, vec), getRotation());//getRotation());
			vec.add(center);

		}
		return vec;

	}

	public Vector2f calcRegionVec(float angle, int reg, Vector2f vec) {
		switch (reg) {
			case 1:
				vec.x += height;
				vec.y += height * Math.tan(angle);
				break;
			case 2:
				vec.x += width / Math.tan(angle);
				vec.y += width;
				break;
			case 3:
				vec.x -= height;
				vec.y -= height * Math.tan(angle);
				break;
			case 4:
				vec.x -= width / Math.tan(angle);
				vec.y -= width;
				break;
		}
		return vec;
	}


	public Vector2f closestTo(RotatableRectangle rr) {
		Vector2f[] points = {
			closestToVec(rr.getBottomLeft()),
			closestToVec(rr.getBottomRight()),
			closestToVec(rr.getTopLeft()),
			closestToVec(rr.getTopRight()),
			closestToVec(rr.getCenter()),
			closestToVec(rr.closestToVec(getCenter()))
		};

		float[] distances = {
				points[0].distance(rr.getBottomLeft()),
				points[1].distance(rr.getBottomRight()),
				points[2].distance(rr.getTopLeft()),
				points[3].distance(rr.getTopRight()),
				points[4].distance(rr.getCenter()),
				points[5].distance(rr.closestToVec(getCenter()))
		};

		int minI = 0;
		for (int i = 1; i < distances.length; i++) {
			if (distances[i] < distances[minI]) {
				minI = i;
			}
		}

		return points[minI];
	}

	@Override
	public Vector2f closestTo(Vector2f v) {
		return closestTo(new RotatableRectangle(v, 1, 1));
	}

	@Override
	public Vector2f closestTo(Shape s) {
		return closestTo(new RotatableRectangle(new Vector2f(s.getCenter()), s.getDimensions().x, s.getDimensions().y));
	}

	public float distance(RotatableRectangle rr) {
		return distance(rr.getCenter());
	}

	@Override
	public Polygon toPolygon() {
		//calcRectangle();
		return Polygon.createPolygon(getTopRight(), getTopLeft(), getBottomLeft(), getBottomRight());
	}

	public float distance(Vector2f pos) {
		return getCenter().distance(pos);
	}

	//https://gist.github.com/jackmott/021bb1bd1135df71c389b42b8b44cc30
	public boolean intersects(RotatableRectangle rr) {
		Vector2f[] ax = {
			new Vector2f(getTopRight()).sub(getTopLeft()),
			new Vector2f(getTopRight()).sub(getBottomRight()),
			new Vector2f(rr.getTopLeft()).sub(rr.getBottomLeft()),
			new Vector2f(rr.getTopLeft()).sub(rr.getTopRight())
		};

		for (Vector2f v : ax) {
			if (!isAxisCollision(rr, v)) {
				return false;
			}
		}
		return true;
	}

	public boolean intersects(RotatableRectangle rr, Vector2f move) {
		RotatableRectangle temp = new RotatableRectangle(this);

		float mX = move.x;
		float mY = move.y;

		for (float y = 0; y < Math.abs(mY); y++) {
			temp.setCenter(temp.getCenter().x, temp.getCenter().y + Math.signum(mY));
			if (temp.intersects(rr)) {
				return true;
			}
		}
		for (float y = 0; y < Math.abs(mY); y++) {
			temp.setCenter(temp.getCenter().x + Math.signum(mX), temp.getCenter().y);
			if (temp.intersects(rr)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean intersects(Shape s) {
		if (s instanceof RotatableRectangle) {
			return intersects((RotatableRectangle)s);
		}
		return s.intersects(this);
	}

	@Override
	public boolean contains(Vector2f v) {
		return contains(v.x, v.y);
	}

	@Override
	public boolean contains(float x, float y) {
		return intersects(new RotatableRectangle(x, y, 0f, 0f));
	}

	@Override
	public Vector2f getDimensions() {
		return new Vector2f(getWidth(), getHeight());
	}

	@Override
	public RotatableRectangle clone() {
		return new RotatableRectangle(this);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeFloat(width * 2);
		out.writeFloat(height * 2);
		out.writeFloat(angle);
		out.writeObject(getCenter());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		float width = in.readFloat();
		float height = in.readFloat();
		float angle = in.readFloat();
		Vector2f center = (Vector2f) in.readObject();
		
		this.center = center;
		init(width, height, angle);
		
	}
}
