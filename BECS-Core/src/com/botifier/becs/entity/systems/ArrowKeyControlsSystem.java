	package com.botifier.becs.entity.systems;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import com.botifier.becs.Game;
import com.botifier.becs.config.ControlsConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntityComponentManager;
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
    	super(g, "ArrowKeyControlled", "PhysicsEnabled", "Position", "Velocity");
    	ControlsConfig.addControl("UP", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP);
		ControlsConfig.addControl("DOwN", GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
		ControlsConfig.addControl("LEFT", GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
		ControlsConfig.addControl("RIGHT", GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);
		ControlsConfig.addControl("CONFIRM", GLFW.GLFW_KEY_SPACE);
    }

	@Override
	public void apply(Entity[] entities) {
		//Gets the Input
		Input in = Game.getCurrent().getInput();

		
		//Creates futures for all entities in entities 
		List<CompletableFuture<Void>> futures = IntStream.range(0, entities.length)
				.mapToObj(i -> CompletableFuture.runAsync(() -> update(entities[i], in)))
				.collect(Collectors.toList());

		//Creates a combined future
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		
		//Waits for the futures to complete
		allOf.join();
		
		keyTick.incrementAndGet();
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
		EntityComponent<Vector2f> positionComponent = e.getComponent("Position");
		EntityComponent<Vector2f> velocityComponent = e.getComponent("Velocity");

		Vector2f p = new Vector2f(positionComponent.get());
		//Gets the velocity of the entity
		Vector2f v = new Vector2f(velocityComponent.get());
		Vector2f toAdd = new Vector2f();


		//If the key-code(s) assigned to UP are held down
		if (ControlsConfig.down(i, "UP")) {
			toAdd.y += 1;
		}
		//If the key-code(s) assigned to DOWN are held down
		if (ControlsConfig.down(i, "DOWN")) {
			toAdd.y -= 1;
		}
		//If the key-code(s) assigned to LEFT are held down
		if (ControlsConfig.down(i, "LEFT")) {
			toAdd.x -= 1;
		}
		//If the key-code(s) assigned to RIGHT are held down
		if (ControlsConfig.down(i, "RIGHT")) {
			toAdd.x += 1;
		}

		if (toAdd.x != 0 || toAdd.y != 0) {
			//Calculates the angle at which the entity will move
			float angle = Math2.calcAngle(p, new Vector2f(toAdd).add(p));

			//Normalizes the movement
			toAdd.set(Math.cos(angle), Math.sin(angle));
		}

		//Modifies the movement based on Speed
		if (EntityComponentManager.hasComponent(e, "Speed")) {
			EntityComponent<Float> speedComponent =  e.getComponent("Speed");
			toAdd.mul(speedComponent.get());
		}
		//If toAdd has any movement update the velocity
		if (toAdd.length() > 0) {
			velocityComponent.set(v.add(toAdd));
		}
		
	}
	@Override
	public void destroy() {
		
	}
}
