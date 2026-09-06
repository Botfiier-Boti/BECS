package com.botifier.becs;

public interface GameTimer {

	/**
	 * Initializes the timer
	 */
	GameTimer init();

	/**
	 * Gets the current time from GLFW
	 * @return
	 */
	double getTime();

	/**
	 * Updates the amount of time since the last call of getDelta
	 * @return Time since last call
	 */
	float getDelta();

	/**
	 * Ups the fpsCount variable
	 */
	void updateFPS();

	/**
	 * Ups the upsCount variable
	 */
	void updateUPS();

	/**
	 * Sets the current FPS and UPS every second
	 */
	void update();

	/**
	 * Returns the current FPS
	 * @return The current FPS
	 */
	int getFPS();

	/**
	 * Returns the current UPS
	 * @return The current UPS
	 */
	int getUPS();

	/**
	 * Gets the time since the last loop
	 * @return Last loop time
	 */
	double getLastLoopTime();

}