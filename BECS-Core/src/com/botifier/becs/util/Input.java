package com.botifier.becs.util;

import org.joml.Vector2f;

public interface Input {

	/**
	 * Initializes the Input 
	 */
	Input init();
	
	/**
	 * Updates the mouse position
	 * @param pX double New x
	 * @param pY double New y
	 */
	void updateMousePos(double pX, double pY);

	/**
	 * Destroys the input
	 */
	void destroy();

	/**
	 * Places a new cursor into the map
	 * @param name String name to use
	 * @param loc long Location of the cursor image
	 */
	void putCursor(String name, long loc);

	/**
	 * Sets the cursor to the specified one
	 * @param name String name of the cursor - case insensitive
	 */
	void setCursor(String name);

	/**
	 * Get the unmodified mouse position
	 * @return Vector2f Unmodded
	 */
	Vector2f getMousePosUnmod();

	/**
	 * Get the raw mouse position
	 * @return Vector2f Raw
	 */
	Vector2f getRawMousePos();

	/**
	 * Get the current mouse position relative to the camera
	 * @return Vector2f Mouse
	 */
	Vector2f getMousePos();

	/**
	 * Get the current mouse position relative to the specified point
	 * @param v Vector2f To use
	 * @return Vector2f Mouse
	 */
	Vector2f getRelativeMousePos(Vector2f v);

	/**
	 * Checks if supplied key was pressed
	 * @param mouseCode int Mouse key code
	 * @return boolean Whether or not it was pressed
	 */
	boolean isMousePressed(int mouseCode);

	/**
	 * Checks if the supplied key was released
	 * @param mouseCode int Mouse key code
	 * @return boolean Whether or not it was released
	 */
	boolean isMouseReleased(int mouseCode);

	/**
	 * Checks if the supplied key is held down
	 * @param keyCode int To check
	 * @return boolean Whether or not the key is down
	 */
	boolean isKeyDown(int keyCode, Object... check);

	/**
	 * Check if the supplied key was pressed
	 * @param keyCode int To check
	 * @return boolean Whether or not the key was pressed
	 */
	boolean isKeyPressed(int keyCode, Object... check);

	/**
	 * Check if the supplied key was released
	 * @param keyCode int To check
	 * @return boolean Whether or not the key was released
	 */
	boolean isKeyReleased(int keyCode, Object... check);

	/**
	 * Adds an action to the key map
	 * @param keyCode int The key code
	 * @param action int The action
	 * @return int The same action
	 */
	int keyAction(int keyCode, int action);

	/**
	 * Sets the last character typed
	 * @param code int Char code
	 */
	void setLastCharCode(int code);

	/**
	 * Returns the last character typed
	 * resets it after
	 * @return char Last character typed
	 */
	char getLastChar(Object... check);

	void lockKeys(Object o);

	void unlockKeys(Object o);

	/**
	 * Purges keys from the map and resets the last pressed character
	 */
	void purgeUnconsumedKeys();

}