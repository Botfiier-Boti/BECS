package com.botifier.becs;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.botifier.becs.graphics.images.Image;

/**
 * Window
 * 
 * @author Botifier
 */
public class Window {
	/**
	 * Thread pool
	 */
	final ExecutorService threadExecutors = Executors.newCachedThreadPool();
	/**
	 * The GLCapabilities of this window
	 */
	private GLCapabilities glc;
	/**
	 * The window id
	 */
	private long windowId;
	/**
	 * Width and height of the window
	 */
	private int width, height;
	/**
	 * Title of the window
	 */
	private String title;
	/**
	 * Whether or not to use VSync
	 */
	private boolean vsync;
	/**
	 * The window's icon
	 */
	private Image icon;

	/**
	 * Window constructor
	 * @param title String Title of the window
	 * @param width int Width of the window
	 * @param height int Height of the window
	 * @param resizable boolean Whether or not the window is resizable
	 * @param vsync boolean Whether or not to use VSync
	 */
	public Window(String title, int width, int height, boolean resizable, boolean vsync) {
		this.title = title;
		this.width = width;
		this.height = height;
		this.vsync = vsync;
		init(resizable ? GLFW_TRUE : GLFW_FALSE);
	}

	/**
	 * Initializes the window
	 * @param resizable boolean Whether or not the window can be resized
	 */
	private void init(int resizable) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, resizable);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

		//Creates the window
		windowId = glfwCreateWindow(width, height, title, NULL, NULL);
		//Throw an exception if it cannot be created
		if (windowId == NULL) {
			throw new RuntimeException("Failed to create the GLFW Window"); 
		}

		//Pulls window size data and centers the window on screen
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);

			glfwGetWindowSize(windowId, pWidth, pHeight);

			GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			glfwSetWindowPos(
					windowId,
					(vidMode.width() - pWidth.get(0)) / 2,
					(vidMode.height() - pHeight.get(0)) / 2
			);
		}
		//Uses this window for context
		glfwMakeContextCurrent(windowId);
		//Creates the window's GL capabilities
		glc = GL.createCapabilities();

		//Sets VSync
		if (vsync) {
			glfwSwapInterval(1);
		} else {
			glfwSwapInterval(0);
		}
		//Displays the window on screen
		glfwShowWindow(windowId);
		//Drops the context
		glfwMakeContextCurrent(0);
	}

	/**
	 * Returns the window id
	 * @return long The window id
	 */
	public long getId() {
		return windowId;
	}

	/**
	 * Resizes the window
	 * @param width int New width
	 * @param height int New height
	 */
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		glfwSetWindowSize(windowId, width, height);
	}

	/**
	 * Sets the window's icon
	 * @param i Image To use as a icon
	 */
	public void setIcon(Image i) {
		if (i != null && i.getTexture() != null) {
			try (MemoryStack stack = stackPush()) {
				icon = i;
				GLFWImage image = GLFWImage.malloc();
				GLFWImage.Buffer buffer = GLFWImage.malloc(1);

				image.set(icon.getTexture().getWidth(), icon.getTexture().getHeight(), icon.getTexture().getBuffer());
				buffer.put(0, image);
				glfwSetWindowIcon(windowId, buffer);
				MemoryUtil.memFree(buffer);
				image.free();
			}
		}
	}

	/**
	 * Updates the window title
	 * uses the thread executor as glfwSetWindowTitle locks
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
		threadExecutors.execute(new Runnable() {
			@Override
			public void run() {
				glfwSetWindowTitle(windowId, title);
			}
		});
	}

	/**
	 * Destroys the window
	 */
	public void destroy() {
		glfwFreeCallbacks(windowId);
		glfwDestroyWindow(windowId);
		threadExecutors.shutdownNow();
		System.out.println("Window Destroyed."); 
	}

	/**
	 * Returns the window's GL capabilities
	 * @return GLCapabilites
	 */
	public GLCapabilities getGLCapabilities() {
		return glc;
	}

}
