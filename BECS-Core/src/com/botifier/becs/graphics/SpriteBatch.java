package com.botifier.becs.graphics;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15.glMapBuffer;

import java.awt.Color;
import java.nio.ByteBuffer;

import org.joml.Math;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;

import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.Math2;
import com.botifier.becs.util.shapes.Polygon;
//import com.botifier.becs.util.shapes.Polygon.TriangulatedPolygon;
import com.botifier.becs.util.shapes.RotatableRectangle;

/**
 * Sprite Batch class
 * Based on https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/graphic/Renderer.java
 * @author Botifier
 *
 */
public class SpriteBatch {
	/**
	 * The vertex size
	 * TODO: Something better than this
	 */
	private static final int VERTEX_SIZE = 24;
	/**
	 * The default line with
	 * TODO: Change this to a config value
	 */
	private static final int DEFAULT_LINE_WIDTH = 2;

	/**
	 * The parent renderer
	 */
	private final Renderer r;

	/**
	 * The vertex buffer
	 */
	private ByteBuffer vertices;

	/**
	 * If the batch is currently drawing
	 */
	private boolean drawing = false;

	/**
	 * Number of vertices within the buffer
	 */
	private int numVertices = 0;

	/**
	 * Number of instances
	 * Not used right now
	 */
	private int numInstances = 0;

	/**
	 * A reusable rectangle
	 */
	private RotatableRectangle reuseRectangle = null;

	/**
	 * SpriteBatch constructor
	 * @param r Parent Renderer
	 */
	public SpriteBatch(Renderer r) {
		this.r = r;
		init();
	}


	/**
	 * Initialization
	 *
	 * all this does is grab the vertices buffer from the Renderer if it is null
	 */
	private void init() {
		if (vertices == null) {
			vertices = r.getVertices();
		}
	}
	/**
	 * Begins drawing
	 */
	public void begin() {
		if (!drawing) {
			//Maps the assigned portion of the render buffer to this
			vertices = glMapBuffer(GL_ARRAY_BUFFER, GL15.GL_WRITE_ONLY,
					r.getVertices().capacity() / r.getNumBatches(),
					r.getVertices());
		    drawing = true;
		}
	}

	/**
	 * Ends drawing
	 */
	public void end() {
		if (drawing) {
			drawing = false;
			flush();
		}
	}

	/**
	 * Pushes vertices to Renderer
	 */
	public void flush() {
		if (numVertices > 0 && vertices != null) {
			getRenderer().addVertices(numVertices);
			getRenderer().addInstances(numInstances);
			numInstances =0;
			numVertices = 0;
		}
	}

	/**
	 * Destroys this batch
	 */
	public void destroy() {
		r.removeBatch(this);
	}

	/**
	 * Checks if there is enough space in the vertice buffer
	 * @param requiredVertices int Required number of vertices
	 * @return boolean If there is enough space
	 */
	private boolean ensureSpace(int requiredVertices) {
		if (vertices == null || vertices.remaining() < requiredVertices * VERTEX_SIZE) {
			return false;
		}
		return true;
	}

	/**
	 * Draws a rectangle
	 * @param width Width
	 * @param height Height
	 * @param x X
	 * @param y Y
	 * @param c Color to use
	 */
	public void fillRectangle(float width, float height, float x, float y, float z, Color c) {
		drawTextureRegion(x, y, x + width, y + height, z, 0, 0, 1, 1, c);
	}

	/**
	 * Fills a triangle
	 * @param x1 float First x
	 * @param y1 float First y
	 * @param x2 float Second x
	 * @param y2 float Second y
	 * @param x3 float Third x
	 * @param y3 float Third y
	 * @param z float Z position
	 * @param s1 float First UV x
	 * @param t1 float First UV y
	 * @param s2 float Second UV x
	 * @param t2 float Second UV y
	 * @param s3 float Third UV x
	 * @param t3 float Third UV y
	 * @param c Color To use
	 */
	public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float z, float s1, float t1, float s2, float t2, float s3, float t3, Color c) {
		if ((vertices == null) || (vertices.remaining() < 72)) {
			//TODO: Fill triangle in renderer
			return;
		}

		drawVertex(x1, y1, z, c, s1, t1);
		drawVertex(x2, y2, z, c, s2, t2);
		drawVertex(x3, y3, z, c, s3, t3);

		numInstances += 1;

	}

	/**
	 * Places location data into the buffer in a rectangle
	 * @param x1 First X
	 * @param y1 First Y
	 * @param x2 Second X
	 * @param y2 Second Y
	 * @param s1 first UV x location
	 * @param t1 first UV y location
	 * @param s2 second UV x location
	 * @param t2 second UV y location
	 * @param c Color to use
	 */
	public void drawTextureRegion(float x1, float y1, float x2, float y2, float z, float s1, float t1, float s2, float t2, Color c) {
		if (!ensureSpace(6)) {
			r.drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
			return;
		}

		drawQuad(x1, y1, // Bottom-Left
				 x2, y1, // Bottom-Right
				 x2, y2, // Top-Right
				 x1, y2, // Top-Left
				 s1, t1, // Tri1 Bottom-Left
				 s1, t2, // Tri1 Top-Left
				 s2, t2, // Tri1 Top-Right
				 s1, t1, // Tri2 Bottom-Left
				 s2, t2, // Tri2 Top-Right
				 s2, t1, // tri2 Bottom-Right
				 z, c);

		numInstances++;
	}

	/**
	 * Draws a RotatableRectangle
	 * @param rotRect Rectangle to draw
	 * @param c Color to use
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, Color c) {
		drawRotatedRectangle(rotRect, 0, c, false);
	}

	/**
	 * Draws a RotatableRectangle
	 * @param rotRect Rectangle to draw
	 * @param z Z
	 * @param c Color to use
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z, Color c) {
		drawRotatedRectangle(rotRect, z, c, false);
	}

	/**
	 * Pushes data about the specified RotatableRectangle to the buffer
	 * @param rotRect RotatableRectangle to use
	 * @param c Color to use
	 * @param flipped Whether or not it should be flipped
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z, Color c, boolean flipped) {
		if (rotRect == null) {
			return;
		}
		if (!ensureSpace(6)) {
			r.drawRotatedRectangle(rotRect, c);
			return;
		}

		Vector2f bl = rotRect.getBottomLeft();
        Vector2f br = rotRect.getBottomRight();
        Vector2f tl = rotRect.getTopLeft();
        Vector2f tr = rotRect.getTopRight();

        if (flipped) {
            Vector2f temp = bl;
            bl = br;
            br = temp;
            temp = tl;
            tl = tr;
            tr = temp;
        }

        float[] bounds = {bl.x, bl.y,
        				  br.x, br.y,
        				  tr.x, tr.y,
        				  tl.x, tl.y};
        float[] texCoords = {0, 0,
        					 0, 1,
        					 1, 1,

        					 0, 0,
        					 1, 1,
        					 1, 0};

		drawQuad(bounds, texCoords, z, c);
        /*
		drawVertex(bl.x, bl.y, z, c, 0, 0);
		drawVertex(tl.x, tl.y, z, c, 0, 1);
		drawVertex(tr.x, tr.y, z, c, 1, 1);

		drawVertex(bl.x, bl.y, z, c, 0, 0);
		drawVertex(tr.x, tr.y, z, c, 1, 1);
		drawVertex(br.x, br.y, z, c, 1, 0);*/

		numInstances++;
	}

	/**
	 * Draws the outline of the specified polygon
	 * @param p Polygon to draw
	 * @param c Color to use
	 */
	public void drawPolygon(Polygon p, float z, Color c) {
		/*float s1 = 0f;
		float t1 = 0f;
		float s2 = 1f;
		float t2 = 1f;

		float r = c.getRed() / 255f;
		float g = c.getGreen() / 255f;
		float b = c.getBlue() / 255f;
		float a = c.getAlpha();*/

		//TriangulatedPolygon tp = p.triangulate();

		//drawRectangle(4, 4, p.getCenter().x-2, p.getCenter().y-2, c);
		for (int i = 0; i < p.getPoints().length; i++) {
			Vector2f point = p.getPoint(i);
			Vector2f next = p.getPoint((i + 1) % p.getPoints().length);

			fillRectangle(4, 4, point.x-2, point.y-2, z, c);
			drawLine(point.x, point.y, next.x, next.y, z, c, 2);
		}
	}

	/**
	 * Pushes vertex information to the buffer to render a polygon
	 * Can pretty much guarantee any texture that isn't a solid color will look funny
	 * @param p Polygon to use
	 * @param c Color to use
	 */
	public void fillPolygon(Polygon p, float z, Color c) {
		// Get center point
	    float centerX = p.getCenter().x;
	    float centerY = p.getCenter().y;

	    Vector2f[] points = p.getPoints();
	    int numPoints = points.length;

	    // Texture coordinates for triangle fan
	    float s1 = 0.5f; // Center point
	    float t1 = 0.5f;

	    // Draw as a triangle fan
	    for (int i = 0; i < numPoints; i++) {
	        // Current point
	        Vector2f current = points[i];
	        // Next point (wrap around to first point if at the end)
	        Vector2f next = points[(i + 1) % numPoints];

	        // Calculate texture coordinates for outer points
	        float angle1 = Math.atan2(current.y - centerY, current.x - centerX);
	        float s2 = Math.cos(angle1) * 0.5f + 0.5f;
	        float t2 = Math.sin(angle1) * 0.5f + 0.5f;

	        float angle2 = Math.atan2(next.y - centerY, next.x - centerX);
	        float s3 = Math.cos(angle2) * 0.5f + 0.5f;
	        float t3 = Math.sin(angle2) * 0.5f + 0.5f;

	        // Draw triangle
	        drawVertex(centerX, centerY, z, c, s1, t1);      // Center
	        drawVertex(current.x, current.y, z, c, s2, t2);  // Current point
	        drawVertex(next.x, next.y, z, c, s3, t3);        // Next point

	        numInstances++;
	    }
	}

	/**
	 * Line draw overload without z
	 */
	public void drawLine(float x, float y, float xx, float yx, Color c, float width) {
		drawLine(x, y, xx, yx, 0, c, width);
	}

	/**
	 * Draws a line by using a RotatatableRectangle
	 * @param x First X
	 * @param y First Y
	 * @param xx Second X
	 * @param yx Second Y
	 * @param z Z
	 * @param c Color to use
	 * @param width Width of the line
	 */
	public void drawLine(float x, float y, float xx, float yx, float z, Color c, float width) {
		if (!ensureSpace(6)) {
			r.drawLine(x, y, xx, yx, c, width);
			return;
		}

		float angle = Math2.calcAngle(x, y, xx, yx);
		float dist = new Vector2f(x, y).distance(xx, yx);

		Vector2f middle = Math2.getMidpoint(x, y, xx, yx);

		width = Math.max(width, DEFAULT_LINE_WIDTH);

		RotatableRectangle rr = getReuseableRectangle(middle, dist, width, angle);
		Image.WHITE_PIXEL.bind();
		drawRotatedRectangle(rr, z, c);
	}

	/**
	 * Draws a circle
	 * @param x X
	 * @param y Y
	 * @param radius Radius of the circle
	 * @param c Color of the circle
	 */
	public void drawCircle(float x, float y, float radius, Color c) {
		drawCircle(x, y, radius, c, 100, 5);
	}

	/**
	 * Pushes vertex information of a circle to the buffer
	 * @param x X
	 * @param y Y
	 * @param radius Radius of the circle
	 * @param c Color of the circle
	 * @param resolution Resolution of the circle. The higher the number the better the quality at the cost of performance
	 */
	public void drawFilledCircle(float x, float y, float radius, Color c, int resolution) {
		drawFilledCircle(x, y, 0, radius, c, resolution);
	}

	/**
	 * Pushes vertex information of a circle to the buffer
	 * Use a shader instead of this if you want a good looking circle
	 * @param x X
	 * @param y Y
	 * @param z Z
	 * @param radius Radius of the circle
	 * @param c Color of the circle
	 * @param resolution Resolution of the circle. The higher the number the better the quality at the cost of performance
	 */
	public void drawFilledCircle(float x, float y, float z, float radius, Color c, int resolution) {
		int adjRes = resolution * 3;

		if (!ensureSpace(adjRes)) {
			SpriteBatch b = getRenderer().getFirstOpenBatch();
			if (b.getVertices().remaining() <= 0) {
				return;
			}
			b.drawFilledCircle(x, y, z, radius, c, resolution);
			return;
		}

		float currentAngle = 0;
		float moveAngle = (float) (Math.PI / adjRes);

		float cosA, sinA, cosB, sinB;
		float x1, y1, x2, y2;

		for (int i = 0; i < resolution; i += 3) {
			cosA = Math.cos(currentAngle);
			sinA = Math.sin(currentAngle);

			cosB = Math.cos(currentAngle + moveAngle * 6);
			sinB = Math.sin(currentAngle + moveAngle * 6);

			x1 = x + cosA * radius;
			y1 = y + sinA * radius;

			x2 = x + cosB * radius;
			y2 = y + sinB * radius;

			drawVertex(x, y, z, c, 0.5f, 0.5f);
			drawVertex(x1, y1, z, c, 0, 1);
			drawVertex(x2, y2, z, c, 1, 1);

			numInstances ++;

	        currentAngle += moveAngle * 6;
		}
		numVertices += resolution;
	}

	/**
	 * Draws the outline of a circle
	 * @param x X
	 * @param y Y
	 * @param radius Radius of the circle
	 * @param c Color to use
	 * @param resolution Resolution of the circle. The higher the number the better the quality at the cost of performance
	 * @param lineWidth Width of the outline
	 */
	public void drawCircle(float x, float y, float radius, Color c, int resolution, int lineWidth) {
		drawCircle(x, y, 0, radius, c, resolution, lineWidth);
	}

	/**
	 * Draws the outline of a circle
	 * @param x X
	 * @param y Y
	 * @param z Z
	 * @param radius Radius of the circle
	 * @param c Color to use
	 * @param resolution Resolution of the circle. The higher the number the better the quality at the cost of performance
	 * @param lineWidth Width of the outline
	 */
	public void drawCircle(float x, float y, float z, float radius, Color c, int resolution, int lineWidth) {
		if (vertices == null) {
			return;
		}
		if (vertices.remaining() <= 24) {
			SpriteBatch b = getRenderer().getFirstOpenBatch();
			if (b.getVertices().remaining() <= 0) {
				return;
			}
			b.drawCircle(x, y, radius, c, resolution, lineWidth);
			return;
		}

		float moveAngle = (float) (2*Math.PI/resolution);

		for (int i = 0; i < resolution; i++) {
			float angle1 = i * moveAngle;
			float angle2 = (i + 1) * moveAngle;

			float x1 = x + Math.cos(angle1) * (radius - lineWidth / 2);
	        float y1 = y + Math.sin(angle1) * (radius - lineWidth / 2);

	        float x2 = x + Math.cos(angle2) * (radius - lineWidth / 2);
	        float y2 = y + Math.sin(angle2) * (radius - lineWidth / 2);

	        drawLine(x1, y1, x2, y2, z, c, lineWidth);
		}
	}

	public void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float z, Color c, int lineWidth) {
		if (vertices == null) {
			return;
		}
		if (vertices.remaining() < 12) {
			SpriteBatch b = getRenderer().getFirstOpenBatch();
			if (b.getVertices().remaining() <= 0) {
				return;
			}
			b.drawTriangle(x1, y1, x2, y2, x3, y3, z, c, lineWidth);
			return;
		}

		drawLine(x1, y1, x2, y2, z, c, lineWidth);
		drawLine(x2, y2, x3, y3, z, c, lineWidth);
		drawLine(x3, y3, x1, y1, z, c, lineWidth);
	}

	public void drawVertex(float x, float y, float z, Color c, float s, float t) {
		if (vertices.remaining() < 24) {
			System.out.println("Out of space: "+x+", "+y+", "+z);
			return;
		}
		vertices.putFloat(x).putFloat(y).putFloat(z).putInt(c.getRGB()).putFloat(s).putFloat(t);
		numVertices++;
	}

	/**
	 * Draws a single triangle
	 * @param x1 float First position x
	 * @param y1 float First position y
	 * @param x2 float Second position x
	 * @param y2 float Second position y
	 * @param x3 float Third position x
	 * @param y3 float Third position y
	 * @param s1 float Texture coord position 1 x
	 * @param t1 float Texture coord position 1 y
	 * @param s2 float Texture coord position 2 x
	 * @param t2 float Texture coord position 2 y
	 * @param s3 float Texture coord position 3 x
	 * @param t3 float Texture coord position 3 y
	 * @param z float Depth
	 * @param c Color Color
	 */
	public void drawTri(float x1, float y1, float x2, float y2, float x3, float y3,
						float s1, float t1, float s2, float t2, float s3, float t3,
						float z, Color c) {
		drawVertex(x1, y1, z, c, s1, t1);
		drawVertex(x2, y2, z, c, s2, t2);
		drawVertex(x3, y3, z, c, s3, t3);
	}

	/**
	 * Draws a quad
	 * @param x1 float Bottom-Left x
	 * @param y1 float Bottom-Left y
	 * @param x2 float Bottom-Right x
	 * @param y2 float Bottom-Right y
	 * @param x3 float Top-Right x
	 * @param y3 float Top-Right y
	 * @param x4 float Top-Left x
	 * @param y4 float Top-Left y
	 * @param s1 float Triangle 1 Bottom-Left texture coordinate x
	 * @param t1 float Triangle 1 Bottom-Left texture coordinate y
	 * @param s2 float Triangle 1 Top-Left texture coordinate x
	 * @param t2 float Triangle 1 Top-Left texture coordinate y
	 * @param s3 float Triangle 1 Top-Right texture coordinate x
	 * @param t3 float Triangle 1 Top-Right texture coordinate y
	 * @param s4 float Triangle 2 Bottom-Left texture coordinate x
	 * @param t4 float Triangle 2 Bottom-Left texture coordinate y
	 * @param s5 float Triangle 2 Top-Right texture coordinate x
	 * @param t5 float Triangle 2 Top-Right texture coordinate y
	 * @param s6 float Triangle 2 Bottom-Right texture coordinate x
	 * @param t6 float Triangle 2 Bottom-Right texture coordinate y
	 * @param z float Depth
	 * @param c Color Color
	 */
	public void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
						 float s1, float t1, float s2, float t2, float s3, float t3,
						 float s4, float t4, float s5, float t5, float s6, float t6,
						 float z,  Color c) {
	    drawTri(x1, y1,
	    		x4, y4,
	    		x3, y3,
	    		s1, t1,
	    		s2, t2,
	    		s3, t3,
	    		z, c);
	    drawTri(x1, y1,
	    		x3, y3,
	    		x2, y2,
	    		s4, t4,
	    		s5, t5,
	    		s6, t6,
	    		z, c);
	}

	/**
	 * Draws a quad
	 * @param bounds float[] Formatted as below, must have exactly 8 entries
	 * 						 {
	 * 						   Bottom-Left x, Bottom Left y,
	 * 						   Bottom-Right x, Bottom Right y,
	 * 						   Top-Right x, Top Right y,
	 * 						   Top-Left x, Top Left y
	 * 						 }
	 * @param texCoords float[] Formatted as below, must have exactly 12 entries, each 6 entries represent a triangle
	 *  					 {
	 * 						   //Tri1
	 * 						   FirstCoord x, FirstCoord y,
	 * 						   SecondCoord x, SecondCoord y,
	 *                         ThirdCoord x, ThirdCoord y,
	 *                         //Tri2
	 *                         FirstCoord x, FirstCoord y,
	 * 						   SecondCoord x, SecondCoord y,
	 *                         ThirdCoord x, ThirdCoord y,
	 * 						 }
	 *                       A square with basic texture mapping should look like below
	 *                       {
	 * 						   //Tri1
	 * 						   0, 0, //Bottom-Left
	 * 						   0, 1, //Top-Left
	 *                         1, 1, //Top Right
	 *                         //Tri2
	 *                         0, 0, //Bottom-Left
	 * 						   1, 1, //Top-Right
	 *                         1, 0, //Bottom-Right
	 * 						 }
	 * @param z float Depth
	 * @param c Color Color
	 */
	public void drawQuad(float[] bounds, float[] texCoords, float z, Color c) {
		if (vertices == null) {
			return;
		}
		if (bounds.length != 8) {
			throw new IllegalArgumentException("Bounds array must contain exactly 8 entries.");
		}
		if (texCoords.length != 12) {
			throw new IllegalArgumentException("texCoord array must contain exactly 12 entries.");
		}

		drawQuad(bounds[0], bounds[1],
				bounds[2], bounds[3],
				bounds[4], bounds[5],
				bounds[6], bounds[7],
				texCoords[0], texCoords[1],
				texCoords[2], texCoords[3],
				texCoords[4], texCoords[5],
				texCoords[6], texCoords[7],
				texCoords[8], texCoords[9],
				texCoords[10], texCoords[11],
				z, c);
	}

	/**
	 * Whether or not the batch is currently drawing
	 * @return
	 */
	public boolean isDrawing() {
		return drawing;
	}

	/**
	 * Whether or not the buffer is full
	 * @return
	 */
	public boolean isFull() {
		return  vertices.remaining() < 24;
	}

	/**
	 * Returns the buffer
	 * @return The vertex buffer
	 */
	public ByteBuffer getVertices() {
		return vertices;
	}

	/**
	 * Returns the parent Renderer
	 * @return The parent Renderer
	 */
	public Renderer getRenderer() {
		return r;
	}

	/**
	 * Returns the current number of vertices in the
	 * @return
	 */
	public int getNumVertices() {
		return numVertices;
	}

	/**
	 * Modifies reuseRectangle for use
	 *
	 * Is this a good idea?
	 * probably not.
	 *
	 * @param center Center of the rectangle
	 * @param width width of the rectangle
	 * @param height height of the rectangle
	 * @param rotation rotation of the rectangle
	 * @return reuseRectangle
	 */
	private RotatableRectangle getReuseableRectangle(Vector2f center, float width, float height, float rotation) {
		if (reuseRectangle == null) {
			return reuseRectangle = new RotatableRectangle(center, width, height, rotation);
		}
		reuseRectangle.setWidth(width);
		reuseRectangle.setHeight(height);
		reuseRectangle.setAngle(rotation);
		reuseRectangle.setCenter(center);
		return reuseRectangle;
	}

}
