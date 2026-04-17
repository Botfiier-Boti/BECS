package com.botifier.becs.entity.systems;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.lwjgl.glfw.GLFW;

import com.botifier.becs.Game;
import com.botifier.becs.config.ControlsConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntitySystem;
import com.botifier.becs.util.Input;
import com.botifier.becs.util.Math2;

/**
 * A basic arrow key controller
 * 
 * Entities require Position, Velocity, ArrowKeyControlled, and PhysicsEnabled components
 * 
 * @author Botifier
 */
public class ArrowKeyControlsSystem extends EntitySystem {
	
	public AtomicLong keyTick = new AtomicLong(0);
	
	private final static String UP = "up";
	private final static String DOWN = "down";
	private final static String LEFT = "left";
	private final static String RIGHT = "right";
	
	private volatile Vector2fc velocity;
	
	
    /**
     * ArrowKeyControlsSystem constructor
     * Automatically adds WASD, Arrow keys and Space bar to ControlsConfig
     * UP - W, Up key
     * DOWN - S, Down key
     * LEFT - A, Left key
     * RIGHT - D, Right key
     * CONFIRM - Space bar
     */
    public ArrowKeyControlsSystem(Game g) {
    	super(g, "ArrowKeyControlled", "PhysicsEnabled", "Velocity");
    	ControlsConfig.addControl(UP, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP);
		ControlsConfig.addControl(DOWN, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
		ControlsConfig.addControl(LEFT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
		ControlsConfig.addControl(RIGHT, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);
		ControlsConfig.addControl("CONFIRM", GLFW.GLFW_KEY_SPACE);
    }

	@Override
	public CompletableFuture<Void> apply(Entity[] entities) {
		//Gets the Input
		Input in = Game.getCurrent().getInput();
		
		final boolean UP_DOWN = ControlsConfig.downRaw(in, UP);
		final boolean DOWN_DOWN = ControlsConfig.downRaw(in, DOWN);
		final boolean LEFT_DOWN = ControlsConfig.downRaw(in, LEFT);
		final boolean RIGHT_DOWN = ControlsConfig.downRaw(in, RIGHT);
		
		
		Vector2f toAdd = new Vector2f();

		//If the key-code(s) assigned to UP are held down
		if (UP_DOWN) {
			toAdd.y += 1;
		}
		//If the key-code(s) assigned to DOWN are held down
		if (DOWN_DOWN) {
			toAdd.y -= 1;
		}
		//If the key-code(s) assigned to LEFT are held down
		if (LEFT_DOWN) {
			toAdd.x -= 1;
		}
		//If the key-code(s) assigned to RIGHT are held down
		if (RIGHT_DOWN) {
			toAdd.x += 1;
		}
		
		if (toAdd.x != 0 || toAdd.y != 0) {
			//Calculates the angle at which the entity will move
			float angle = Math2.calcAngle(new Vector2f(0), toAdd);

			//Normalizes the movement
			toAdd.set(Math.cos(angle), Math.sin(angle));
		}
		
		velocity = toAdd;
		
		final int BATCH_SIZE = 500; // Adjust based on your performance needs
		
		List<Entity> entityList = List.of(entities);
		
		//Creates futures for all entities in entities 
		List<CompletableFuture<Void>> futures = IntStream.range(0, (entityList.size() + BATCH_SIZE - 1) / BATCH_SIZE)
		        .mapToObj(i -> {
		            int startIndex = i * BATCH_SIZE;
		            int endIndex = Math.min(startIndex + BATCH_SIZE, entityList.size());
		            return entityList.subList(startIndex, endIndex);
		        })
				.map(batch -> CompletableFuture.runAsync(() -> {
					for (Entity e : batch)
						update(e, in);
				}))
				.collect(Collectors.toList());

		//Creates a combined future
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		
		keyTick.incrementAndGet();
		return allOf;
	}

	/**
	 * Performs functionality on supplied entity
	 * @param e Entity To use
	 * @param i Input Input being used
	 */
	private void update(Entity e, Input i) {
		EntityComponent<Boolean> arrow = e.getComponent("ArrowKeyControlled");
		if (!arrow.get()) { //Continue if ArrowKeyControlled is set to false
			return;
		}
		EntityComponent<Vector2f> velocityComponent = e.getComponent("Velocity");

		//Gets the velocity of the entity
		Vector2f v = velocityComponent.get();
		
		Vector2f toAdd = new Vector2f(velocity);

		//Modifies the movement based on Speed
		if (e.hasComponent("Speed")) {
			EntityComponent<Float> speedComponent =  e.getComponent("Speed");
			toAdd.mul(speedComponent.get());
		}
		//If toAdd has any movement update the velocity
		if (toAdd.length() > 0) {
			Vector2f hold = new Vector2f(v).add(toAdd);
			velocityComponent.set(hold);
		}
		
	}
	@Override
	public void destroy() {
		
	}
}
