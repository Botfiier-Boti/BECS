package com.botifier.becs.util.render;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector2f;
import org.joml.Vector3f;
import com.botifier.becs.Game;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.events.listeners.CameraListener;
import com.botifier.becs.graphics.FBO;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.shader.Shader;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.util.Math2;
import com.botifier.becs.util.ResourceManager;
import com.botifier.becs.util.SpatialEntityMap;
import com.botifier.becs.util.SpatialPolygonHolder;
import com.botifier.becs.util.shapes.RotatableRectangle;

/**
 * Camera class
 * @author Botifier
 */
public class Camera {
	
	/**
	 * Game that this camera exists in
	 */
	private Game game;
	
	/**
	 * Rectangle that represents the camera
	 */
	private RotatableRectangle camera;
	
	/**
	 * The map to query for entity culling
	 */
	private SpatialEntityMap sem = Entity.spatialMap();
	
	/**
	 * Polygon cache for entity querying
	 */
	private SpatialPolygonHolder cache = null;
	
	/**
	 * Target entity to follow
	 */
	private Entity target = null;
	
	/**
	 * Event listener tied to the followed entity 
	 */
	private CameraListener cl = null;
	
	/**
	 * Temporary storage for the original center of the camera
	 */
	private Vector3f oldCenter = null;
	
	/**
	 * Frame buffer that the camera is drawn to
	 */
	private FBO cameraBuffer = null;
	
	/**
	 * Whether or not the camera has resize or moved
	 */
	private boolean changed = false;
	
	
	/**
	 * Camera constructor
	 * @param g Game To use
	 * @param center Vector2f Center of the camera
	 * @param width float Width of the camera
	 * @param height float Height of the camera
	 */
	public Camera(Game g, Vector2f center, float width, float height) {
		this.game = g;
		this.camera = new RotatableRectangle(center, width, height);
		
		ShaderProgram shp = ResourceManager.getOrPutShaderProgram("CameraShaderProgram", s -> {
			ShaderProgram sp = new ShaderProgram();
			Shader v = ResourceManager.loadOrGetShader("CameraVertex", GL_VERTEX_SHADER, "framebuffer.vert");
			Shader f = ResourceManager.loadOrGetShader("CameraFragment", GL_FRAGMENT_SHADER, "basic_framebuffer.frag");
			
			sp.attachShader(v);
			sp.attachShader(f);
			sp.bindFragmentDataLocation(0, "fragColor");
			sp.link();
			return sp;
		});
		this.cameraBuffer = new FBO().init(shp);
		this.cameraBuffer.resize((int) width, (int) height);
	}
	
	
	/**
	 * Begin drawing to the camera's frame buffer
	 * @param r Renderer To use
	 */
	public void beginDrawing(Renderer r) {
		this.oldCenter = r.getCameraCenter();
		r.setCameraCenter(getCenter());
		this.cameraBuffer.bind();
		this.cameraBuffer.clearTexture();

	}
	
	/**
	 * Finish drawing to the camera's frame buffer
	 * @param r Renderer To use
	 */
	public void endDrawing(Renderer r) {
		this.cameraBuffer.unbind();
		
		this.cameraBuffer.draw(r);
		r.setCameraCenter(new Vector2f(oldCenter));
		oldCenter = null;
	}
	
	/**
	 * Draws entities within the camera along with anything that is in the auto batcher.
	 * @param r Renderer To use
	 */
	public void draw(Renderer r) {
		beginDrawing(r);
		queryVisible().forEach(e -> {
	  		e.draw(r);
		});
		r.getAutoBatcher().draw(r);
		endDrawing(r);
	}
	
	/**
	 * Queries the current SpatialEntityMap for visible entities
	 * @return Set\<Entity\> Valid entities 
	 */
	public Set<Entity> queryVisible() {
		if (this.sem == null) 
			return ConcurrentHashMap.newKeySet();
		
		if (this.cache == null || changed) {
			this.cache = this.getSpatialEntityMap().gridifyPolygon(camera.toPolygon());
			changed = false;
		}
		
		Set<Entity> entities = getSpatialEntityMap().getEntitiesIn(null, false, cache.getHashes());
		
		return entities;
	}
	
	/**
	 * Gets the currently used SpatialEntityMap
	 * @return SpatialEntityMap Current
	 */
	public SpatialEntityMap getSpatialEntityMap() {
		return this.sem;
	}
	
	/**
	 * Starts following the target entity
	 * Unfollows the last one if valid
	 * @param e Entity To follow
	 */
	public void setFollowEntity(Entity e) {
		if (e == null) {
			if (cl != null) {
				this.game.getEventManager().unregisterListener(cl);
				this.cl = null;
			}
			return;
		}
		if (!e.hasComponent("Position"))
			return;
		if (e != null && this.cl != null) {
			this.game.getEventManager().unregisterListener(cl);
			this.cl = null;
		}
		this.target = e;
		this.cl = new CameraListener(this, e.getUUID());
		this.game.getEventManager().registerListener(cl);
	}
	
	/**
	 * Sets the current SpatialEntityMap
	 * @param sem SpatialEntityMap To use
	 */
	public void setSpatialMap(SpatialEntityMap sem) {
		this.sem = sem;
	}
	
	/**
	 * Sets the camera's center
	 * @param center Vector2f The center
	 */
	public void setCenter(Vector2f center) {
		if (center == null)
			throw new IllegalArgumentException("Center cannot be null");
		if (!center.isFinite())
			throw new IllegalArgumentException("Center cannot be either NaN or Infinite");
		this.changed = true;
		this.camera.setCenter(center);
	}
	
	/**
	 * Sets the camera's width
	 * Cannot be zero or less
	 * @param width float To use
	 */
	public void setWidth(float width) {
		if (width <= 0)
			throw new IllegalArgumentException("Width cannot be zero or less");
		this.changed = true;
		this.camera.setWidth(width);

		this.cameraBuffer.resize((int) this.camera.getWidth(), (int) this.camera.getHeight());
	}
	
	/**
	 * Sets the camera's height
	 * Cannot be zero or less
	 * @param height float To use
	 */
	public void setHeight(float height) {
		if (height <= 0)
			throw new IllegalArgumentException("Height cannot be zero or less.");
		this.changed = true;
		this.camera.setHeight(height);
		this.cameraBuffer.resize((int) this.camera.getWidth(), (int) this.camera.getHeight());
	}
	
	/**
	 * Gets the entity that this camera is following
	 * @return Entity The target
	 */
	public Entity getFollowing() {
		return target;
	}
	
	/**
	 * Gets the center of the camera
	 * @return Vector2f The center of the camera
	 */
	public Vector2f getCenter() {
		return this.camera.getCenter();
	}
	
	/**
	 * Gets the RotatableRectangle that represents the camera
	 * @return RotatableRectangle The rectangle
	 */
	public RotatableRectangle getRectangle() {
		return camera;
	}
	
	/**
	 * Gets the camera's width
	 * @return float The width
	 */
	public float getWidth() {
		return this.camera.getWidth();
	}
	
	/**
	 * Gets the camera's height
	 * @return float The height
	 */
	public float getHeight() {
		return this.camera.getHeight();
	}
	
	/**
	 * Destroys the camera
	 */
	public void destroy() {
		if (cameraBuffer != null) {
			cameraBuffer.delete();
			cameraBuffer = null;
		}
	}
}
