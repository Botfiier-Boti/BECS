package com.botifier.becs.util;

import org.joml.Vector2f;

import com.botifier.becs.util.shapes.Polygon;

public class CollisionUtil {

	public static Vector2f projectPolygon(Polygon p, Vector2f axis) {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		for (int i = 0; i < p.getPoints().length; i++) {
			Vector2f v = p.getPoint(i);
			float proj = v.dot(axis);

			if (proj < min) {
				min = proj;
			}
			if (proj > max) {
				max = proj;
			}
		}

		return new Vector2f(min, max);

	}

	public static class PolygonOutput {
		private Vector2f normal;
		private float depth;

		public PolygonOutput() {
			this.normal = new Vector2f();
			this.depth = Float.MAX_VALUE;
		}

		public PolygonOutput(Vector2f normal, float depth) {
			this.normal = normal;
			this.depth = depth;
		}

		public void setDepth(float depth) {
			this.depth = depth;
		}

		public void setNormal(Vector2f normal) {
			this.normal = normal;
		}

		public Vector2f getNormal() {
			return this.normal;
		}

		public float getDepth() {
			if (depth < 0) {
				return 0;
			}
			return this.depth;
		}
 	}
}
