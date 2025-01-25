package com.botifier.junit.assertions;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Math;


public class FloatAssertions {
	public static void assertFloatEquals(float a, float b, float epsilon, String... message) {
		assertTrue(Math.abs(b - a) < epsilon, message.length == 0 ? String.format("Float %f is not within epsilon range of %f", a, b) : String.join("\n", message));
	}
	
	public static void assertFloatNotEquals(float a, float b, float epsilon, String... message) {
		assertFalse(Math.abs(b - a) < epsilon, message.length == 0 ? String.format("Float %f is within epsilon range of %f", a, b) : String.join("\n", message));
	}
	
	
	public static void assertFloatArrayEquals(float[] a, float[] b, float epsilon, String... message) {
		
		try {
			for (int i = 0; i < a.length; i++) {
				assertFloatEquals(a[i], b[i], epsilon, message);
			}
		} catch (Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
		
	}
	
	public static void assertFloatArrayNotEquals(float[] a, float[] b, float epsilon, String... message) {
		if (a == null || b == null)
			return;
		if (a.length != b.length)
			return;
		
		try {
			for (int i = 0; i < a.length; i++) {
				assertFloatNotEquals(a[i], b[i], epsilon, message);
			}
		} catch (Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
	}
}
