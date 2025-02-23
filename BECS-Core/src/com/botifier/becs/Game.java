package com.botifier.becs;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DONT_CARE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLUtil;

import com.botifier.becs.config.IConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponentManager;
import com.botifier.becs.entity.EntitySystem;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.sound.SoundListener;
import com.botifier.becs.sound.SoundManager;
import com.botifier.becs.util.Input;
import com.botifier.becs.util.shapes.RotatableRectangle;
import com.botifier.becs.util.events.*;
import com.botifier.becs.events.KeyCharacterActionEvent;
import com.botifier.becs.events.listeners.*;

//Based on https://www.lwjgl.org/guide
//Added a separate thread for game loop and render. like in https://github.com/LWJGL/lwjgl3-demos/blob/main/src/org/lwjgl/demo/opengl/glfw/Multithreaded.java
/**
 * Base game class
 * 
 * @author Botifier
 *
 */
public abstract class Game {
	/**
	 * Target updates per second
	 */
	private final int targetUPS = 60;

	/**
	 * Debug mode
	 */
	private static AtomicBoolean debug = new AtomicBoolean(false);

	/**
	 * Current active game
	 */
	private static Game current;

	/**
	 * Input controller
	 */
	private Input input;

	/**
	 * Window id
	 */
	private Window window;

	/**
	 * Renderer
	 */
	private AtomicReference<Renderer> renderer = new AtomicReference<Renderer>();

	/**
	 * Sound manager
	 */
	private SoundManager soundManager;

	/**
	 * Event manager
	 */
	private EventManager eventManager;

	/**
	 * Size Callback Updates width and height when the window is resized
	 */
	private GLFWFramebufferSizeCallback fbc;

	/**
	 * Error Callback
	 */
	@SuppressWarnings("unused")
	private GLFWErrorCallback ecb;

	/**
	 * Cursor Position Callback
	 */
	private GLFWCursorPosCallback ccb;

	/**
	 * Character Callback For typing
	 */
	private GLFWCharCallback gcc;

	/**
	 * Key Callback Sends key input to the input manager
	 */
	private GLFWKeyCallback fkc;

	/**
	 * Game title
	 */
	private String title;

	/**
	 * Vsync enabled/disabled true = enabled false = disabled
	 *
	 * Only used at startup
	 */
	private boolean vsync;

	/**
	 * Boolean indicating that the autobatcher should be drawn automatically
	 */
	private boolean autoDrawBatch = true;

	/**
	 * Whether or not the locks should be used
	 */
	private final boolean noLock;

	/**
	 * Tracks whether or not the game is running
	 */
	private final AtomicBoolean running = new AtomicBoolean(true);

	/**
	 * Whether or not the game is resizable
	 */
	private boolean resizable;

	/**
	 * Window width
	 */
	private AtomicInteger width = new AtomicInteger(0);

	/**
	 * Window height
	 */
	private AtomicInteger height = new AtomicInteger(0);

	/**
	 * Initial window width
	 */
	private int iWidth;

	/**
	 * Initial window height
	 */
	private int iHeight;

	/**
	 * Game delta
	 */
	private AtomicReference<Float> delta = new AtomicReference<>(0f);

	/**
	 * Accumulator Used for game ticks
	 */
	@SuppressWarnings("unused")
	private float accumulator = 0f;

	/**
	 * How many ticks have occurred since the game started.
	 */
	private AtomicLong ticksAlive = new AtomicLong(0);

	/**
	 * Interval Used for game ticks
	 */
	private float interval = 1f / targetUPS;

	/**
	 * Alpha Used for interpolation
	 */
	private AtomicReference<Float> alpha = new AtomicReference<>(0f);

	/**
	 * Lock for threads. Used if noLock is false.
	 */
	private ReentrantLock l = new ReentrantLock();

	/**
	 * Game timer Tracks the UPS and FPS
	 */
	private GameTimer t;

	/**
	 * Window icon
	 */
	private Image icon;

	/**
	 * Game systems Stuff like physics or any other custom system
	 */
	private List<EntitySystem> systems = new CopyOnWriteArrayList<EntitySystem>();

	/**
	 * World listener id
	 */
	private AtomicReference<UUID> worldListenerId = new AtomicReference<UUID>(null);

	/**
	 * Current configs
	 */
	private Map<String, IConfig> configs = new ConcurrentHashMap<>();

	/**
	 * Game constructor
	 * 
	 * @param title     Window title
	 * @param width     Window width
	 * @param height    Window height
	 * @param vsync     Enable/disable vsync
	 * @param resizable Enable/disable window resizing
	 * @param noLock    Sets whether or not locks should be used; Causes visual
	 *                  artifacts
	 */
	public Game(String title, int width, int height, boolean vsync, boolean resizable, boolean noLock) {
		this.title = title;
		this.iWidth = width;
		this.iHeight = height;
		this.vsync = vsync;
		this.resizable = resizable;
		this.noLock = noLock;
		this.setWidth(width);
		this.setHeight(height);
	}

	/**
	 * Game constructor
	 * 
	 * @param title     Window title
	 * @param width     Window width
	 * @param height    Window height
	 * @param vsync     Enable/disable vsync
	 * @param resizable Enable/disable window resizing
	 */
	public Game(String title, int width, int height, boolean vsync, boolean resizable) {
		this(title, width, height, vsync, resizable, false);
	}

	/**
	 * Runs the game
	 */
	public void run() {
		l.lock();
		try {
			initialize();
		} finally {
			l.unlock();
		}
		procLoop();
		cleanup();
	}

	/**
	 * Cleanup function
	 */
	private void cleanup() {
		running.set(false);
		soundManager.destroy();
		glfwMakeContextCurrent(window.getId());
		GL.setCapabilities(window.getGLCapabilities());
		exit();
		clearSystems();
		renderer.get().destroy();
		window.destroy();
		GL.setCapabilities(null);
		glfwTerminate();
	}

	/**
	 * Customizable Intializer
	 */
	public abstract void init();

	/**
	 * Customizable Update
	 */
	public abstract void update();

	/**
	 * Customizable Draw
	 * 
	 * @param r      Renderer to use
	 * @param ws     WorldState a wrapper for the Entity Map
	 * @param camera RotatableRectangle A RotatableRectangle representing the camera
	 *               area
	 * @param alpha  Alpha used for interpolation
	 */
	public abstract void draw(Renderer r, WorldState ws, RotatableRectangle camera, float alpha);

	/**
	 * Customizable Exit
	 *
	 * Used for freeing memory and the like.
	 */
	public abstract void exit();

	/**
	 * Initialization of Window and Game functionality Runs init()
	 */
	private void initialize() {
		ecb = GLFWErrorCallback.createPrint(System.err);

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		current = this;

		window = new Window(title, getWidth(), getHeight(), resizable, vsync);

		glfwMakeContextCurrent(window.getId());

		glfwSetKeyCallback(window.getId(), fkc = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (input != null) {
					input.keyAction(key, action);
				}
			}
		});

		glfwSetFramebufferSizeCallback(window.getId(), fbc = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				setWidth(width);
				setHeight(height);
				if (getRenderer() != null) {
					getRenderer().setZoom(1);
					getRenderer().setOffset(new Vector2f());
					getRenderer().refreshWindow();
				}
			}
		});

		glfwSetCursorPosCallback(window.getId(), ccb = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double x, double y) {
				if (input != null) {
					input.updateMousePos(x, y);
				}
			}
		});

		GLFW.glfwSetWindowCloseCallback(window.getId(), new GLFWWindowCloseCallback() {

			@Override
			public void invoke(long window) {
				running.set(false);
			}

		});

		GLFW.glfwSetCharCallback(window.getId(), gcc = new GLFWCharCallback() {

			@Override
			public void invoke(long window, int charCode) {
				if (input != null) {
					input.setLastCharCode(charCode);
				}
				if (eventManager != null)
					eventManager.executeEvent(new KeyCharacterActionEvent(charCode), "CharCallback");
			}

		});

		t = new GameTimer();
		t.init();

		EntityComponentManager.init();

		soundManager = new SoundManager();
		try {
			soundManager.init();
		} catch (Exception e) {
			e.printStackTrace();
		}

		soundManager.setListener(new SoundListener());
		renderer.set(new Renderer());
		renderer.get().init(this);
		renderer.get().refreshWindow();

		eventManager = new EventManager();
		WorldListener wl = new WorldListener();
		eventManager.registerListener(wl);
		worldListenerId.set(wl.getOwner());

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println(String.format("Thread %s threw an Exception: %s", t.getName(), e.getMessage()));
				e.printStackTrace();
			}
		});

		init();

		input = new Input(window.getId());
		glfwMakeContextCurrent(0);
	}

	/**
	 * Creates a thread to separate the window and game loop
	 */
	private void procLoop() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		if (isDebug()) {
			GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
					(IntBuffer) null, false);
			GLUtil.setupDebugMessageCallback();
		}

		window.setIcon(icon);

		ScheduledExecutorService schedular = Executors.newScheduledThreadPool(1, new HighPriorityThreadFactory());
		ScheduledFuture<?> update = schedular.scheduleAtFixedRate(new UpdateRunnable(), 0, 1000 / targetUPS,
				TimeUnit.MILLISECONDS);

		ScheduledExecutorService renderExecutor = Executors.newScheduledThreadPool(1, new HighPriorityThreadFactory());
		ScheduledFuture<?> render = renderExecutor.scheduleWithFixedDelay(new RenderRunnable(), 0, 10,
				TimeUnit.NANOSECONDS);

		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		while (running.get()) {
			GLFW.glfwWaitEventsTimeout(1);
			long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked.

			if (threadIds != null) {
				ThreadInfo[] infos = bean.getThreadInfo(threadIds);

				for (ThreadInfo info : infos) {
					StackTraceElement[] stack = info.getStackTrace();
					System.err.print(Arrays.toString(stack));
				}
			}
		}
		update.cancel(true);
		render.cancel(true);
		renderExecutor.shutdownNow();
		schedular.shutdownNow();
	}

	/**
	 * Game loop
	 *
	 * Performs some initializations and then starts the game loop Capped at UPS No
	 * longer used in favor of scheduled Runnables
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void loop() {
		float accumulator = 0;
		while (running.get()) {
			t.update();
			ticksAlive.incrementAndGet();
			delta.set(t.getDelta());
			accumulator += delta.get();
			try {

				l.lock();
				int updates = 0;
				while (accumulator > interval && updates < 5) {
					update();
					t.updateUPS();
					systems.forEach(system -> {
						Entity[] entities = system.getValidEntities().toArray(Entity[]::new);
						system.apply(entities);
					});
					accumulator -= interval;
					updates++;
				}

				if (accumulator > interval * 2) {
					accumulator = 0;
				}

				alpha.set(accumulator / interval);
				// getInput().purgeUnconsumedKeys();
			} finally {
				l.unlock();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Render loop
	 *
	 * Runs as many times a second as possible.
	 *
	 * No longer in use in favor of delayed runnables
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void render() {
		glfwMakeContextCurrent(window.getId());
		GL.setCapabilities(window.getGLCapabilities());
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		if (isDebug()) {
			GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
					(IntBuffer) null, false);
			GLUtil.setupDebugMessageCallback();
		}

		window.setIcon(icon);

		while (running.get()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			// l.lock();
			RotatableRectangle camera = new RotatableRectangle(getRenderer().getCameraCenter().x,
					getRenderer().getCameraCenter().y, getWidth() * getRenderer().getZoom(),
					getHeight() * getRenderer().getZoom());
			WorldState ws = new WorldState(camera.toPolygon(), noLock);

			draw(getRenderer(), ws, camera, alpha.get());
			if (autoDrawBatch) {
				getRenderer().getAutoBatcher().draw(getRenderer());
			}
			if (getRenderer().hasRendered()) {
				glfwSwapBuffers(window.getId());
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			getRenderer().resetRenderStatus();
			t.updateFPS();
			// l.unlock();
		}
		exit();
		renderer.get().destroy();
		GL.setCapabilities(null);
	}

	/**
	 * Adds a config to use
	 * 
	 * @param name   String Name of the config
	 * @param config IConfig To use
	 */
	public void addConfig(String name, IConfig config) {
		configs.put(name.toLowerCase(), config);
	}

	/**
	 * Removes config
	 * 
	 * @param name String Name of the config
	 * @return IConfig The dropped config
	 */
	public IConfig dropConfig(String name) {
		return configs.remove(name.toLowerCase());
	}

	/**
	 * Sets the window width
	 * 
	 * @param width Window width
	 */
	public void setWidth(int width) {
		this.width.set(width);
		;
	}

	/**
	 * Sets the window height
	 * 
	 * @param height Window height
	 */
	public void setHeight(int height) {
		this.height.set(height);
		;
	}

	/**
	 * Sets the window icon
	 * 
	 * @param i Image to set icon as
	 */
	public void setIcon(Image i) {
		icon = i;
		window.setIcon(i);
	}

	/**
	 * Sets the window title
	 * 
	 * @param title Title to use
	 */
	public void setTitle(String title) {
		this.title = title;
		window.setTitle(title);
	}

	/**
	 * Sets whether or not the AutoBatcher should automatically be drawn.
	 * 
	 * @param auto
	 */
	public void setBatchAutoDraw(boolean auto) {
		this.autoDrawBatch = auto;
	}

	/**
	 * Removes all active entity systems
	 */
	public void clearSystems() {
		systems.forEach(s -> s.destroy());
		systems.clear();
	}

	/**
	 * Plays a sound from file location
	 * 
	 * @param loc File location
	 */
	@Deprecated
	public static void playSound(String loc) {

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Clip c = AudioSystem.getClip();
					c.close();
					BufferedInputStream bis = new BufferedInputStream(Game.class.getResourceAsStream("/" + loc));
					AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
					c.open(ais);
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	/**
	 * Plays a sound from specified input stream
	 * 
	 * @param loc Audio to play
	 */
	@Deprecated
	public static void playSound(AudioInputStream loc) {
		Thread t = new Thread() {
			@Override
			public void run() {

				try {
					Clip c = AudioSystem.getClip();

					c.close();
					c.open(loc);
					c.start();
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	/**
	 * Sets the debug state of the game
	 * 
	 * @param debug
	 */
	public static void setDebug(boolean debug) {
		Game.debug.set(debug);
		;
	}

	/**
	 * Returns the current debug state
	 * 
	 * @return Debug
	 */
	public static boolean isDebug() {
		return debug.get();
	}

	/**
	 * Returns the input manager
	 * 
	 * @return Input Manager
	 */
	public Input getInput() {
		return input;
	}

	/**
	 * Returns the game timer
	 * 
	 * @return Game Timer
	 */
	public GameTimer getTimer() {
		return t;
	}

	/**
	 * Returns the entity systems
	 * 
	 * @return Entity Systems as ArrayList
	 */
	public List<EntitySystem> getEntitySystems() {
		return systems;
	}

	/**
	 * Adds specified entity system
	 * 
	 * @param es System to add
	 */
	public void addSystem(EntitySystem es) {
		systems.add(es);
	}

	/**
	 * Returns the current window icon
	 * 
	 * @return icon as Image
	 */
	public Image getIcon() {
		return icon;
	}

	/**
	 * Returns the renderer
	 * 
	 * @return Renderer
	 */
	public Renderer getRenderer() {
		return renderer.get();
	}

	/**
	 * Returns the window title
	 * 
	 * @return Window Title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns current game width
	 * 
	 * @return Width
	 */
	public int getWidth() {
		return width.get();
	}

	/**
	 * Returns current game height
	 * 
	 * @return Height
	 */
	public int getHeight() {
		return height.get();
	}

	/**
	 * Returns initial game width
	 * 
	 * @return Initial Game Width
	 */
	public int getInitWidth() {
		return iWidth;
	}

	/**
	 * Returns initial game height
	 * 
	 * @return Initial Game Height
	 */
	public int getInitHeight() {
		return iHeight;
	}

	/**
	 * Returns window id
	 * 
	 * @return Process ID of Window
	 */
	public long getWindowID() {
		return window.getId();
	}

	/**
	 * Returns how many ticks have passed since the game started.
	 * 
	 * @return
	 */
	public long getCurrentTick() {
		return ticksAlive.get();
	}

	/**
	 * Returns the last delta time
	 * 
	 * @return The last delta time value
	 */
	public float getDelta() {
		return delta.get();
	}

	/**
	 * Returns the current active game
	 * 
	 * @return Current active game
	 */
	public static Game getCurrent() {
		return current;
	}

	/**
	 * Checks if OpenGL3.2 is supported
	 * 
	 * @return Whether OpenGL3.2 is supported or not
	 */
	public static boolean supportsOpenGL32() {
		return GL.getCapabilities().OpenGL32;
	}

	/**
	 * Returns the sound manager
	 * 
	 * @return SoundManager
	 */
	public SoundManager getSoundManager() {
		return soundManager;
	}

	/**
	 * Returns the event manager
	 * 
	 * @return EventManager
	 */
	public EventManager getEventManager() {
		return eventManager;
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
	 * The key callback
	 * 
	 * @return GLFWKeyCallback
	 */
	public GLFWKeyCallback getFkc() {
		return fkc;
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
	 * The character callback
	 * 
	 * @return GLFWCharCallback
	 */
	public GLFWCharCallback getCharacterCallback() {
		return gcc;
	}

	/**
	 * Gets the UUID of the world listener
	 * 
	 * @return UUID
	 */
	public UUID getWorldListenerId() {
		return worldListenerId.getAcquire();
	}

	public Window getWindow() {
		return window;
	}

	@SuppressWarnings("unchecked")
	public <T extends IConfig> T getConfig(String name) {
		return (T) configs.getOrDefault(name.toLowerCase(), null);
	}

	/**
	 * Runnable for the update thread
	 */
	private class UpdateRunnable implements Runnable {

		@Override
		public void run() {
			if (!running.get()) {
				return;
			}
			Thread.currentThread().setName("Update Thread");
			t.update(); // Update the timer
			delta.set(t.getDelta()); // set delta
			try {
				if (!noLock) {
					l.lock(); // Obtains a lock if locking is enabled
					tick();
					l.unlock(); // Unlocks if locking is enabled
				} else
					tick();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ticksAlive.incrementAndGet(); // Add to the tick tracker
			}
		}
		
		private void tick() {
			update(); // Performs an update
			t.updateUPS(); // Updates UPS counter (Updates Per Second)
			systems.stream().parallel().forEach(system -> { // Runs all systems in parallel
				Entity[] entities = system.getValidEntities().toArray(Entity[]::new); // Obtains all valid entities
																						// for a system
				system.apply(entities); // Applies the system to all of those entities
			});
		}
	}

	/**
	 * Runnable for the render thread
	 */
	private class RenderRunnable implements Runnable {

		@Override
		public void run() {
			if (!running.get()) {
				return;
			}
			Thread.currentThread().setName("Render Thread");
			glfwMakeContextCurrent(window.getId()); // Obtains context
			GL.setCapabilities(window.getGLCapabilities()); // Obtains the current window's GL Capabilities
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clears the frame

			try {
				if (!noLock) {
					l.lock(); // Locks if locking is enabled
					render();
					l.unlock();
				} else {
					render();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (getRenderer().hasRendered()) {
					glfwSwapBuffers(window.getId()); // Only swaps buffers when a render has occurred
				}
				getRenderer().resetRenderStatus(); // Resets the rendering status, for tracking whether or not any
													// Draws happened on this frame
				t.updateFPS(); // Updates the FPS counter
			}
		}

		private void render() {
			// A rectangle representing the camera, for culling purposes
			RotatableRectangle camera = new RotatableRectangle(getRenderer().getCameraCenter().x,
					getRenderer().getCameraCenter().y, getWidth() * getRenderer().getZoom(),
					getHeight() * getRenderer().getZoom());
			WorldState ws = new WorldState(camera.toPolygon(), false); // Creates a WorldState
			getRenderer().refreshWindow();
			draw(getRenderer(), ws, camera, alpha.get()); // Runs draw functions
			if (autoDrawBatch) {
				getRenderer().getAutoBatcher().draw(getRenderer()); // Automatically draws information in the
																	// AutoBatcher
			}
		}

	}

	private class HighPriorityThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);

			thread.setPriority(Thread.MAX_PRIORITY);
			thread.setDaemon(true);
			return thread;
		}

	}

}
