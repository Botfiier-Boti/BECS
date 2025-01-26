# BECS v0.0.1-SNAPSHOT
## Setting up a game environment
Start by creating a new class
```java
public class MainGame extends Game {

	/**
    * Game constructor
    * 
    * Title, width, height, vsync, resizable 
    **/
	public MainGame() {
		super("My Game", 480, 480, false, false);
	}
    
    @Override
    public void init() {
    	//Run at initialization
    }
    
    @Override
    public void update() {
    	//Runs at a fixed rate
    }
    
   @Override
    public void draw(Renderer r, WorldState ws, RotatableRectangle cameraRectangle, float alpha) {
    	//Runs as fast as it can
    }
     
    @Override
    public void exit() { 
    	//Run before the game closes
    }
	
    
    public static void main(String[] args) {
    	//Creates a new Game 
        MainGame mg = new MainGame();
        //Runs the game
    	mg.run();   
    }
}
```
Now that we have a game setup, let us add some systems! 
```java
/*Store globally if you want to perform actions based on physics ticks, like if you want something to 
* Do something every other tick: 
* if (physics.GetPhysicsTick() % 2 == 0) doSomething();
*/
private PhysicsSystem physics;
private ArrowKeyControlsSystem akcs;

@Override
public void init() {
	physics = new PhysicsSystem(this);
    akcs = new ArrowKeyControlsSystem(this);
    
    addSystem(physics);
    addSystem(akcs);
}

```
Systems are nothing without entities, lets create a Player class.
```java
public class Player extends Entity{
	/**Entities are minimalist and require components to function
    *  As a result they don't naturally take any information other than a name to create
    *  If you want to pass extra information into a constructor, you have to create variables for it yourself
    **/
	protected Vector2f startingPos;

	/**
    * Entities require a name which is set in super(name)
    **/
	public TestPlayer(float x, float y) {
    	super("Player");
    }
    
    @Override
    public void init() {
    	//This is called whenever an entity is added to the world using Entity.addEntity()
        
        //Images are loaded internally, so make sure any images you use are on the classpath
        Image i = new Image("christmasfroggy.png");
        i.setScale(0.25f);
        i.setZ(1);
        
        //Components are added via addComponent(name, data) with name being case-insensitive
       	addComponent("Position", new Vector2f(startingPos)); 
        addComponent("Velocity", new Vector2f());
        addComponent("Image", i);
        addComponent("PhysicsEnabled", new PhysicsListener(getUUID()));
       	addComponent("ArrowKeyControlled", true);
        addComponent("Speed", 10f);
    }
}
```
NOTE: When creating entities keep in mind what systems you want to use them, for example if you want the PhysicsSystem to function on an entity you need at a minimum the **Position, Velocity,** and **PhysicsEnabled** components. Systems can also have optional functionality with components, like if you want collision to function the **Collidable, Solid** and **CollisionShape** components will be needed as well.
<br><br>
Now that we have the player setup we can add it to the world
```java
Player p;

@Override
public void init() {
	physics = new PhysicsSystem(this);
    akcs = new ArrowKeyControlsSystem(this);
    
    addSystem(physics);
    addSystem(akcs);
    
    p = new Player(0, 0);
    Entity.addEntity(p);
}
```
Simpily adding the entity does not make it render though, here is how we can get the little guy to show up on screen
```java
@Override
public void draw(Renderer r, WorldState ws, RotatableRectangle cameraRectangle, float alpha) {
	//Entities have a built in draw function that changes functionality based on what components the entity has
    p.draw(r);
}
```
Now that the player is in the world and can move around, lets get the camera to follow him
```java
@Override
public void draw(Renderer r, WorldState ws, RotatableRectangle cameraRectangle, float alpha) {
	if (player != null) {
    	//Checking if an entity has a component before grabbing it is a good practice
        if (player.hasComponent("Position")) {
        	/**EntityComponents use the generic T and should be grabbed in this formatting
            *  EntityComponent<Type> comp = entity.getComponent("Name");
            *  Type should be related to what is stored in the component
            *  For example the position component can be safely grabbed as Vector2f, Vector2fc, or Object
            *  You can also do entity.getComponent("Comp").get(); but you would have to cast the result of get()
            **/
        	EntityComponent<Vector2f> posComp = player.getComponent("Position"):
            
            Vector2f v = posComp.get();
            
            r.setCameraCenter(v);
        }
    }

    p.draw(r);
}
```
NOTE: In a future version, the cameraRectangle may be replaced for easier use.
<BR><BR>
Now that you have the basic setup, go out and create!