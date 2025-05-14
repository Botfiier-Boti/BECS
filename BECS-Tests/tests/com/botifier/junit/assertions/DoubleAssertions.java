package com.botifier.junit.assertions;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Math;


public class DoubleAssertions {
	public static void assertDoubleEquals(double a, double b, double epsilon, String... message) {
		assertTrue(Math.abs(b - a) < epsilon, message.length == 0 ? String.format("Double %f is not within epsilon range of %f", a, b) : String.join("\n", message));
	}
	
	public static void assertDoubleNotEquals(double a, double b, double epsilon, String... message) {
		assertFalse(Math.abs(b - a) < epsilon, message.length == 0 ? String.format("Double %f is within epsilon range of %f", a, b) : String.join("\n", message));
	}
	
	
	public static void assertDoubleArrayEquals(double[] a, double[] b, double epsilon, String... message) {
		
		try {
			for (int i = 0; i < a.length; i++) {
				assertDoubleEquals(a[i], b[i], epsilon, message);
			}
		} catch (Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
		
	}
	
	public static void assertDoubleArrayNotEquals(double[] a, double[] b, double epsilon, String... message) {
		if (a == null || b == null)
			return;
		if (a.length != b.length)
			return;
		
		try {
			for (int i = 0; i < a.length; i++) {
				assertDoubleNotEquals(a[i], b[i], epsilon, message);
			}
		} catch (Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
	}
}
