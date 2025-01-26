package com.botifier.becs.util;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursor;

import java.nio.DoubleBuffer;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import com.botifier.becs.Game;
import com.botifier.becs.graphics.Renderer;

/**
 * Input
 * 
 * TODO: Add more events
 * TODO: Make sure there isn't any memory leaks
 * 
 * @author Botifier
 */
public class Input {
	/**
	 * The window id
	 */
	private final long window;

	/**
	 * The key map
	 */
	private final ConcurrentHashMap<Integer, Integer> keys = new ConcurrentHashMap<>();

	/**
	 * The cursor icon map
	 */
	private final ConcurrentHashMap<String, Long> cursors = new ConcurrentHashMap<>();

	/**
	 * Mouse location
	 */
	private final Vector2f mouse;

	/**
	 * Raw mouse location
	 */
	private final Vector2f mouseRaw;

	/**
	 * X and Y buffers
	 */
	private final DoubleBuffer x, y;

	/**
	 * Last character typed
	 */
	private int lastChar = 0;
	
	private Object keyLock = null;

	/**
	 * Input constructor
	 *
	 * pre-added cursors:
	 * - basic : GLFW_CURSOR_NORMAL
	 * - beam  : GLFW_IBEAM_CURSOR
	 * - hand  : GLFW_HAND_CURSOR
	 *
	 * @param window long Window id
	 */
	public Input(long window) {
		this.window = window;
		this.mouse = new Vector2f(0, 0);
		this.mouseRaw = new Vector2f(0, 0);

		this.x = BufferUtils.createDoubleBuffer(1);
		this.y = BufferUtils.createDoubleBuffer(1);
		this.initMousePos();
		this.cursors.put("basic", GLFW.glfwCreateStandardCursor(GLFW.GLFW_CURSOR_NORMAL));
		this.cursors.put("beam", GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
		this.cursors.put("hand", GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
	}

	/**
	 * Initializes the mouse position
	 */
	private void initMousePos() {
		glfwPollEvents();
		glfwGetCursorPos(window, x, y);

		double pX = x.get(),
			   pY = y.get();

		mouse.set(pX, pY);
		mouseRaw.set(pX, pY);
	}

	/**
	 * Updates the mouse position
	 * @param pX double New x
	 * @param pY double New y
	 */
	public void updateMousePos(double pX, double pY) {
		mouse.set(pX, -pY);
		mouseRaw.set(pX, -pY+ Game.getCurrent().getHeight());


		Renderer r = Game.getCurrent().getRenderer();
		if (r != null) {
			mouse.add(0, Game.getCurrent().getHeight());
			mouse.mul(Game.getCurrent().getRenderer().getZoom());
		}
	}

	/**
	 * Destroys the input
	 */
	public void destroy() {
		MemoryUtil.memFree(x);
		MemoryUtil.memFree(y);

		purgeUnconsumedKeys();
	}

	/**
	 * Places a new cursor into the map
	 * @param name String name to use
	 * @param loc long Location of the cursor image
	 */
	public void putCursor(String name, long loc) {
		cursors.put(name.toLowerCase(), loc);
	}

	/**
	 * Sets the cursor to the specified one
	 * @param name String name of the cursor - case insensitive
	 */
	public void setCursor(String name) {
		glfwSetCursor(window, cursors.get(name.toLowerCase()));
	}

	/**
	 * Get the unmodified mouse position
	 * @return Vector2f Unmodded
	 */
	public Vector2f getMousePosUnmod() {
		return mouse;
	}

	/**
	 * Get the raw mouse position
	 * @return Vector2f Raw
	 */
	public Vector2f getRawMousePos() {
		return mouseRaw;
	}

	/**
	 * Get the current mouse position relative to the camera
	 * @return Vector2f Mouse
	 */
	public Vector2f getMousePos() {
		Vector2f m = new Vector2f(mouse);
		m.set(m.x-Game.getCurrent().getRenderer().getCameraCenter().x, m.y-Game.getCurrent().getRenderer().getCameraCenter().y);
		return m;
	}

	/**
	 * Get the current mouse position relative to the specified point
	 * @param v Vector2f To use
	 * @return Vector2f Mouse
	 */
	public Vector2f getRelativeMousePos(Vector2f v) {
		Vector2f m = new Vector2f(mouse);
		m.set(mouse.x+Game.getCurrent().getWidth()/2-v.x, mouse.y+Game.getCurrent().getHeight()/2+v.y);
		return m;
	}

	/**
	 * Checks if supplied key was pressed
	 * @param mouseCode int Mouse key code
	 * @return boolean Whether or not it was pressed
	 */
	public boolean isMousePressed(int mouseCode) {
		int state = glfwGetMouseButton(window, mouseCode);
		if (state == GLFW_PRESS) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if the supplied key was released
	 * @param mouseCode int Mouse key code
	 * @return boolean Whether or not it was released
	 */
	public boolean isMouseReleased(int mouseCode) {
		int state = glfwGetMouseButton(window, mouseCode);
		if (state == GLFW_RELEASE) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if the supplied key is held down
	 * @param keyCode int To check
	 * @return boolean Whether or not the key is down
	 */
	public boolean isKeyDown(int keyCode, Object... check) {
		if (keyLock != null && check != null && (check.length < 1 || !check[0].equals(keyLock)))
			return false;
		if (!keys.containsKey(keyCode)) {
			keyAction(keyCode, glfwGetKey(window, keyCode));
		}
		int state = keys.get(keyCode);
		if (state == GLFW_PRESS || state == GLFW_REPEAT) {
			lastChar = 0;
			return true;
		}
		return false;
	}

	/**
	 * Check if the supplied key was pressed
	 * @param keyCode int To check
	 * @return boolean Whether or not the key was pressed
	 */
	public boolean isKeyPressed(int keyCode, Object... check) {
		if (keyLock != null && check != null && (check.length < 1 || !check[0].equals(keyLock)))
			return false;
		if (!keys.containsKey(keyCode)) {
			keyAction(keyCode, glfwGetKey(window, keyCode));
		}
		int state = keys.get(keyCode);
		if (state == GLFW_PRESS) {
			keyAction(keyCode, GLFW_REPEAT);
			lastChar = 0;
			return true;
		}
		return false;
	}

	/**
	 * Check if the supplied key was released
	 * @param keyCode int To check
	 * @return boolean Whether or not the key was released
	 */
	public boolean isKeyReleased(int keyCode, Object... check) {
		if (keyLock != null && check != null && (check.length < 1 || !check[0].equals(keyLock)))
			return false;
		if (!keys.containsKey(keyCode)) {
			keyAction(keyCode, glfwGetKey(window, keyCode));
		}
		int state = keys.get(keyCode);
		if (state == GLFW_RELEASE) {
			keys.remove(keyCode);
			lastChar = 0;
			return true;
		}
		return false;
	}

	/**
	 * Adds an action to the key map
	 * @param keyCode int The key code
	 * @param action int The action
	 * @return int The same action
	 */
	public int keyAction(int keyCode, int action) {
		keys.put(keyCode, action);
		return action;
	}

	/**
	 * Sets the last character typed
	 * @param code int Char code
	 */
	public void setLastCharCode(int code) {
		this.lastChar = code;
	}

	/**
	 * Returns the last character typed
	 * resets it after
	 * @return char Last character typed
	 */
	public char getLastChar(Object... check) {
		if (keyLock != null && check != null && (check.length < 1 || !check[0].equals(keyLock)))
			return 0;
		char last = (char) lastChar;
		lastChar = 0;
		return last;
	}
	
	public void lockKeys(Object o) {
		if (this.keyLock == null)
			this.keyLock = o;
	}
	
	public void unlockKeys(Object o) {
		if (!o.equals(keyLock))
			return;
		this.keyLock = null;
	}

	/**
	 * Purges keys from the map and resets the last pressed character
	 */
	public void purgeUnconsumedKeys() {
		keys.clear();
		lastChar = 0;
	}

}
