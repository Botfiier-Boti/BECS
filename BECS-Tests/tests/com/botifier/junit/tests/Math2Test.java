package com.botifier.junit.tests;

import static com.botifier.junit.assertions.VectorAssertions.*;
import static com.botifier.junit.assertions.FloatAssertions.*;
import static com.botifier.junit.assertions.DoubleAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import com.botifier.becs.util.Math2;

public class Math2Test {
	public static final float EPSILON = 0.0001f;
	
	List<Vector2f> getTestVectors() {
		final List<Vector2f> testVectors = new ArrayList<Vector2f>();
		final float[] testValues = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1, 0, -1, 327, -327 };
		
		for (int y = 0; y < testValues.length; y++) {
			float vY = testValues[y];
			for (int x = 0; x < testValues.length; x++) {
				float vX = testValues[x];
				
				testVectors.add(new Vector2f(vX, vY));
			}
		}
		return testVectors;
	}
	
	@Test
	void testXDist() {
		
	}
}
