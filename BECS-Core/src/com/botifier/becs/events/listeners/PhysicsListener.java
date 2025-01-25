package com.botifier.becs.events.listeners;



import com.botifier.becs.events.*;
import com.botifier.becs.util.annotations.EventHandler;
import com.botifier.becs.util.events.EventListener;

public class PhysicsListener extends EventListener {

	
	@EventHandler(event = VelocityChangeEvent.class)
	public void onVelocityChanged(VelocityChangeEvent e) {
		
	}
	
}
