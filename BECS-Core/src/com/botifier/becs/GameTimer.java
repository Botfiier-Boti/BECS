package com.botifier.becs;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

import java.util.concurrent.atomic.AtomicInteger;
/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2014-2015, Heiko Brumme
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class GameTimer {

    /**
     * The last time getDelta was called
     */
    private double lastLoop;

    /**
     * The amount of time since last update
     * Used to determine UPS and FPS
     */
    private float timeCount;

    /**
     * The current FPS
     */
    private final AtomicInteger fps = new AtomicInteger(0);

    /**
     * The current UPS
     */
    private final AtomicInteger ups = new AtomicInteger(0);

    /**
     * Counter used to determine the current FPS
     */
    private final AtomicInteger fpsCount = new AtomicInteger(0);

    /**
     * Counter used to determine the current UPS
     */
    private final AtomicInteger upsCount = new AtomicInteger(0);

    /**
     * Initializes the timer
     */
    public void init() {
        lastLoop = getTime();
    }

    /**
     * Gets the current time from GLFW
     * @return
     */
    public double getTime() {
        return glfwGetTime();
    }

    /**
     * Updates the amount of time since the last call of getDelta
     * @return Time since last call
     */
    public float getDelta() {
        double time = getTime();
        float delta = (float) (time - lastLoop);
        lastLoop = time;
        timeCount += delta;
        return delta;
    }

    /**
     * Ups the fpsCount variable
     */
    public void updateFPS() {
    	fpsCount.incrementAndGet();
    }

    /**
     * Ups the upsCount variable
     */
    public void updateUPS() {
    	upsCount.getAndIncrement();
    }

    /**
     * Sets the current FPS and UPS every second
     */
    public void update() {
        if (timeCount > 1f) {
            fps.set(fpsCount.get());
            fpsCount.set(0);

            ups.set(upsCount.get());
            upsCount.set(0);

            timeCount--;
        }
    }

    /**
     * Returns the current FPS
     * @return The current FPS
     */
    public int getFPS() {
    	int fps = this.fps.get();
        return fps > 0 ? fps : fpsCount.get();
    }

    /**
     * Returns the current UPS
     * @return The current UPS
     */
    public int getUPS() {
    	int ups = this.ups.get();
        return ups > 0 ? ups : upsCount.get();
    }

    /**
     * Gets the time since the last loop
     * @return Last loop time
     */
    public double getLastLoopTime() {
        return lastLoop;
    }

}
