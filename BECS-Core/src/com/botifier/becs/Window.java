package com.botifier.becs;

import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.GLCapabilities;

import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.Input;

public interface Window {

	/**
	 * Returns the window id
	 * @return long The window id
	 */
	long getId();

	/**
	 * Resizes the window
	 * @param width int New width
	 * @param height int New height
	 */
	void resize(int width, int height);

	/**
	 * Sets the window's icon
	 * @param i Image To use as a icon
	 */
	void setIcon(Image i);

	/**
	 * Updates the window title
	 * uses the thread executor as glfwSetWindowTitle locks
	 * @param title
	 */
	void setTitle(String title);

	/**
	 * Destroys the window
	 */
	void destroy();
	
	/**
	 * Uses the capabilities from this window
	 */
	void useCapabilities();
	
	/**
	 * Clears the window
	 */
	void clear();
	
	/**
	 * Get the window width
	 * @return int Width
	 */
	int getWidth();
	
	/**
	 * Get the window height
	 * @return int height
	 */
	int getHeight();

	/**
	 * Returns if the window is running
	 * 
	 * Implementation: should use an atomic access, or be volatile
	 * 
	 * @return boolean If the window is running
	 */
	boolean isRunning();
	
	/**
	 * Gets this windows input
	 * @return Input
	 */
	Input getInput();
	
	/**
	 * Gets this windows timer
	 * @return GameTimer
	 */
	GameTimer getTimer();

}