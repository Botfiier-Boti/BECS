package com.botifier.becs.graphics;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.botifier.becs.Game;
import com.botifier.becs.WorldState;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.graphics.shader.Shader;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.graphics.text.Font;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;

/**
 * Based on
 * https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/graphic/Renderer.java
 * Renderer class
 *
 * Renders using orthographic projection
 * @author Botifier
 */
public class Renderer {

	/**
	 * Size of each buffer
	 * Divisible by the size of each vertex for maximum efficiency.
	 */
	public static final int BUFFER_SIZE = 19200;//2400000;//30720;

	/**
	 * Amount of batches the game can create
	 */
	public static final int BATCH_AMOUNT = 10;

	/**
	 * Whether or not the zoom is being used
	 */
	private boolean useZoom = true;

	/**
	 * Whether or not a flush has occurred this frame
	 */
	private boolean hasRendered = false;

	/**
	 * The camera zoom
	 */
	private float currentZoom = 1f;

	/**
	 * The location of the camera center
	 */
	private Vector3f center = new Vector3f(0, 0, 0);

	/**
	 * The TRUE location of the camera center, no context is needed
	 */
	private Vector3f trueCenter = new Vector3f(0, 0, 0);

	/**
	 * Each Sprite Batch
	 */
	private List<SpriteBatch> batches = new CopyOnWriteArrayList<>();

	/**
	 * The Byte Buffer
	 */
	private ByteBuffer vertices;

	/**
	 * Number of vertices to draw
	 */
	private int numVertices;

	@SuppressWarnings("unused")
	private int numInstances;

	/**
	 * Current number of batches
	 */
	private int numBatches = 0;

	/**
	 * Whether or not the renderer is currently attempting to draw
	 */
	private boolean drawing;

	/**
	 * The Vertex Array Object
	 */
	private VAO vao;

	/**
	 * The Vertex Buffer Object
	 */
	private VBO vbo;

	/**
	 * The Element Buffer Object
	 */
	private VBO ebo;

	/**
	 * The vertex shader
	 */
	private Shader vertex;

	/**
	 * The fragment shader
	 */
	private Shader fragment;

	/**
	 * The shader program
	 */
	private ShaderProgram program;

	/**
	 * The automatic batcher
	 */
	private AutoBatcher batcher;

	/**
	 * Debug/Default font
	 */
	private Font debug;

	/**
	 * Current font
	 */
	private Font font;

	/**
	 * The camera
	 */
	private Matrix4f projection;

	/**
	 * Used to check if something is within the camera
	 */
	private FrustumIntersection intersection;

	/**
	 * Renderer constructor
	 */
	public Renderer() {
		debug = new Font();
		font = new Font();
	}

	/**
	 * Renderer constructor
	 *
	 * @param f Font to use
	 */
	public Renderer(Font f) {
		this();
		font = f;
	}

	/**
	 * Renderer initializer
	 *
	 * @param window Game To use
	 */
	public void init(Game window) {
		//Setup the auto batcher
		batcher = new AutoBatcher();
		//Setup the buffers
		setupBuffers();
		//General setup
		setup();

		//Enable blending
		glEnable(GL_BLEND);
		//Enable the depth test
		glEnable(GL11.GL_DEPTH_TEST);
		//Enable alpha blending
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		//Use the Less than or equal to depth function
		GL11.glDepthFunc(GL11.GL_LEQUAL);
	}

	/**
	 * Renders everything to the supplied FBO
	 * @param g Game Game of origin
	 * @param fbo FBO fbo to use
	 * @param alpha float Alpha value, for interpolation
	 */
	public void renderAllToFBO(Game g, FBO fbo, float alpha) {
		//Binds the FBO
		fbo.bind();
		//Clears the depth and color buffers
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		//Creates a camera rectangle
		RotatableRectangle camera = new RotatableRectangle(getCameraCenter().x, getCameraCenter().y, getCurrentWidth(), getCurrentHeight());
		//Creates a world state
		WorldState ws = new WorldState(camera.toPolygon(), false);
		//Perform draw commands
		g.draw(this, ws, camera, alpha);
		//Draw the auto batcher
		getAutoBatcher().draw(this);
		//Unbind the FBO
		fbo.unbind();
		//Draw the FBO image
		fbo.draw(this);
	}


	/**
	 * Writes text to the screen
	 *
	 * @param cs CharSequence Text to write
	 * @param x float X location
	 * @param y float Y location
	 */
	public void writeText(CharSequence cs, float x, float y) {
		writeText(cs, x, y, Color.WHITE);
	}

	/**
	 * Writes text to the screen
	 *
	 * @param cs Text to write
	 * @param x  X location
	 * @param y  Y location
	 * @param c  Color To use
	 */
	public void writeText(CharSequence cs, float x, float y, Color c) {
		if (font == null) {
			debug.draw(this, cs, x, y, c);
		} else {
			font.draw(this, cs, x, y, c);
		}
	}

	/**
	 * Draws specified image's texture to the screen
	 * Doesn't bind the texture, just uses the properties
	 *
	 * @param i Image To use
	 * @param x float X location
	 * @param y float Y location
	 * @param z float Z location
	 * @param c Color To use
	 */
	public void drawImage(Image i, float x, float y, float z, Color c) {
		float x1 = x;
		float y1 = y;
		float x2 = x1 + (i.getWidth());
		float y2 = y1 + (i.getHeight());

		float s1 = i.getOffset().x / i.getTexture().getWidth();
		float t1 = i.getOffset().y / i.getTexture().getHeight();
		float s2 = (i.getOffset().x + i.getInternalWidth()) / i.getTexture().getWidth();
		float t2 = (i.getOffset().y + i.getInternalHeight()) / i.getTexture().getHeight();

		drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
	}

	/**
	 * Draws a section of the binded texture
	 * @param x float X location
	 * @param y float Y location
	 * @param z float Z location
	 * @param width float The width
	 * @param height float The height
	 * @param tx float The internal x location
	 * @param ty float The internal t location
	 * @param tWidth float The width of the internal texture
	 * @param tHeight float The height of the internal texture
	 * @param c Color To use
	 */
	public void drawSubImage(float x, float y, float z, float width, float height, float tx, float ty, float tWidth,
			float tHeight, Color c) {
		float x1 = x;
		float y1 = y;
		float x2 = x1 + width;
		float y2 = y1 + height;

		float s1 = tx / tWidth;
		float t1 = ty / tHeight;
		float s2 = (tx + width) / tWidth;
		float t2 = (ty + height) / tHeight;

		drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
	}

	/**
	 * Draws a rectangle
	 * @param x float The X
	 * @param y float The Y
	 * @param z float The Z
	 * @param width float The width
	 * @param height float The height
	 * @param c Color To use
	 */
	public void drawRectangle(float x, float y, float z, float width, float height, Color c) {
		float x1 = x;
		float y1 = y;
		float x2 = x1 + (width);
		float y2 = y1 + (height);

		float s1 = 0;
		float t1 = 0;
		float s2 = 1;
		float t2 = 1;

		drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
	}

	/**
	 * Draws texture to the screen
	 *
	 * @param texture Texture to use
	 * @param x       X location
	 * @param y       Y location
	 * @param c       Color to use
	 */
	public void drawTexture(Texture texture, float x, float y, Color c) {
		float x1 = x;
		float y1 = y;
		float x2 = x1 + texture.getWidth();
		float y2 = y1 + texture.getHeight();

		float s1 = 0;
		float t1 = 0;
		float s2 = 1;
		float t2 = 1;

		drawTextureRegion(x1, y1, x2, y2, 0, s1, t1, s2, t2, c);
	}

	/**
	 * Draws specified texture region
	 *
	 * @param t  Texture to use
	 * @param x  X location
	 * @param y  Y location
	 * @param rX X position in texture
	 * @param rY Y position in texture
	 * @param rW Width to use
	 * @param rH Height to use
	 * @param c  Color to use
	 */
	public void drawTextureRegion(Texture t, float x, float y, float z, float rX, float rY, float rW, float rH,
			Color c) {

		float x1 = x;
		float y1 = y;
		float x2 = x + rW;
		float y2 = y + rH;

		float s1 = rX / t.getWidth();
		float t1 = rY / t.getHeight();
		float s2 = (rX + rW) / t.getWidth();
		float t2 = (rY + rH) / t.getHeight();

		drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
	}

	/**
	 * Draws specified texture region
	 *
	 * @param x1 First X position
	 * @param y1 First Y position
	 * @param x2 Second X position
	 * @param y2 Second Y position
	 * @param s1 X scalar
	 * @param t1 Y scalar
	 * @param s2 Width scalar
	 * @param t2 Height scalar
	 * @param c  Color to use
	 */
	public void drawTextureRegion(float x1, float y1, float x2, float y2, float z, float s1, float t1, float s2,
			float t2, Color c) {
		/*
		 * FrustumIntersection i = getFrustumIntersection(); if (i.testPlaneXY(x1, y1,
		 * x2, y2) == false) return;
		 */

		SpriteBatch batch = getFirstOpenBatch(144);
		if (batch != null) {
			batch.drawTextureRegion(x1, y1, x2, y2, z, s1, t1, s2, t2, c);
		}
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param c       Color to use
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, Color c) {
		drawRotatedRectangle(rotRect, 0, c);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param z       Z
	 * @param c       Color to use
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z, Color c) {
		drawRotatedRectangle(rotRect, z, c, false);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect) {
		drawRotatedRectangle(rotRect, 0);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param z       Z
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z) {
		drawRotatedRectangle(rotRect, z, Color.white, false);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param z       Z
	 * @param flipped Whether or not it is flipped
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z, boolean flipped) {
		drawRotatedRectangle(rotRect, z, Color.white, flipped);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param flipped Whether or not it is flipped
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, boolean flipped) {
		drawRotatedRectangle(rotRect, 0, flipped);
	}

	/**
	 * Draws Rotated Rectangle
	 *
	 * @param rotRect Rotated Rectangle to use
	 * @param c       Color to use
	 * @param flipped Whether or not it is flipped
	 */
	public void drawRotatedRectangle(RotatableRectangle rotRect, float z, Color c, boolean flipped) {
		SpriteBatch batch = getFirstOpenBatch(144);

		if (batch != null) {
			batch.drawRotatedRectangle(rotRect, z, c, flipped);
		}
	}
	
	public void drawRotatedRectangleWithTexCoords(RotatableRectangle rotRect, float[] texCoords, float z, Color c, boolean flipped) {
		SpriteBatch batch = getFirstOpenBatch(144);

		if (batch != null) {
			batch.drawRotatedRectangle(rotRect, texCoords, z, c, flipped);
		}
	}

	/**
	 * Draws a line
	 *
	 * @param x1    First X position
	 * @param y1    First Y position
	 * @param x2    Second X position
	 * @param y2    Second Y position
	 * @param c     Color to use
	 * @param width Line width
	 */
	public void drawLine(float x1, float y1, float x2, float y2, Color c, float width) {
		SpriteBatch batch = getFirstOpenBatch();

		if (batch != null) {
			batch.drawLine(x1, y1, x2, y2, 1, c, width);
		}
	}

	/**
	 * Draws a circle
	 *
	 * @param x          X position
	 * @param y          Y position
	 * @param radius     Circle radius
	 * @param c          Color to use
	 * @param resolution Circle resolution
	 * @param lineWidth  Line width
	 */
	public void drawCircle(float x, float y, float radius, Color c, int resolution, int lineWidth) {
		SpriteBatch batch = getFirstOpenBatch(resolution * 24);

		if (batch != null) {
			batch.drawCircle(x, y, radius, c, resolution, lineWidth);
		}
	}

	/**
	 * Draws a filled circle
	 *
	 * @param x          X position
	 * @param y          Y position
	 * @param radius     Circle radius
	 * @param c          Color to use
	 * @param resolution Circle resolution
	 */
	public void drawFilledCircle(float x, float y, float radius, Color c, int resolution) {
		SpriteBatch batch = getFirstOpenBatch();

		if (batch != null) {
			batch.drawFilledCircle(x, y, radius, c, resolution);
		}
	}

	/**
	 * Fills the specified polygon using the specified color
	 *
	 * @param p Polygon to use
	 * @param c Color to use
	 */
	public void fillPolygon(Polygon p, float z, Color c) {
		SpriteBatch batch = getFirstOpenBatch();

		if (batch != null) {
			batch.fillPolygon(p, z, c);
		}
	}

	/**
	 * Draws the specified polygon using specified color
	 *
	 * @param p Polygon to use
	 * @param c Color to use
	 */
	public void drawPolygon(Polygon p, float z, Color c) {
		SpriteBatch batch = getFirstOpenBatch();

		if (batch != null) {
			batch.drawPolygon(p, z, c);
		}
	}

	/**
	 * Destroys the renderer and everything within
	 */
	public void destroy() {
		begin();
		end();
		if (vao != null) {
			System.out.println("Deleting VAO...");
			vao.delete();
		}
		System.out.println("Deleting VBO...");
		vbo.delete();
		System.out.println("Deleting EBO...");
		ebo.delete();
		System.out.println("Deleting Shader Program...");
		program.delete();
		//System.out.println("Deleting Font Textures...");
		//font.delete();
		//debug.delete();
		//System.out.println("Deleting White Pixel Texture");
		//Image.WHITE_PIXEL.destroy();
		System.out.println("Purging textures...");
		Texture.purgeTextures();
		
		System.out.println("Removing Batches...");
		if (batches != null) {
			for (int i = batches.size() - 1; i >= 0; i--) {
				SpriteBatch batch = batches.get(i);
				if (batches == null) {
					continue;
				}
				batch.destroy();
			}
		}

		System.out.println("Clearing Vertices...");
		if (vertices != null) {
			MemoryUtil.memFree(vertices);
			vertices = null;
		}

		System.out.println("Done!");
	}

	/**
	 * Begin without a specified shader program
	 */
	public void begin() {
		begin(null);
	}
	/**
	 * Begins the drawing process
	 * @param p ShaderProgram To use
	 */
	public void begin(ShaderProgram p) {
		if (drawing || (vertices == null)) {
			return;
		}
		if (p != null) {
			p.use();
		} else {
			program.use();
			updateProjectionMatrix(program, projection);
		}
		vbo.bind(GL_ARRAY_BUFFER);

		batches.forEach(SpriteBatch::begin);
		drawing = true;
		numVertices = 0;
	}

	/**
	 * Ends the drawing process
	 */
	public void end() {
		if (!drawing) {
			return;
		}
		drawing = false;
		glUnmapBuffer(GL_ARRAY_BUFFER);
		batches.forEach(SpriteBatch::end);
		long time = 0;
		if (Game.isDebug()) {
			time = System.nanoTime();
		}

		flush();
		if (Game.isDebug()) {
			System.out.println("DEBUG: Flush took " + (System.nanoTime() - time) + "ns");
		}
	}

	/**
	 * Flushes information to be rendered
	 */
	public void flush() {
		batches.forEach(SpriteBatch::flush);
		if (numVertices > 0) {

			vao.bind();

			glDrawArrays(GL_TRIANGLES, 0, numVertices);

			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glBindTexture(GL_TEXTURE_2D, 0);
		    glEnableVertexAttribArray(0);

			if (Game.isDebug()) {
				System.out.println("DEBUG: Drew " + numVertices + " vertices using texture ID:"
						+ (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) - 1));
			}
			numVertices = 0;
			numInstances = 0;
		}
		useZoom = true;
		hasRendered = true;
	}

	/**
	 * Refreshes projection
	 */
	public void refreshWindow() {
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		projection = new Matrix4f().ortho(0, width * getZoom(), 0, height * getZoom(), 100, -100);
		projection.translate(center);
		setProjectionMatrix(program, projection);
		GL11.glViewport(0, 0, Game.getCurrent().getWidth(), Game.getCurrent().getHeight());
	}

	/**
	 * Resets the zoom
	 */
	public void resetZoom() {
		setZoom(1f);
	}

	/**
	 * Temporarily resets the camera
	 */
	public void tempResetCamera() {
		tempResetZoom();
		tempResetCenter();
	}

	/**
	 * Temporarily Resets the zoom
	 */
	public void tempResetZoom() {

		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();
		//useZoom = false;
		Matrix4f projection = new Matrix4f().ortho2D(0, width, 0, height);
		projection.setTranslation(center);
		updateProjectionMatrix(program, projection);
	}

	/**
	 * Temporarily resets the camera center
	 */
	public void tempResetCenter() {
		if (center != null && center.equals(0, 0, 0)) {
			return;
		}
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		Matrix4f projection = new Matrix4f().ortho2D(0, width * getZoom(), 0, height * getZoom());

		updateProjectionMatrix(program, projection);
	}
	
	/**
	 * Temporarily moves the camera
	 * @param pos Vector2f Position to move to
	 * @param offset Vector2f Offset
	 */
	public void tempSetCenter(Vector2f pos, Vector2f offset) {
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		Matrix4f projection = new Matrix4f().ortho2D(0, width * getZoom(), 0, height * getZoom());
		Vector3f center = new Vector3f(-pos.x + (width * getZoom()) / 2 + offset.x, -pos.y + (height * getZoom()) / 2 + offset.y, 0);
		projection.translate(center);

		updateProjectionMatrix(program, projection);
	}

	/**
	 * Resets the camera center
	 */
	public void resetCenter() {
		setCameraCenter(new Vector2f(0, 0));
	}

	/**
	 * Sets the camera's zoom
	 *
	 * @param zoom Zoom to use; larger numbers zoom out more
	 */
	public void setZoom(float zoom) {
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		currentZoom = zoom;
		useZoom = true;

		projection = new Matrix4f().ortho2D(0, width * currentZoom, 0, height * currentZoom);
		setProjectionMatrix(program, projection);
	}

	/**
	 * Sets the camera's offset
	 *
	 * @param pos Position to use
	 */
	public void setOffset(Vector2f pos) {
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		projection = new Matrix4f().ortho2D(0, width * getZoom(), 0, height * getZoom());
		center = new Vector3f(-pos.x, -pos.y, 0);
		trueCenter = new Vector3f(pos, 0);
		projection.translate(center);
		setProjectionMatrix(program, projection);
	}

	/**
	 * Sets the center of the camera
	 *
	 * @param pos Position to use
	 */
	public void setCameraCenter(Vector2f pos) {
		setCameraCenter(pos, new Vector2f());
	}

	/**
	 * Sets the center of the camera with an offset
	 * 
	 * @param pos Vector2f The camera's center
	 * @param offset Vector2f The offset
	 */
	public void setCameraCenter(Vector2f pos, Vector2f offset) {
		int width = Game.getCurrent().getWidth(), height = Game.getCurrent().getHeight();

		projection = new Matrix4f().ortho2D(0, width * getZoom(), 0, height * getZoom());
		center = new Vector3f(-pos.x + (width * getZoom()) / 2 + offset.x, -pos.y + (height * getZoom()) / 2 + offset.y, 0);
		trueCenter = new Vector3f(pos, 0);
		projection.translate(center);

		setProjectionMatrix(program, projection);
	}

	/**
	 * Updates the cameras projection without updating the main projection
	 * @param program ShaderProgram To update
	 * @param projection Matrix4f To use
	 */
	public void updateProjectionMatrix(ShaderProgram program, Matrix4f projection) {
		program.use();
		int uniProjection = program.getUniformLocation("projection");
		program.setUniform(uniProjection, projection);
		if (intersection != null) {
			intersection.set(projection);
		} else {
			intersection = new FrustumIntersection(projection);
		}
		GL11.glViewport(0, 0, Game.getCurrent().getWidth(), Game.getCurrent().getHeight());
	}

	/**
	 * Sets the projection
	 * @param program ShaderProgram To update
	 * @param projection Matrix4f to use
	 */
	public void setProjectionMatrix(ShaderProgram program, Matrix4f projection) {
		this.projection = projection;
		updateProjectionMatrix(program, projection);
	}

	/**
	 * Setup the basics
	 */
	private void setup() {
		//Setup tracking variables
		numVertices = 0;
		drawing = false;
		
		//Setup the zoom
		currentZoom = 1;

		//Adds the first SpriteBatch
		numBatches++;
		SpriteBatch use = new SpriteBatch(this);
		batches.add(use);

		//Setup the shaders
		vertex = Shader.loadShader(GL_VERTEX_SHADER, "default.vert");
		fragment = Shader.loadShader(GL_FRAGMENT_SHADER, "default.frag");

		//Setup the shader program
		program = new ShaderProgram(vertex, fragment);

		//Delete the shaders they aren't needed anymore
		vertex.delete();
		fragment.delete();

		//Initializes the projection
		long window = GLFW.glfwGetCurrentContext();
		int width, height;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer widthBuffer = stack.mallocInt(1);
			IntBuffer heightBuffer = stack.mallocInt(1);
			GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
			width = widthBuffer.get();
			height = heightBuffer.get();
		}
		projection = new Matrix4f().ortho2D(0, width * currentZoom, 0, height * currentZoom);

		//Initializes the shader program with fragColor as the fragColor variable
		program.init("fragColor");
		setupShader(program);

		//List the attributes to the console
		int numAttributes = GL20.glGetProgrami(program.getId(), GL20.GL_ACTIVE_ATTRIBUTES);
		for (int i = 0; i < numAttributes; i++) {
		    IntBuffer sizeBuffer = BufferUtils.createIntBuffer(1);
		    IntBuffer typeBuffer = BufferUtils.createIntBuffer(1);

		    // Get the name of the attribute
		    String name = GL20.glGetActiveAttrib(program.getId(), i, 256, sizeBuffer, typeBuffer);

		    // Get the location of the attribute
		    int location = program.getAttributeLocation(name);

		    System.out.println("Attribute " + i + ": " + name + " Location: " + location);
		}
	}

	/**
	 * Initializes shader
	 */
	public void setupShader(ShaderProgram sp) {
		sp.use();

		specifyVertexAttributes(sp);

		int uniTex = sp.getUniformLocation("texImage");
		sp.setUniform(uniTex, 0);

		Matrix4f model = new Matrix4f();
		int uniModel = sp.getUniformLocation("model");
		sp.setUniform(uniModel, model);

		Matrix4f view = new Matrix4f();
		int uniView = sp.getUniformLocation("view");
		sp.setUniform(uniView, view);

		updateProjectionMatrix(sp, projection);
	}

	/**
	 * Sets up the buffers
	 */
	public void setupBuffers() {
		vbo = new VBO();
		vbo.bind(GL_ARRAY_BUFFER);

		ebo = new VBO();
		ebo.bind(GL_ELEMENT_ARRAY_BUFFER);

		if (Game.supportsOpenGL32()) {
			vao = new VAO();
			vao.bind();
		} else {
			throw new RuntimeException("OpenGL 3.2 not supported.");
		}

		vertices = MemoryUtil.memAlloc(BUFFER_SIZE);

		long size = BUFFER_SIZE * (6 * Float.BYTES + 4);
		vbo.uploadData(GL_ARRAY_BUFFER, size, GL_DYNAMIC_DRAW);
	}

	/**
	 * Specifies shader information
	 */
	private void specifyVertexAttributes(ShaderProgram program) {


		final int len = 5 * Float.BYTES + 4;
		program.use();
		/* Specify Vertex Pointer */
		int posAttrib = program.getAttributeLocation("position");
		program.enableVertexAttribute(posAttrib);
		program.pointVertexAttribute(posAttrib, 3, len, 0);

		/* Specify Color Pointer */
		int colAttrib = program.getAttributeLocation("color");
		program.enableVertexAttribute(colAttrib);
		program.pointVertexAttribute(colAttrib, 1, len, 3 * Float.BYTES);

		/* Specify Texture Pointer */
		int texAttrib = program.getAttributeLocation("texcoord");
		program.enableVertexAttribute(texAttrib);
		program.pointVertexAttribute(texAttrib, 2, len, 3 * Float.BYTES + 4);
	}

	/**
	 * Resets the font
	 */
	public void resetFont() {
		font = debug;
	}

	/**
	 * Sets the font
	 *
	 * @param f
	 */
	public void setFont(Font f) {
		this.font = f;
	}

	/**
	 * Add num vertices
	 *
	 * @param num To add
	 */
	public void addVertices(int num) {
		numVertices += num;
	}

	/**
	 * Add num to instances
	 *
	 * @param num To add
	 */
	public void addInstances(int num) {
		numInstances += num;
	}

	public void setShaderProgram(ShaderProgram program) {
		this.program = program;
	}
	
	/**
	 * 
	 * Returns the first open sprite batch and expands the buffer if needed
	 *
	 * @return SpriteBatch that is open
	 */
	public SpriteBatch getFirstOpenBatch() {
		return getFirstOpenBatch(0);
	}

	/**
	 * Returns the first open sprite batch and expands the buffer if needed
	 *
	 * @param vert int Number of vertices needed
	 * @return SpriteBatch that is open
	 */
	public SpriteBatch getFirstOpenBatch(int vert) {
		if (vertices == null)
			return null;
		SpriteBatch use = null;

		Optional<SpriteBatch> lookup = batches.stream().filter(Objects::nonNull).filter(batch -> {
			if (batch == null)
				return false;
			if (batch.getVertices() == null)
				return false;
			if (batch.isFull())
				return false;
			if (batch.getVertices().remaining() < vert)
				return false;
			
			return true;
		}).findFirst();
		
		if (lookup.isPresent())
			use = lookup.get();
		else {
			if (vertices.capacity() >= BUFFER_SIZE * (BATCH_AMOUNT + 1)) {
				//If over capacity, finish rendering and then return the first batch
				end();
				begin();
				return batches.get(0);
			}
			//If this is done mid draw finish rendering
			if (drawing) {
				end();
			}
			System.out.println("Getting new batch..");
			numBatches++;
			MemoryUtil.memFree(vertices);
			vertices = MemoryUtil.memAlloc(numBatches * BUFFER_SIZE);
			// vertices = MemoryUtil.memRealloc(vertices, numBatches * BUFFER_SIZE);

			System.out.println("Current capacity: " + vertices.capacity());
			System.out.println("Current remaining: " + vertices.remaining());

			use = new SpriteBatch(this);
			batches.add(use);
		}

		return use;
	}

	/**
	 * Removes specified Sprite Batch
	 *
	 * @param b Batch to remove
	 */
	public void removeBatch(SpriteBatch b) {
		if (batches.remove(b)) {
			numBatches--;
			// MemoryUtil.memFree(vertices);
			// vertices = MemoryUtil.memAlloc(numBatches * BUFFER_SIZE);//
			if (numBatches * BUFFER_SIZE > 0) {
				if (vertices != null) {
					MemoryUtil.memFree(vertices);
					vertices = MemoryUtil.memAlloc(numBatches * BUFFER_SIZE);
				}
			}
		}
	}

	/**
	 * Resets the last flush status
	 */
	public void resetRenderStatus() {
		hasRendered = false;
	}

	/**
	 * Gets the frustum intersection for camera culling
	 *
	 * @return The current frustum intersection
	 */
	public FrustumIntersection getFrustumIntersection() {
		if (intersection == null) {
			intersection = new FrustumIntersection(projection);
		}
		return intersection;
	}

	/**
	 * Uses the frustrum intersection to test if area is within the camera
	 *
	 * @param x1 First X position
	 * @param y1 First Y position
	 * @param x2 Second X position
	 * @param y2 Second Y position
	 * @return Whether or not area is within the camera
	 */
	public boolean isInCamera(float x1, float y1, float x2, float y2) {
		return getFrustumIntersection().testPlaneXY(x1, y1, x2, y2);
	}

	/**
	 * Checks if rotated rectangle is in the camera
	 *
	 * @param rr Rotated Rectangle to check
	 * @return Whether or not shape is within the camera
	 */
	public boolean isInCamera(RotatableRectangle rr) {
		return getFrustumIntersection().testPlaneXY(rr.getCenter().x - rr.getWidth(), rr.getCenter().y - rr.getHeight(),
				rr.getCenter().x + rr.getWidth(), rr.getCenter().y + rr.getHeight());
	}

	/**
	 * Returns the current camera width
	 *
	 * @return Current camera width
	 */
	public float getCurrentWidth() {
		return Game.getCurrent().getWidth() * currentZoom;
	}

	/**
	 * Returns the current camera height
	 *
	 * @return Current camera height
	 */
	public float getCurrentHeight() {
		return Game.getCurrent().getHeight() * currentZoom;
	}

	/**
	 * Whether or not a flush has occurred since the last buffer swap.
	 *
	 * @return
	 */
	public boolean hasRendered() {
		return hasRendered;
	}

	/**
	 * Returns whether or not the camera zoom is currently being used
	 *
	 * @return If zoom is in use
	 */
	public boolean isZoomed() {
		return useZoom;
	}

	/**
	 * Returns the current zoom value
	 *
	 * @return Current zoom
	 */
	public float getZoom() {
		return useZoom ? currentZoom : 1;
	}

	/**
	 * Returns the center of the camera
	 *
	 * @return Camera center as Vector3f
	 */
	public Vector3f getCameraCenter() {
		return trueCenter;
	}

	/**
	 * Returns the current number of sprite batches
	 *
	 * @return Current number of sprite batches
	 */
	public int getNumBatches() {
		return numBatches;
	}

	/**
	 * Returns the automatic batcher
	 *
	 * @return Automatic Batcher
	 */
	public AutoBatcher getAutoBatcher() {
		return batcher;
	}

	/**
	 * Returns the current font
	 *
	 * @return Current font
	 */
	public Font getFont() {
		return font;
	}

	public Matrix4f getProjection() {
		return projection;
	}

	/**
	 * Returns the vertex buffer
	 *
	 * @return The vertex buffer
	 */
	public ByteBuffer getVertices() {
		return vertices;
	}

	/**
	 * Returns the VAO
	 *
	 * @return VAO
	 */
	public VAO getVAO() {
		return vao;
	}

	/**
	 * Returns the VBO
	 *
	 * @return VBO
	 */
	public VBO getVBO() {
		return vbo;
	}

	/**
	 * Returns the EBO
	 *
	 * @return EBO
	 */
	public VBO getEBO() {
		return ebo;
	}

	/**
	 * Returns the shader program
	 *
	 * @return Shader Program
	 */
	public ShaderProgram getShaderProgram() {
		return program;
	}
}
