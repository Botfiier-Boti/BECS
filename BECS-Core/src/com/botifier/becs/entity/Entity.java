package com.botifier.becs.entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.joml.Vector2f;

import com.botifier.becs.Game;
import com.botifier.becs.events.EntityDeathEvent;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.SpatialEntityMap;
import com.botifier.becs.util.shapes.Shape;

/**
 * Based on the Entity Component System made by klavinski at https://github.com/klavinski/jecs
 * @author Botifier
 *
 */
public class Entity implements Comparable<Entity>, Cloneable{
	/**
	 * Map containing all entities and their UUIDs
	 */
	private static ConcurrentHashMap<UUID, Entity> entities = new ConcurrentHashMap<>();

	/**
	 * Spatial map of all entities
	 */
	private static SpatialEntityMap entitiesInSpace = new SpatialEntityMap(256);

	/**
	 * List of current components for easy access
	 * Won't contain components not added via addComponent()
	 */
	ConcurrentHashMap<String, EntityComponent<?>> components = new ConcurrentHashMap<>();
	
	/**
	 * Stores the initial values of added components
	 */
	ConcurrentHashMap<String, Object> initialComponentValues = new ConcurrentHashMap<>();

	/**
	 * Entity name
	 */
	private String name;

	/**
	 * Entity UUID
	 */
	private UUID uuid;

	/**
	 * Whether or not the entity is dead
	 */
	private boolean dead = false;

	/**
	 * Whether or not this entity's sprites should be automatically batched
	 */
	private boolean autoBatch = true;

	/**
	 * Whether or not the Entity is a ruse
	 */
	private boolean fake = false;

	/**
	 * Rendering layer
	 */
	private int renderingLayer = 0;

	/**
	 * Entity constructor for rebuilding, initializing and cloning Entities
	 * 
	 * @param name String Name of the Entity
	 * @param uuid UUID Entity's UUID
	 */
	private Entity(String name, UUID uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	/**
	 * Entity constructor
	 *
	 * Automatically adds entity to the map
	 *
	 * @param name Name of entity
	 */
	public Entity(String name) {
		this(name, UUID.randomUUID());
	}

	/**
	 * Entity initializer
	 * 
	 * Used when addEntity is run
	 */
	public void init() {}
	
	/**
	 * "Kills" the entity by marking them as dead and removing them from the entity map
	 */
	public void destroy() {
		dead = true;

		Game.getCurrent().getEventManager().executeEvent(new EntityDeathEvent(this.falseClone()));
		
		if (entities.contains(uuid))
			entities.remove(uuid);
		for (String s : components.keySet()) {
			removeComponent(s);
		}
		
	}

	/**
	 * Draws the entity
	 * @param r Renderer to use
	 */
	public void draw(Renderer r) {
		//If the entity doesn't have a position there is nowhere to draw anything
		if (!hasComponent("Position")) {
			return;
		}
		//Gets the position of this entity
		Vector2f pos = (Vector2f) getComponent("Position").get();
		//Gets the entity's image
		Image im = (Image) getComponent("Image").get();
		//Sets the color to white
		Color c = Color.white;
		//Changes the color if there is an Color component
		if (hasComponent("Color")) {
			c = (Color) getComponent("Color").get();
		}

		//Use the collision shape as a base to draw the image if it exists
		if (hasComponent("CollisionShape")) {
			Shape s = (Shape) getComponent("CollisionShape").get();
			//Use the AutoBatcher for rendering if enabled
			if (autoBatch) {
				r.getAutoBatcher().add(im, s, c);
				return;
			}
			if (im != null) { //If the image exists
				r.begin(im.getShaderProgram());
				s.drawImage(r, im, c, false);
				r.end();
			} else { //If the image exists
				s.draw(r, c);
			}

		} else {
			//Return if there is no image
			if (im == null) {
				return;
			}
			//Use the AutoBatcher for rendering if enabled
			if (autoBatch) {
				r.getAutoBatcher().add(im, pos.x, pos.y, c);
			}  else {
				r.begin(); //Begin drawing
				im.draw(r, pos.x, pos.y, renderingLayer, c);
				r.end(); //Finish drawing
			}

		}

	}

	/**
	 * Sets the entitie's name
	 * @param name To set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the entitie's rendering layer
	 * @param layer To set
	 */
	public void setRenderingLayer(int layer) {
		this.renderingLayer = layer;
	}

	/**
	 * Sets whether or not this entitie's images are automatically batched
	 * @param autoBatch Automatic Batching enabled/disabled
	 */
	public void setAutoBatch(boolean autoBatch) {
		this.autoBatch = autoBatch;
	}

	/**
	 * Adds specified component to component list
	 * @param \<T\> Type of component
	 * @param e Component to add
	 * @param value value to put
	 */
	public EntityComponent<?> addComponent(String componentName, Object value) {
		return EntityComponentManager.giveComponent(this, componentName, value);
	}
	
	/**
	 * Updates a component or adds it if it doesn't exist
	 * @param componentName String Name of the component
	 * @param value Object value stored within the component
	 */
	public void updateOrAddComponent(String componentName, Object value) {
		if (components.contains(componentName.toLowerCase())) {
			EntityComponent<Object> ec = getComponent(componentName);
			ec.set(value);
			return;
		}
		
		addComponent(componentName, value);
	}
	
	/**
	 * Removes a component from this entity
	 * @param componentName String Component to removed
	 * @return EntityComponent\<?\> Removed component
	 */
	public EntityComponent<?> removeComponent(String componentName) {
		return EntityComponentManager.removeComponent(this, componentName);
	}

	@SuppressWarnings("unchecked")
	public <T> EntityComponent<T> getComponent(String name) {
		return (EntityComponent<T>) components.get(name.toLowerCase());
	}

	/**
	 * Returns the name of the entity
	 * @return Entity Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns whether or not automatic batching is enabled
	 * @return Automatic Batching enabled/disabled
	 */
	public boolean isAutoBatched() {
		return autoBatch;
	}

	/**
	 * Returns the dead status of the entity
	 * @return Whether it is dead or not
	 */
	public boolean isDead() {
		return dead;
	}

	/**
	 * Checks whether or not the entity current has a component
	 * @param name String Name of the component
	 * @return boolean Whether or not the entity has the specified component
	 */
	public boolean hasComponent(String name) {
		return components.containsKey(name.toLowerCase());
	}

	/**
	 * Returns the rendering layer of the entity.
	 * Used for sorting
	 * @return int The entity's render layer
	 */
	public int getRenderingLayer() {
		return renderingLayer;
	}

	/**
	 * Returns this entitie's UUID and creates one if it doesn't have one
	 * Should probably be concerned it it didn't already have one after creation
	 * @return Entitie's UUID
	 */
	public UUID getUUID() {
		if (uuid == null) { //Grant a random UUID if the entity doesn't have one
			uuid = UUID.randomUUID();
		}
		return uuid;
	}

	/**
	 * Returns the entitie's component list
	 * @return List of components
	 */
	public ArrayList<EntityComponent<?>> getComponents() {
		return new ArrayList<>(components.values());
	}

	/**
	 * Destroys specified entity
	 * @param e To destroy
	 */
	public static void remove(Entity e) {
		e.destroy();
	}

	/**
	 * Gets an Entity by it's UUID
	 * @param u UUID To use
	 * @return Entity that belongs to the specified UUID; null if it doesn't exist
	 */
	public static Entity getEntity(UUID u) {
		return entities.getOrDefault(u, null);
	}

	/**
	 * Returns the entity map as an array list
	 * @return
	 */
	public static HashSet<Entity> getEntities() {
		//Creates an ArrayList using the Entity map
		return new HashSet<>(entities.values());
	}

	/**
	 * Get all entities as fake copies
	 * @return Set\<Entity\> fake versions of all entities
	 */
	public static Set<Entity> getFalseEntities() {
		Set<Entity> en = entities.values().stream()
											.map(k -> k.falseClone())
											.collect(Collectors.toSet());

		return en;
	}

	/**
	 * Destroys all entities
	 */
	public static void clear() {
		Set<Entity> toDestroy = getEntities();

		for (Entity e : toDestroy) {
			e.destroy();
		}
	}

	/**
	 * Returns the spatial map of entities
	 * @return SpatialEntityMap The map
	 */
	public static SpatialEntityMap spatialMap() {
		return entitiesInSpace;
	}

	/**
	 * Literally just clear()
	 */
	public static void reset() {
		entities.clear();
	}

	/**
	 * Clones the entity
	 */
	public Entity falseClone() {
		Entity clone = new Entity(this.name, this.uuid);
		boolean collision = false;
		for (Map.Entry<String, EntityComponent<?>> entry : this.components.entrySet()) {
	        String key = entry.getKey().toLowerCase();
	        EntityComponent<?> ec = entry.getValue().clone();
	        clone.components.put(key, ec);

	        if (!collision && ec.getName().equalsIgnoreCase("CollisionShape")) {
	            collision = true;
	        }
	    }

		if (collision && hasComponent("Position")) {
			EntityComponent<Shape> sC = getComponent("CollisionShape");
			EntityComponent<Vector2f> pC = getComponent("Position");
			Vector2f p = pC.get();

			sC.get().setCenter(p.x, p.y);
		}
		clone.fake = true;
		return clone;
	}

	/**
	 * Is the entity real?
	 * @return Boolean maybe
	 */
	public boolean isReal() {
		return fake;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fake, name, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		
		return fake == other.fake && Objects.equals(name, other.name)
				&& Objects.equals(uuid, other.uuid);
	}

	@Override
	public int compareTo(Entity e) {
		return renderingLayer < e.renderingLayer ? 1 : renderingLayer > e.renderingLayer ? -1 : 0; //Sorts entities based on their rendering layer.
	}
	
	/**
	 * Adds an Entity to the entity map
	 * @param e Entity To add
	 */
	public static void addEntity(Entity e) {
		if (!entities.contains(e.getUUID())) {
			e.init();
			entities.put(e.getUUID(), e);
		}
	}

}
