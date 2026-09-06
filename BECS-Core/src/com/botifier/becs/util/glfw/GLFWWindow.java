package com.botifier.becs.util.glfw;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.botifier.becs.Game;
import com.botifier.becs.GameTimer;
import com.botifier.becs.Window;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.util.Input;

/**
 * Window
 * 
 * @author Botifier
 */
public class GLFWWindow implements Window {
	/**
	 * Thread pool
	 */
	final ExecutorService threadExecutors = Executors.newVirtualThreadPerTaskExecutor();
	
	/**
	 * The game that owns this window
	 */
	private final Game g;
	/**
	 * The input for this w
	 */
	private GLFWInput input;
	/**
	 * The input for this w
	 */
	private GLFWGameTimer timer;
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
	private AtomicInteger width, height;
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
	 * Size Callback Updates width and height when the window is resized
	 */
	private GLFWFramebufferSizeCallback fbc;

	/**
	 * Cursor Position Callback
	 */
	private GLFWCursorPosCallback ccb;
	
	/**
	 * Tracks whether or not the window is running
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * Window constructor
	 * @param title String Title of the window
	 * @param width int Width of the window
	 * @param height int Height of the window
	 * @param resizable boolean Whether or not the window is resizable
	 * @param vsync boolean Whether or not to use VSync
	 */
	public GLFWWindow(String title, Game g,  boolean resizable, boolean vsync) {
		this.g = g;
		this.title = g.getTitle();
		this.width = new AtomicInteger(g.getInitWidth());
		this.height = new AtomicInteger(g.getInitHeight());
		this.vsync = vsync;
		init(resizable ? GLFW_TRUE : GLFW_FALSE);
	}

	/**
	 * Initializes the window
	 * @param resizable boolean Whether or not the window can be resized
	 */
	private void init(int resizable) {
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, resizable);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);

		//Creates the window
		windowId = glfwCreateWindow(width.getAcquire(), height.getAcquire(), title, NULL, NULL);
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
		
		//Set the window as running
		running.set(true);
		
		//Setup GLFW related callbacks
		setupCallbacks();
		
		//Sets up input
		input = new GLFWInput(g, windowId).init();
		//Sets up a timer
		timer = new GLFWGameTimer().init();
		
		//Displays the window on screen
		glfwShowWindow(windowId);
		//Drops the context
		glfwMakeContextCurrent(0);
	}

	private void setupCallbacks() {
		
		glfwSetErrorCallback(new GLFWErrorCallback() {

			@Override
			public void invoke(int error, long description) {
				throw new IllegalStateException("GLFW ERROR CODE "+ error +": "+ GLFWErrorCallback.getDescription(description));
			}
			
		});
		
		glfwSetWindowCloseCallback(windowId, new GLFWWindowCloseCallback() {

			@Override
			public void invoke(long window) {
				running.set(false);
			}

		});
		
		glfwSetFramebufferSizeCallback(windowId, fbc = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int w, int h) {
				width.set(w);
				height.set(h);
				if (g.getRenderer() != null) {
					g.getRenderer().setZoom(1);
					g.getRenderer().setOffset(new Vector2f());
					g.getRenderer().refreshWindow();
				}
			}
		});
		
		glfwSetCursorPosCallback(windowId, ccb = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double x, double y) {
				if (input != null) {
					input.updateMousePos(x, y);
				}
			}
		});
	}
	
	/**
	 * Returns the window id
	 * @return long The window id
	 */
	@Override
	public long getId() {
		return windowId;
	}

	/**
	 * Resizes the window
	 * @param width int New width
	 * @param height int New height
	 */
	@Override
	public void resize(int width, int height) {
		this.width.set(width);
		this.height.set(height);
		glfwSetWindowSize(windowId, width, height);
	}

	/**
	 * Sets the window's icon
	 * @param i Image To use as a icon
	 */
	@Override
	public void setIcon(Image i) {
		if (i != null && i.getTexture() != null) {
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

	/**
	 * Updates the window title
	 * uses the thread executor as glfwSetWindowTitle locks
	 * @param title
	 */
	@Override
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
	@Override
	public void destroy() {
		running.set(false);
		glfwFreeCallbacks(windowId);
		glfwDestroyWindow(windowId);
		threadExecutors.shutdownNow();
		System.out.println("Window Destroyed."); 
	}

	@Override
	public int getWidth() {
		return width.get();
	}
	
	@Override
	public int getHeight() {
		return height.get();
	}
	
	/**
	 * The frame buffer size callback
	 * 
	 * @return GLFWFramebufferSizeCallback
	 */
	public GLFWFramebufferSizeCallback getFbc() {
		return fbc;
	}
	/**
	 * The cursor position callback
	 * 
	 * @return GLFWCursorPosCallback
	 */
	public GLFWCursorPosCallback getCursorPosCallback() {
		return ccb;
	}
	
	/**
	 * Returns the window's GL capabilities
	 * @return GLCapabilites
	 */
	public GLCapabilities getGLCapabilities() {
		return glc;
	}
	
	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public void useCapabilities() {
		GL.setCapabilities(glc);
	}

	@Override
	public void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public GLFWInput getInput() {
		return input;
	}

	@Override
	public GLFWGameTimer getTimer() {
		return timer;
	}

}
