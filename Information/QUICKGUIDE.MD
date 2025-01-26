# BECS v0.0.1-SNAPSHOT

## BECS Quick Reference
### Game
#### Disabling automatic autobatcher execution
**WARNING: Any entity with autobatching enabled will not draw if this is disabled unless getRenderer().getAutoBatcher().draw(getRenderer()) is called during rendering**
```java
game.setBatchAutoDraw(false);
```
#### Get the current running game from anywhere
```java
Game.getCurrent();
```
#### Get the event manager
```java
game.getEventManager();
```
#### Get the sound manager
```java
game.getSoundManager();
```
#### Get the renderer
```java
game.getRenderer();
```
#### Set the window icon
```java
//com.botifier.becs.Image
game.setIcon(image);
```
#### Set the window title
```java
game.setTitle("title");
```
#### Get the GLCapabilities
```java
game.getWindow().getGLCapabilities();
```
---
### Entity
#### Creating an entity
```java
Entity e = new Entity("name");
```
#### Adding the entity into the world
```java
Entity.addEntity(entity);
```
#### Destroying an entity
```java
entity.destroy();
//OR
Entity.destroy(entity);
```
#### Changing entity render priority
```java
entity.setRenderingLayer(1);
```
#### Enabling auto batching
```java
entity.setAutoBatch(true);
```
---
### EntityComponent
#### Creating a component
**NOTE: Java destroys type information, remember that components utilize generics when creating and using components with classes that utilize generics like List**
```java
EntityComponentManager.createComponent("Children", ArrayList.class);
```
#### Adding a component to an entity
```java
public class EntitySubclass extends Entity {
	
    public EntitySubclass() {
    	super("Subclass");
        addComponent("CollisionShape", new RotatableRectangle(0, 0, 10, 10));
    }

	@Override
    public void init() {
    	//Good for creating new entity classes
    	addComponent("Position", new Vector2f(0, 0);
    }
}

entity.addComponent("Velocity", new Vector2f(1, 1));

Entity e = new Entity("Dave") {
	@Override
    public void init() {
    	addComponent("PhysicsEnabled", new PhysicsListener(getUUID()));
    }
}
```
#### Checking if an entity has a component
```java
EntityComponentManager.hasComponent(entity, "Component");
//OR
entity.hasComponent("Component");

```
#### Grabbing data from a component from an entity
```java
EntityComponent<Vector2f> velComp = entity.getComponent("Velocity");
Vector2f v = velComp.get();
//OR
Vector2f v = (Vector2f) entity.getComponent("Velocity").get();
//OR
Vector2f v = entity.<Vector2f>getComponent("Velocity").get();
```
#### Grabbing all entities with a specific component
```java
Set<Entity> entities = EntityComponentManager.getEntitiesWithComponent("Component");
```
#### Getting a component class type in code
```java
Class<?> clazz = EntityComponentManager.getComponentDataType("Component");
```
#### Updating the value in a component
**NOTE: Treat component data as immutable if you want to use the EntityComponentUpdatedEvent**
```java
//If immutable or if that doesn't matter
component.set(data);
//If not immutable and it matters, clone or do something like component.set(new DataType(data));
```
#### Grabbing the component's owner
```java
Entity owner = component.getOwner();
UUID ownerUUID = component.getOwnerUUID();
```
---
### Entity System
#### Adding a system
```java
game.addSystem(new CustomEntitySystem(game, ...));
```
#### Getting valid entity targets
```java
system.getValidEntities();
```
#### Pausing supported systems
```java
system.pause();
```
#### Getting a list of the require components
```java
system.getRequiredComponents();
```
#### Getting the game from inside an entity system
```java
Game game = getGame();
```
#### Creating a new entity system
```java
public class CustomSystem extends EntitySystem {
	public CustomerSystem(Game g) {
    	super(g, "RequiredComponent1", "RequiredComponent2");
    }
    
    @Override
    public void apply(Entity[] entities) {
    	//Code here
    }
}
```
#### Physics system specifics
##### Creating a physics system extension
```java
public class Extension extends PhysicsSystemExtension {
	public Extension(PhysicsSystem origin) {
    	super(origin);
    }
    
    @Override
    public void preTick(Entity e, List<Entity> moved) {
    	//code here
    }
    
    @Override
    public void postTick(Entity e, List<Entity> moved) {
    	//code here
    }
}
```
##### Components required for collision
```
Position, Velocity, PhysicsEnabled, CollisionShape, Collidable
If others collide with it: Solid
```
---
### Events
#### Getting the event manager from anywhere
```java
EventManager em = Game.getCurrent().getEventManager();
```
#### Creating a new event type
```java
public class SomethingEvent extends Event {
	private final int value;
    
    public SomethingEvent(int value) {
    	this.value = value;
    }
    
    public int getValue() {
    	return value;
    }
}
```
#### Executing an event with no target or origin
```java
eventManager.executeEvent(new Event(...));
```
#### Executing event targeting the world listener alone
```java
eventManager.executeEventOn(new Event(...), "origin", true, null);
```
#### Executing an event targeting a specific owner
```java
eventManager.executeEventOn(new BobEvent(...), "Bobbening", false, entityWithListener.getUUID());
```
#### Creating a new event listener
```java
public class BobListener extends EventListener {
	@EventHandler(event = SomethingEvent.class)
    public void onSomething(SomethingEvent e) {
    	//Code
    }
}
```
#### Listening for events from a specific origin
```java
public class BobListener extends EventListener {
	@EventHandler(event = EntityComponentUpdatedEvent.class, origin="Velocity")
    public void onSomething(EntityComponentUpdatedEvent<Vector2f> e) {
    	//Code
    }
    
    @EventHandler(event = EntityComponentUpdatedEvent.class, origin="Position")
    public void onSomething(EntityComponentUpdatedEvent<Vector2f> e) {
    	//Code
    }
    
    @EventHandler(event = EntityComponentUpdatedEvent.class, origin="CollisionShape")
    public void onSomething(EntityComponentUpdatedEvent<Shape> e) {
    	//Code
    }
}
```
#### Instantiating and registering an event listener with an entity owner
```java
Entity bob = new Bob();
BobListener bl = new BobListener(bob.getUUID());
eventManager.registerListener(bl);
```
#### Instantiating and registering an event listener with a custom owner id
```java
BobListener bl = new BobListener(new UUID(0,2));
eventManager.registerListener(bl);
```
#### Instantiating and registering an event listener with the global owner
```java
BobListener bl = new BobListener(new UUID(1,0)); // OR just new BobListener();
eventManager.registerListener(bl);
```
---
### Rendering
#### Changing the zoom
```java
//Smaller numbers bring the camera closer
renderer.setZoom(2);
```
#### Setting the camera center
```java
renderer.setCameraCenter(new Vector2f(x, y));
```
#### Reset the zoom for the frame
```java
render.tempResetZoom();
```
#### Reset the camera origin for the frame
```java
render.tempResetCenter();
```
#### Creating a frame buffer object
```java
//Shaders are grabbed internally
FBO fbo = new FBO(vertexLocation, fragmentLocation).init();
```
#### Rendering to a frame buffer object
```java
fbo.bind();
//Do rendering
fbo.unbind();
fbo.draw();
```
#### Safely destroying a frame buffer object
```java
//Game code

@Override
public void exit() {
	fbo.delete();
}
```
#### Frame buffer fragment color name
```
fragColor
```
#### Frame buffer sampler2D name
```
fbo_texture
```
#### Frame buffer vertex inputs
```
vec2 aPos;
vec2 aTexCoords;
```