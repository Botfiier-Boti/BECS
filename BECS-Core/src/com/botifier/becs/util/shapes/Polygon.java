package com.botifier.becs.util.shapes;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.joml.Intersectionf;
import org.joml.Matrix2f;
import org.joml.Matrix2fc;
import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.CollisionUtil;
import com.botifier.becs.util.CollisionUtil.PolygonOutput;
import com.botifier.becs.util.Math2;

public class Polygon extends Shape {

	// TriangulatedPolygon triangl;

	private List<Line> edges;
	private boolean updated = false;
	private Vector2f[] points;

	float width = 0;
	float height = 0;

	private Polygon(Vector2f... points) {
		super(0, 0);
		if (points.length == 0) {
			throw new IllegalArgumentException("Point length must be greater than zero.");
		}
		this.points = points;
		calcCenter();
		sort();
	}
	
	public Polygon(ObjectInput in) throws ClassNotFoundException, IOException {
		super(in);
	}
	

	@Override
	public boolean contains(Vector2f v) {
		return contains(v.x, v.y);
	}

	@Override
	public boolean contains(float x, float y) {
		return Math2.polyContains(x, y, points);
	}

	@Override
	public void draw(Renderer r, Color c) {
		r.begin();
		r.drawPolygon(this, 0, c);
		r.end();
	}

	@Override
	public void drawFilled(Renderer r, Color c) {
		r.begin();
		r.fillPolygon(this, 0, c);
		r.end();
	}

	@Override
	public void drawImage(Renderer r, Image i, Color c, boolean flipped) {
		i.bind();
		r.begin();
		r.fillPolygon(this, 0, c);
		r.end();
	}

	@Override
	public void drawRaw(Renderer r, Color c) {
		r.drawPolygon(this, 0, c);
	}

	@Override
	public void drawImageNoBegin(Renderer r, Image i, Color c) {
		i.bind();
		r.fillPolygon(this, i.getZ(), c);
	}

	@Override
	public Vector2f closestTo(Shape s) {
		throw new UnsupportedOperationException("Polygons must use findClosest for shape detection.");
	}

	@Override
	public Vector2f closestTo(Vector2f v) {
		return Arrays.stream(points)
				.min(Comparator.comparingDouble(p -> p.distance(v)))
				.orElse(points[0]);
	}

	@Override
	public boolean intersects(Shape s) {
		return intersects(s.toPolygon());
	}

	public boolean intersects(Vector2f v) {
		return intersects(v.x, v.y);
	}

	public boolean intersects(float x, float y) {
		return contains(x, y) || getEdges().stream().anyMatch(l -> l.contains(x, y));
	}

	/**
	 * Checks whether or not two polygons intersect using their edges
	 * Performs a contains check if not
	 * @param p Polygon to check
	 * @return boolean whether an intersection occured or not
	 */
	public boolean intersects(Polygon p) {
		return  Intersectionf.testPolygonPolygon(p.points, points);

	}

	private boolean checkProjection(Vector2f axis, Polygon p, Polygon p2, PolygonOutput out) {
		Vector2f outputA = CollisionUtil.projectPolygon(p, axis);
		Vector2f outputB = CollisionUtil.projectPolygon(p2, axis);

		final float epsilon = 1e-6f;
		if (outputA.x >= outputB.y + epsilon || outputB.x >= outputA.y + epsilon) {
			return false;
		}


		float axisDepth = Math.min(outputB.y - outputA.x, outputA.y - outputB.x);
		if (axisDepth < out.getDepth()) {
			out.setDepth(axisDepth);
			out.setNormal(axis);
		}
		return true;
	}

	private boolean checkFullContainment(Vector2f axis, Polygon p1, Polygon p2) {
	    Vector2f projection1 = CollisionUtil.projectPolygon(p1, axis); // Min and max for p1
	    Vector2f projection2 = CollisionUtil.projectPolygon(p2, axis); // Min and max for p2

	    // Check if p1 is fully contained within p2
	    boolean p1Contained = (projection1.x >= projection2.x && projection1.y <= projection2.y);

	    // Check if p2 is fully contained within p1
	    boolean p2Contained = (projection2.x >= projection1.x && projection2.y <= projection1.y);

	    // Return true if either polygon's projection is fully contained in the other
	    return p1Contained || p2Contained;
	}

	/**
	 * Checks for intersection using the Separating Axis Theorem
	 * @param p Polygon To check
	 * @return PolygonOutput the SAT result
	 */
	public PolygonOutput intersectsSAT(Polygon p) {
		PolygonOutput out = new PolygonOutput();

		boolean fullyOverlapping = true;

		//First Polygon
		for (int i = 0; i < getPoints().length + p.getPoints().length; i++) {
			Vector2f p1;
			Vector2f p2;

			if (i < getPoints().length) {
				p1 = getPoint(i);
				p2 = getPoint((i+1) % getPoints().length);
			} else {
				int j = i - getPoints().length;
				p1 = p.getPoint(j);
				p2 = p.getPoint((j+1) % p.getPoints().length);
			}


			Vector2f edge = new Vector2f(p2).sub(p1);
			Vector2f axis = new Vector2f(-edge.y, edge.x).normalize();


			if (!checkProjection(axis, this, p, out)) {
				return null;
			}

			fullyOverlapping &= checkFullContainment(axis, this, p);

		}

		float depth = out.getDepth();
		Vector2f normal = out.getNormal();

		if (fullyOverlapping) {
			Vector2f dir = new Vector2f(p.getCenter().sub(getCenter()));
			if (dir.dot(normal) < 0) {
				normal.negate();
			}

			out.setDepth(depth);
			out.setNormal(normal.normalize());
			return out;
		}

		if (depth < Float.MAX_VALUE) {
			float nLen = normal.length();
			if (nLen != 0) {
				Vector2f dir = new Vector2f(p.getCenter()).sub(getCenter());

				if (dir.dot(normal) < 0) {
					normal.negate();
				}

				if (depth <= 0) {
					return null;
				}

				out.setDepth(depth);
				out.setNormal(normal);
				return out;
			}
		}
		return null;
	}


	/**
	 * Adds a point the polygon
	 * @param v Vector2f point to add
	 */
	public void addPoint(Vector2f v) {
		points = Arrays.copyOf(points, points.length+1);
		points[points.length-1] = v;
		calcCenter();
		sort();
		updated = true;
	}

	/**
	 * Removes a point at specified position in the array
	 * @param pos int To remove
	 */
	public void removePoint(int pos) {
		Vector2f[] temp = new Vector2f[points.length-1];
		System.arraycopy(points, 0, temp, 0, pos);
		System.arraycopy(points, pos, temp, pos-1, temp.length-1);
		points = temp;
		calcCenter();
		sort();
		updated = true;
	}

	public Polygon startAtIntersection(Polygon p, Vector2f vel) {
		//Vector2f start = new Vector2f(getCenter());
		Polygon movement = move(vel);
		Polygon clip = p.clip(this);

		return clip == null ? null : movement.mergeNoRepeat(clip);
	}

	/**
	 * Creates a clipping using the specified polygon
	 * @param p Polygon to use
	 * @return Polygon the result
	 */
	public Polygon clip(Polygon p) {
		if (p.equals(this)) {
			return p;
		}
		List<Line> edges = p.getEdges();
		Set<Vector2f> points = new HashSet<>();
		for (Line l : edges) {
			points.addAll(l.getPointsOfIntersection(this));
			if (p.contains(l.getPoint1()) && contains(l.getPoint1())) {
				points.add(new Vector2f(l.getPoint1()));
			}
			if (p.contains(l.getPoint2()) && contains(l.getPoint2())) {
				points.add(new Vector2f(l.getPoint2()));
			}
		}

		return points.isEmpty() ? null : new Polygon(points.toArray(Vector2f[]::new));
	}

	/**
	 * Converts the polygon into a list of lines representing its edges
	 * @return List<Line> The polygon as lines
	 */
	public List<Line> getEdges() {
		if (edges == null || edges.isEmpty() || updated) {
			edges = new ArrayList<>();
			for (int i = 0; i < points.length; i++) {
				Vector2f start = points[i];
				Vector2f end = points[(i+1) % points.length];
				edges.add(new Line(start.x, start.y, end.x, end.y));
			}
			updated = false;
		}
		return edges;
	}

	@Override
	public Vector2f getDimensions() {
		return new Vector2f(width, height);
	}

	/**
	 * Creates a RotatableRectangle as a bounding box for the Polygon
	 * @return RotatableRectangle the bounding box
	 */
	public RotatableRectangle getBoundingBox() {
		float max = Math.max(width, height);
		return new RotatableRectangle(getCenter(), max, max);
	}

	/**
	 * Returns all of the points within the polygon
	 * @return
	 */
	public Vector2f[] getPoints() {
		return points;
	}

	/**
	 * Returns the point from the location specified in the array
	 * @param point int position in the array
	 * @return Vector2f point at the position
	 */
	public Vector2f getPoint(int point) {
		return points[point];
	}

	/**
	 * Creates a copy of the Polygon in a new location
	 * @param v Vector2f amount to move
	 * @return Polygon the polygon in a new location
	 */
	public Polygon move(Vector2f v) {
		Vector2f[] newPoints = Arrays.stream(points)
									 .map(point -> new Vector2f(point).add(v))
									 .toArray(Vector2f[]::new);
		return new Polygon(newPoints);
	}

	/**
	 * Merges two polygons and outputs the result. Does not remove any points.
	 * @param p Polygon to merge
	 * @return Polygon the result
	 */
	public Polygon merge(Polygon p) {
		Vector2f[] hold = Arrays.copyOf(points, points.length + p.points.length);
		System.arraycopy(p.points, 0, hold, points.length, p.points.length);
		return createPolygon(hold);
	}

	/**
	 *
	 * @param p
	 * @return
	 */
	public Polygon mergeNoRepeat(Polygon p) {
		Polygon hold = union(p);
		hold.simplify();
		return hold;
	}

	private int orien(Vector2f p, Vector2f q, Vector2f r) {
		float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		return val > 0 ? 1 : val < 0 ? 2 : 0;
	}

	public Polygon convexHull(Polygon... p) {

		List<Vector2f> unique = new ArrayList<>(Arrays.asList(points));
		for (Polygon pol : p) {
			unique.addAll(Arrays.asList(pol.points));
		}

		Vector2f[] points = unique.toArray(Vector2f[]::new);
		List<Vector2f> hull = new ArrayList<>();

		int l = 0;
		for (int i = 1; i < points.length; i++) {
			if (points[i].x < points[l].x) {
				l = i;
			}
		}

		int po = l, q;
		do {
			hull.add(points[po]);

			if (hull.size() >= unique.size()) {
				break;
			}

			q = (po + 1) % points.length;

			for (int i = 0; i < points.length; i++) {
				if (orien(points[po], points[i], points[q]) == 2) {
					q = i;
				}
			}


			po = q;
		} while (po != l);
		return Polygon.createPolygon(hull.toArray(Vector2f[]::new));
	}

	public Polygon scale(Matrix2fc mat2) {
		Vector2f[] p = Arrays.stream(points).map(k -> {
				Vector2f res = new Vector2f(k).sub(getCenter());
				res.mul(mat2);
				res.add(getCenter());
				return res;
			}).toArray(Vector2f[]::new);
		return Polygon.createPolygon(p);
	}

	public Polygon scale(float val) {
		Matrix2fc mat2 = new Matrix2f(val, 0,
								      0, val);
		return scale(mat2);
	}

	/**
	 * Fuses two Polygons together Does not override current polygon
	 *
	 *
	 * @param p The polygon to fuse with
	 * @return The resulting polygon
	 */
	public Polygon union(Polygon p) {
		if (p.equals(this)) {
			return this;
		}
		/*// Adds all of the points of the combined polygons together
		Set<Vector2f> unique = new HashSet<>(Arrays.asList(points));
		unique.addAll(Arrays.asList(p.points));

		// Gets the normalized vectors towards each polygon relative to each polygon
		Vector2f dir = new Vector2f(p.getCenter()).sub(getCenter());
		Vector2f dir2 = new Vector2f(getCenter()).sub(p.getCenter());

		// Creates test polygons by creating clones that are slightly closer to each
		// other
		Polygon test = move(dir.mul(0.5f));
		Polygon test2 = p.move(dir2.mul(0.5f));

		// Removes values that exist between both of the polygons
		unique = unique.stream().filter(v -> !test.contains(v) || !test2.contains(v))
								.collect(Collectors.toSet());

		if (unique.size() < points.length || unique.size() < 3) {
			System.out.println("Somehow list is too small");
			return this;
		}

		Polygon hold = createPolygon(unique.toArray(Vector2f[]::new));
		if(hold.points.length < 3) {
			System.out.println("Somehow array is too small");
			return this;
		}*/
		Polygon hold = convexHull(p);
		return hold;
	}

	/**
	 * Removes points near the center based on the leniency
	 * @param leniency float minimum distance to remain
	 */
	public void removeCenterPoint(float leniency) {
		points = Arrays.stream(points)
					   .filter(v -> Math.abs(v.x - center.x) > leniency || Math.abs(v.y - center.y) > leniency)
					   .toArray(Vector2f[]::new);
		updated = true;
	}

	/**
	 * Calculates the center of the polygon and stores it
	 */
	public void calcCenter() {
		Math2.calcPolygonDimensionsAndUpdate(this);
	}

	/**
	 * Sorts all of the points within the polygon based on their angles between each other.
	 */
	public void sort() {
		Arrays.sort(points, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, getCenter()));
	}

	/**
	 * Simplifies a polygon by removing points that do not change topology
	 */
	public void simplify() {
		Set<Vector2f> hold = new HashSet<>();
		double lastAngle = -99;
		for (int i = 0; i < points.length; i++) {
			Vector2f p1 = points[i];
			Vector2f p2 = points[(i + 1) % points.length];

			double angle = Math2.round(Math.atan2(p1.y - p2.y, p1.x - p2.x), 2);

			if (lastAngle != -99 && angle == lastAngle) {
				continue;
			}
			lastAngle = angle;
			hold.add(p1);
		}
		//Final check through the first points again
		Vector2f p1 = points[0];
		Vector2f p2 = points[1];
		double angle = Math2.round(Math.atan2(p1.y - p2.y, p1.x - p2.x), 1);
		if (lastAngle == angle) {
			hold.remove(p1);
		}

		updated = true;
		points = hold.toArray(Vector2f[]::new);
		sort();
	}

	public Entry<Vector2f, Float> findClosestPoint(Polygon target) {
		if ((target == null) || (target.getPoints().length == 0)) {
			return null;
		}
		if (target.getPoints().length == 1) {
			return new SimpleEntry<>(target.getPoint(0), getCenter().distance(target.getPoint(0)));
		}

		return Arrays.stream(target.getPoints())
					 .map(point -> new SimpleEntry<>(point, getCenter().distance(point)))
					 .min(Comparator.comparingDouble(Entry::getValue))
					 .orElse(new SimpleEntry<>(target.getPoint(0), getCenter().distance(target.getPoint(0))));
	}

	/**
	 * Searches for the closest point(s) to the specified Polygon
	 * Based on https://www.geeksforgeeks.org/closest-pair-of-points-using-divide-and-conquer-algorithm/
	 * @param p Polygon to use
	 * @return The point(s) closest to the specified polygon
	 */
	public Vector2f[] findClosest(Polygon p) {
		if (p.intersects(this)) {
			return clip(p).points;
		}

		float min = Float.MAX_VALUE;
		Vector2f closest = points[0];
		Set<Vector2f> hold = new HashSet<>();

		Vector2f[] tmp = merge(p).points;
		Arrays.sort(tmp, Comparator.comparingDouble(o -> o.y));

		for (int i = 0; i < points.length; i++) {
			for (int j = i+1; j < p.getPoints().length && (tmp[j].y - tmp[i].y) < min; ++j) {
				float dist = tmp[i].distance(tmp[j]);
				if (dist < min) {
					min = dist;
					closest = (contains(tmp[i]) ? tmp[i] : tmp[j]);
					hold.clear();
				} else if (dist == min) {
					hold.add((contains(tmp[i]) ? tmp[i] : tmp[j]));
				}
			}
		}
		hold.add(closest);
		return hold.toArray(new Vector2f[0]);
	}

	/**
	 * Finds a point on the edge using an angle
	 * @param angle float To check
	 * @return Vector2f The point
	 */
	public Vector2f getEdgePoint(float angle) {
		return getEdgePoint(angle, getCenter());
	}

	public Vector2f getEdgePoint(float angle, Vector2f origin) {
		Vector2f hold = new Vector2f();
		int intersects = Intersectionf.intersectPolygonRay(points, origin.x, origin.y, (float) Math.cos(angle), (float) Math.sin(angle), hold);

		return intersects > 0 ? hold : origin;
	}

	/**
	 * Returns the value stored within the width variable
	 * @return float width
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * Returns the value stored within the height variable
	 * @return float height
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * Sets the width variable of the polygon to the specified number
	 * @param height float to use
	 */
	public void setWidth(float width) {
		this.width = width;
	}

	/**
	 * Sets the height variable of the polygon to the specified number
	 * @param height float to use
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/*
	 * public TriangulatedPolygon triangulate() { return new
	 * TriangulatedPolygon(center, points); }
	 */

	/**
	 * Creates a polygon
	 * Must have at least 3 points
	 * @param points Vector2f... any number of points
	 * @return Polygon the result
	 */
	public static Polygon createPolygon(Vector2f... points) {
		if (points.length < 3) {
			System.out.println("ERROR: Polygon must have at least 3 points. given: " + points);
			return null;
		}

		return new Polygon(points);
	}

	@Override
	public Polygon toPolygon() {
		return this;
	}

	/**
	 * Compares two polygons
	 * @param p Polygon other
	 * @return boolean Whether the two polygons are the same
	 */
	public boolean equals(Polygon p) {
		return Arrays.equals(points, p.points);
	}


	@Override
	public Polygon clone() {
		return new Polygon(this.points);
	}

	public float distance(Polygon p) {
		float minDist = Float.MAX_VALUE;

		for (Vector2f v1 : points) {
			for (Vector2f v2 : p.points) {
				float dist = v1.distance(v2);

				if (dist < minDist) {
					minDist = dist;
				}
			}
		}

		return minDist;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(points);
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
		Polygon other = (Polygon) obj;
		return Arrays.equals(points, other.points);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(points);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int len = in.readInt();
		
		Vector2f[] points = (Vector2f[]) in.readObject();
		
		this.points = points;
		calcCenter();
		sort();
	}

}
