package com.botifier.becs.events;

import com.botifier.becs.util.events.Event;

public class KeyCharacterActionEvent extends Event {
	
	private final int keyCode;
	
	
	public KeyCharacterActionEvent(int charCode) {
		this.keyCode = charCode;
	}
	
	public int getKeyCode() {
		return this.keyCode;
	}
}
