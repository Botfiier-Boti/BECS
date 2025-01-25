package com.botifier.junit.assertions;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Vector2f;

public class VectorAssertions {
	public static void assertVector2fEquals(Vector2f a, Vector2f b, float epsilon, String... message) {
		assertTrue(a.equals(b, epsilon), message.length == 0 ? String.format("Vector %s is not epsilon range of %s", a, b) : String.join("\n", message));
	}
	
	public static void assertVector2fNotEquals(Vector2f a, Vector2f b, float epsilon, String... message) {
		assertFalse(a.equals(b, epsilon), message.length == 0 ? String.format("Vector %s is in epsilon range of %s", a, b) : String.join("\n", message));
	}
	
	public static void assertVector2fArrayEquals(Vector2f[] a, Vector2f[] b, float epsilon, String... message) {
		try {
			for (int i = 0; i < a.length; i++) {
				assertVector2fEquals(a[i], b[i], epsilon);
			}
		} catch(Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
	}
	
	public static void assertVector2fArrayNotEquals(Vector2f[] a, Vector2f[] b, float epsilon, String... message) {
		if (a == null || b == null)
			return;
		if (a.length != b.length)
			return;
		
		try {
			for (int i = 0; i < a.length; i++) {
				assertVector2fNotEquals(a[i], b[i], epsilon);
			}
		} catch(Exception e) {
			fail(message.length == 0 ? "An error occured": String.join("\n", message), e);
		}
	}
}
