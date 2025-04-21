package com.botifier.becs.util;

import javax.annotation.Nonnull;

import org.joml.Math;
import org.joml.Vector2f;

import com.botifier.becs.util.shapes.*;

/**
 * Math functions that do not already exist in this form
 * 
 * TODO: More documentation
 * TODO: Add more math
 * TODO: Ensure math isn't wrong
 * 
 * @author Botifier
 *
 */
public class Math2 {

	public static final Vector2f NORTH = new Vector2f(0, 1),
								 SOUTH = new Vector2f(0, -1),
								 EAST = new Vector2f(1, 0),
								 WEST = new Vector2f(-1, 0);

	/**
	 * Should perform a faster modulus for power of 2?
	 * @param n Numerator
	 * @param d Divisor
	 * @return
	 */
	public static int quickModulus(int n, int d) {
		return n & (d - 1);
	}

	/**
	 * Returns the point between the supplied locations
	 * @param x1 float X
	 * @param y1 float Y
	 * @param x2 float X
	 * @param y2 float Y
	 * @return Vector2f The point between
	 */
	public static Vector2f getMidpoint(float x1, float y1, float x2, float y2) {
		return new Vector2f( (x1 + x2) / 2 , (y1 + y2) / 2 );
	}

	/**
	 * Returns the point between v and v2
	 * @param v Vector2f first
	 * @param v2 Vector2f second
	 * @return Vector2f the point between
	 */
	public static Vector2f getMidpoint(Vector2f v, Vector2f v2) {
		return getMidpoint(v.x, v.y, v2.x, v2.y);
	}

	/**
	 * Returns the distance on the x axis
	 * @param v Vector2f first
	 * @param v2 Vector2f second
	 * @return float X distance
	 */
	public static float xDist(Vector2f v, Vector2f v2) {
		Vector2f vC = new Vector2f(v);
		Vector2f v2C = new Vector2f(v2);

		v2C.set(v2C.x, vC.y);
		return vC.distance(v2C);
	}

	/**
	 * Returns the distance between two vectors on the y axis
	 * @param v Vector2f first
	 * @param v2 Vector2f second
	 * @return float Y distance
	 */
	public static float yDist(Vector2f v, Vector2f v2) {
		Vector2f vC = new Vector2f(v);
		Vector2f v2C = new Vector2f(v2);

		v2C.set(vC.x, v2C.y);
		return vC.distance(v2C);
	}

	/**
	 * Finds the slope
	 * @param x float X
	 * @param y float Y
	 * @param x2 float X
	 * @param y2 float Y
	 * @return float Slope
	 */
	public static float slope(float x, float y, float x2, float y2) {
		float maxX = Math.max(x, x2);
		float maxY = Math.max(y, y2);
		float minX = Math.min(x, x2);
		float minY = Math.min(y, y2);

		if (maxY-minY == 0) {
			return 0;
		}
		return (maxX-minX)/(maxY-minY);
	}

	/**
	 * Finds the y-intercept using a slope
	 * @param x float X
	 * @param y float Y
	 * @param slope float Slope
	 * @return float The y-intercept
	 */
	public static float yInter(float x, float y, float slope) {
		return y - slope * x;
	}

	/**
	 * Checks if two vectors are going the same direction
	 * @param one Vector2f first
	 * @param two Vector2f second
	 * @return boolean Whether or not they are going the same direction
	 */
	public static boolean sameDirection(Vector2f one, Vector2f two) {
		return one.dot(two) > 0;
	}

	/**
	 * Returns the closest cardinal direction to the supplied vector
	 * @param toClamp Vector2f To use
	 * @return Vector2f The closest cardinal direction
	 */
	public static Vector2f getDirection(Vector2f toClamp) {
		if (toClamp == null) {
			return null;
		}

		float distanceNorth = toClamp.distance(NORTH);
		float distanceEast = toClamp.distance(EAST);
		float distanceSouth = toClamp.distance(SOUTH);
		float distanceWest = toClamp.distance(WEST);

		float min = min(distanceNorth, distanceSouth, distanceEast, distanceWest);

		return min == distanceNorth ? new Vector2f(NORTH) : min == distanceSouth ? new Vector2f(SOUTH) :
			          min == distanceEast ? new Vector2f(EAST) : new Vector2f(WEST);
	}

	/**
	 * Rotates a point towards an angle
	 * @param point Vector2f To rotate
	 * @param angle float To use
	 * @return Vector2f Resulting point
	 */
	public static Vector2f rotatePoint(Vector2f point, float angle) {
		Vector2f newPoint = new Vector2f();

		float mx = (Math.cos(angle));
		float my = (Math.sin(angle));

		newPoint.x = (mx * point.x) - (my * point.y);
		newPoint.y = (my * point.x) + (mx * point.y);

		return newPoint;
	}

	/**
	 * Calculates the angle between two vectors
	 * @param src2 Vector2f first
	 * @param dst2 Vector2f second
	 * @return float The angle between
	 */
	public static float calcAngle(Vector2f src2,Vector2f dst2) {
		return Math.atan2(dst2.y-src2.y, dst2.x-src2.x);
	}

	public static float calcAngle(float x1, float y1, float x2, float y2) {
		return Math.atan2(y2-y1, x2-x1);
	}

	//TODO: find a way to do this with different outputs without copy/pasting
	public static float greatestNumber(float x, float y) {
		return x > y ? x : y > x ? y : x;
	}
	public static float lowestNumber(float x, float y) {
		return x > y ? y : y > x ? x : x;
	}

	public static boolean greaterThan(float x, float y) {
		return Math.max(x, y) == x ? true : false;
	}

	public static boolean lessThan(float x, float y) {
		return Math.min(x,y) == x ? true : false;
	}

	/**
	 * A linear interpolation function
	 * @param a float start
	 * @param b float end
	 * @param c float time
	 * @return float The interpolation
	 */
	public static float lerp(float a, float b, float c) {
		return a+c*(b-a);
	}

	/**
	 * Finds what's closer
	 * @param a float A
	 * @param b float B
	 * @param c float C
	 * @return float The closest of the three
	 */
	public static float getCloser(float a, float b, float c) {
		return (Math.abs(c-a) < Math.abs(c-b)) ? a : b;
	}

	public static Vector2f round(Vector2f v, int dec) {
		Vector2f n = new Vector2f();
		return n.set(round(v.x, dec), round(v.y, dec));
	}

	/**
	 * Rounds a float to x decimal places
	 * @param f float To round
	 * @param dec int Number of decimals
	 * @return float rounded number
	 */
	public static float round(float f, int dec) {
		float divisor =  (float) java.lang.Math.pow(10, dec);
		int value = (int) (f * divisor);

		return value / divisor;
	}
	/**
	 * Rounds a double to x decimal places
	 * @param f double To round
	 * @param dec int Number of decimals
	 * @return double rounded number
	 */
	public static double round(double f, int dec) {
		double divisor =  java.lang.Math.pow(10, dec);
		int value = (int) (f * divisor);

		return value / divisor;
	}

	/**
	 * AABB check
	 * @param x float
	 * @param y float
	 * @param width float
	 * @param height float
	 * @param px float To check
	 * @param py float To check
	 * @return boolean Whether or not px and py are inside the rectangle
	 */
	public static boolean rectContains(float x, float y, float width, float height, float px, float py) {
		float x1 = x+width;
		float y1 = y;
		float x2 = x;
		float y2 = y+height;
		return (px <= x1 &&  px >= x2 && py >= y1 && py <= y2) ? true : false;
	}

	/**
	 * lerp but on angles
	 * @param a float Start
	 * @param b float End
	 * @param weight float Time
	 * @return
	 */
	public static float lerpAngle(float a, float b, float weight) {
		return a + shortAngleDist(a,b) * weight;
	}

	/**
	 * The returns the shortest distance between two angles
	 * @param a float Angle1
	 * @param b float Angle2
	 * @return float The shortest distance
	 */
	public static float shortAngleDist(float a, float b) {
		float max = (float) (2*Math.PI);
		float dif = fmod(a-b, max);
		return fmod(2*dif, max)-dif;
	}

	/**
	 * Finds the 2d cross product
	 * @param x1 float X
	 * @param y1 float Y
	 * @param x2 float X
	 * @param y2 float Y
	 * @return float 2D cross product
	 */
	public static float crossProduct2D(float x1, float y1, float x2, float y2) {
		return x1 * y2 - x2 * y1;
	}

	/**
	 * Calculate the center of an array of points
	 * @param points Vector2f... To use
	 * @return Vector2f The center
	 */
	public static Vector2f calcCenteroid(Vector2f... points) {
		float cx = 0, cy = 0;
		float area = 0;

		for (int i = 0; i < points.length; i++) {
			Vector2f c = points[i];
			Vector2f n = points[(i + 1) % points.length];

			float cross = Math2.crossProduct2D(c.x, c.y, n.x, n.y);
			area += cross;
			cx += (c.x + n.x) * cross;
			cy += (c.y + n.y) * cross;
		}

		area *= 0.5f;
		cx /= (6 * area);
		cy /= (6 * area);

		return new Vector2f(cx, cy);
	}

	/**
	 * Calculates the center of an array of points using the original center
	 * @param origin Vector2f Original center as an offset
	 * @param points Vector2f... To use
	 * @return Vector2f The center
	 */
	public static Vector2f calcCentroidWithOrigin(Vector2f origin, Vector2f... points) {
		float cx = 0, cy = 0;
		float area = 0;
		Vector2f c = new Vector2f(origin);
		Vector2f n = new Vector2f(origin);
		
		for (int i = 0; i < points.length; i++) {
			c.add(points[i]);
			n.add(points[(i + 1) % points.length]);

			float cross = Math2.crossProduct2D(c.x, c.y, n.x, n.y);
			area += cross;
			cx += (c.x + n.x) * cross;
			cy += (c.y + n.y) * cross;
			
			//Reset to reduce instantiation of new vectors
			//Will cause minor inaccuracies due to the nature of floating points
			c.sub(points[i]);
			n.sub(points[(i + 1) % points.length]);
		}

		area *= 0.5f;
		cx /= (6 * area);
		cy /= (6 * area);

		return new Vector2f(cx, cy);
	}

	/**
	 * Floating point modulus
	 * @param a float A
	 * @param b float B
	 * @return float Modulus
	 */
	public static float fmod(float a, float b) {
		int result = (int) Math.floor(a/b);
		return a-result*b;
	}

	/**
	 * Checks if a string is an integer
	 * @param s String to Check
	 * @return boolean Whether or not the String can be parsed as an Integer
	 */
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if a string is a float
	 * @param s String to use
	 * @return boolean Whether or not the String can be parsed as a Float
	 */
	public static boolean isFloat(String s) {
		try {
			Float.parseFloat(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks if a string is a long
	 * @param s String To use
	 * @return boolean Whether or not the String can be parsed as a Long
	 */
	public static boolean isLong(String s) {
		try {
			Long.parseLong(s);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Max float value
	 * @param x float... Values
	 * @return float The max
	 */
	public static float max(float... x) {
		float max = x[0];
		for (float element : x) {
			if (element > max) {
				max = element;
			}
		}
		return max;
	}

	/**
	 * Min float value
	 * @param x float... Values
	 * @return float The min
	 */
	public static float min(float... x) {
		float min = x[0];
		for (float element : x) {
			if (element < min) {
				min = element;
			}
		}
		return min;
	}

	/**
	 * Calculates magnitude
	 * Redundant as Vector2f.length() exists
	 * @param toUse Vector2f To check
	 * @return float The magnitude
	 */
	@Deprecated
	public static float mag(Vector2f toUse) {
		return Math.sqrt(toUse.x*toUse.x + toUse.y*toUse.y);
	}

	/**
	 * Cap the magnitude of a vector
	 * @param toUse Vector2f To cap
	 * @param limit float Max magnitude
	 * @return Vector2f Modified vector
	 */
	public static Vector2f limit(Vector2f toUse, float limit) {
		float mag = toUse.length();

		if (mag != 0 && mag > limit) {
			toUse.mul(limit);
		}
		Vector2f newVector = new Vector2f(toUse);
		return newVector;
	}

	/**
	 * Max int value
	 * @param x int... Values
	 * @return int The max
	 */
	public static int max(int... x) {
		int max = x[0];
		for (int element : x) {
			if (element > max) {
				max = element;
			}
		}
		return max;
	}
	/**
	 * Min int value
	 * @param x int... Values
	 * @return int The min
	 */
	public static int min(int... x) {
		int min = x[0];
		for (int element : x) {
			if (element < min) {
				min = element;
			}
		}
		return min;
	}
	
	public static int compareVectorsByAngle(Vector2f v1, Vector2f v2, Vector2f center) {
		double angle1 = Math.atan2(v1.y - center.y, v1.x - center.x);
		double angle2 = Math.atan2(v2.y - center.y, v2.x - center.x);

		angle1 = round(angle1, 6);
		angle2 = round(angle2, 6);
		
		return angle1 == angle2 ?
			   Double.compare(v1.distance(center), v2.distance(center)) :
			   Double.compare(angle1, angle2);
	}
	
	/**
	 * Calculates the dimensions of a polygon given an array.
	 * Should work on lines, but if you just need the midpoint use getMidPoint()
	 * Works on a single point, though that isn't useful
	 * @param points Vector2f... Polygon to check
	 * @return
	 */
	public static Vector2f[] calcPolygonDimensions(@Nonnull Vector2f... points) {
		if (points.length == 0)
			return null;
		if (points.length == 1)
			return new Vector2f[] {new Vector2f(points[0]), new Vector2f(Float.MIN_EXPONENT)};
		
		float lx = points[0].x;
		float ly = points[0].y;

		float sx = points[0].x;
		float sy = points[0].y;

		float mx = 0;
		float my = 0;

		for (Vector2f point : points) {
			mx += point.x;
			my += point.y;
			lx = Math.max(lx, point.x);
			ly = Math.max(ly, point.y);
			sx = Math.min(sx, point.x);
			sy = Math.min(sy, point.y);
		}

		mx /= points.length;
		my /= points.length;

		if (!polyContains(mx, my, points)) {
			if (mx > lx) {
				mx -= (mx - lx) / 2 + lx;
			}
			if (mx < sx) {
				mx += (sx - mx) / 2 + sx;
			}

			if (my > ly) {
				my -= (my - ly) / 2 + ly;
			}
			if (my < sy) {
				my += (sy - my) / 2 + sy;
			}
		}
		
		return new Vector2f[] {new Vector2f(mx, my), new Vector2f(lx-sx, ly-sy)};
	}
	
	public static Polygon calcPolygonDimensionsAndUpdate(@Nonnull Polygon p) {
		Vector2f[] res = calcPolygonDimensions(p.getPoints());

		if (res != null) {
			p.setCenter(res[0].x, res[0].y);
			p.setWidth(res[1].x);
			p.setHeight(res[1].y);
			return p;
		}
		return null;
	}
	
	// based on http://www.jeffreythompson.org/collision-detection/poly-point.php
	public static boolean polyContains(float x, float y, @Nonnull Vector2f[] points) {
		boolean collides = false;

		for (int i = 0, next; i < points.length; i++) {

			next = (i + 1) % points.length;

			Vector2f c = points[i];
			Vector2f n = points[next];

			if ((c.y > y != n.y > y)
					&& (x < (n.x - c.x) * (y - c.y) / (n.y - c.y) + c.x)) {
				collides = !collides;
			} else if (Math.abs(y - c.y) < 0e6 && Math.abs(y - n.y) < 0e6) {
				if (x >= min(c.x, n.x) && x <= max(c.x, n.x)) {
					
				}
			}

		}

		return collides;
	}
}
