package com.botifier.becs.tests;

import com.botifier.becs.Game;
import com.botifier.becs.events.listeners.PhysicsListener;

public class WindowTest {

	public static void main(String[] args) {
		HelloWorld hw = new HelloWorld();
		//Game.setDebug(true);
		if (args.length > 0 && args[0].equalsIgnoreCase("-debug")) {
			Game.setDebug(true);
		}

		hw.run();

		//EmptyWorld ew = new EmptyWorld();

		//ew.run();

		//CollisionTest ct = new CollisionTest();
		//ct.run();
	}

}
