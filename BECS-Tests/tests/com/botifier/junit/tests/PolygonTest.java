package com.botifier.junit.tests;

import static org.junit.jupiter.api.Assertions.*;
import static com.botifier.junit.assertions.FloatAssertions.*;
import static com.botifier.junit.assertions.VectorAssertions.*;

import java.util.Arrays;

import org.joml.Vector2f;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.botifier.becs.util.shapes.*;
import com.botifier.becs.util.Math2;

@TestMethodOrder(OrderAnnotation.class)
class PolygonTest {
	public static final float EPSILON = 0.0001f;
	
	@Test
	void testSort() {
		Vector2f[] points = {new Vector2f(1,0), new Vector2f(-1, 0), new Vector2f(0, 1), new Vector2f(1, 1)};
		//Create polygon uses the same reference
		Polygon p = Polygon.createPolygon(Arrays.copyOf(points, points.length));
		
		assertVector2fArrayNotEquals(points, p.getPoints(), EPSILON);

		//Polygons are always sorted upon creation and the addition/removal of points
		Arrays.sort(points, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points)[0]));
		
		assertVector2fArrayEquals(points, p.getPoints(), EPSILON);
	}
	
	@Test
	void testAddPoint() {
		Vector2f[] points = {new Vector2f (0,1), new Vector2f(1, 0), new Vector2f(0,0)};
		//Polygons are always sorted upon creation and the addition/removal of points
		Arrays.sort(points, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points)[0]));
		//Create polygon uses the same reference
		Polygon p = Polygon.createPolygon(Arrays.copyOf(points, points.length));

		p.addPoint(new Vector2f(1, 1));
		
		assertVector2fArrayNotEquals(p.getPoints(), points, EPSILON);
		
		Vector2f[] points2 = Arrays.copyOf(points, 4);
		points2[3] = new Vector2f(1, 1);
		Arrays.sort(points2, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points2)[0]));
		
		
		assertVector2fArrayEquals(points2, p.getPoints(), EPSILON);
	}
	
	@Test
	void testRemovePoint() {
		Vector2f[] points = {new Vector2f(0,0), new Vector2f(1, 0), new Vector2f(0, 1), new Vector2f(1, 1)};
		//Polygons are always sorted upon creation and the addition/removal of points
		Arrays.sort(points, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points)[0]));
		//Create polygon uses the same reference
		Polygon p = Polygon.createPolygon(Arrays.copyOf(points, points.length));
		
		p.removePoint(2);
		
		assertVector2fArrayNotEquals(points, p.getPoints(), EPSILON);
		
		Vector2f[] points2 = new Vector2f[3];
		System.arraycopy(points, 0, points2, 0, 2);
		System.arraycopy(points, 2, points2, 1, points2.length-1);
		
		
		Arrays.sort(points2, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points2)[0]));
		
		assertVector2fArrayEquals(points2, p.getPoints(), EPSILON);
	}

	@Test
	void testMove() {
		Vector2f[] points = {new Vector2f(0,0), new Vector2f(1, 0), new Vector2f(0, 1)};
		//Polygons are always sorted upon creation and the addition/removal of points
		Arrays.sort(points, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcCenteroid(points)));
		//Create polygon uses the same reference
		Polygon p = Polygon.createPolygon(Arrays.copyOf(points, points.length));
		
		//Tests 8 directions 
		for (int y = -1; y < 1; y++) {
			for (int x = -1; x < 1; x++) {
				Vector2f test = new Vector2f(x, y);
				Polygon moved = p.move(test);
				
				Vector2f[] movedPoints = Arrays.copyOf(points, 3);
				movedPoints = Arrays.stream(movedPoints).map(v -> new Vector2f(v).add(test)).toArray(Vector2f[]::new);
				
				assertVector2fArrayEquals(moved.getPoints(), movedPoints, EPSILON);
			}
		}
	}

	@Test
	void testScaleFloat() {
		Vector2f[] points1 = {new Vector2f(-1, -1), new Vector2f(-1, 1), new Vector2f(1, 1), new Vector2f(1, -1)};
		Vector2f[] points2 = {new Vector2f(-2, -2), new Vector2f(-2, 2), new Vector2f(2, 2), new Vector2f(2, -2)};
		Arrays.sort(points2, (v1, v2) -> Math2.compareVectorsByAngle(v1, v2, Math2.calcPolygonDimensions(points2)[0]));
		
		Polygon p1 = Polygon.createPolygon(points1);
		p1 = p1.scale(2);
		
		assertVector2fArrayEquals(points2, p1.getPoints(), EPSILON);
		assertEquals(p1.getCenter(), Math2.calcCenteroid(points2));
	}
	
	

}
