package com.botifier.becs;

import org.lwjgl.opengl.GLCapabilities;

import com.botifier.becs.graphics.images.Image;

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
	 * Returns the window's GL capabilities
	 * @return GLCapabilites
	 */
	GLCapabilities getGLCapabilities();

}