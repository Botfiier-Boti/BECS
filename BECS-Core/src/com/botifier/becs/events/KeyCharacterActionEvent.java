package com.botifier.becs.events;

import com.botifier.becs.util.events.Event;

/**
 * KeyCharacterActionEvent
 * 
 * Executes whenever a key is typed
 */
public class KeyCharacterActionEvent extends Event {
	
	/**
	 * The GLFW key code
	 */
	private final int keyCode;
	
	/**
	 * KeyCharacterActionEvent constructor
	 * @param charCode The GLFW character typed 
	 */
	public KeyCharacterActionEvent(int charCode) {
		this.keyCode = charCode;
	}
	
	/**
	 * Returns the GLFW key code that was typed
	 * @return int The GLFW key code
	 */
	public int getKeyCode() {
		return this.keyCode;
	}
}
