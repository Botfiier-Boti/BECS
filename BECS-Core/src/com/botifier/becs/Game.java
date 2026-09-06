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
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
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
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLUtil;

import com.botifier.becs.config.Config;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponentManager;
import com.botifier.becs.entity.EntitySystem;
import com.botifier.becs.events.listeners.WorldListener;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.sound.SoundListener;
import com.botifier.becs.sound.SoundManager;
import com.botifier.becs.util.Input;
import com.botifier.becs.util.ResourceManager;
import com.botifier.becs.util.events.EventManager;
import com.botifier.becs.util.glfw.GLFWWindow;
import com.botifier.becs.util.memory.HighSpeedGate;
import com.botifier.becs.util.glfw.GLFWInput;
import com.botifier.becs.util.glfw.GLFWGameTimer;
import com.botifier.becs.util.shapes.RotatableRectangle;

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

	/*
	 * A high speed atomic gate
	 */
	private static volatile HighSpeedGate gate = new HighSpeedGate();

	/**
	 * Window
	 */
	private Window window;

	/**
	 * Renderer
	 */
	private AtomicReference<Renderer> renderer = new AtomicReference<Renderer>();
	
	/**
	 * Whether or not framerate should be maximized
	 */
	private final static AtomicLong frameSleepNs = new AtomicLong(0);

	/**
	 * Sound manager
	 */
	private SoundManager soundManager;

	/**
	 * Event manager
	 */
	private EventManager eventManager;
	
	/**
	 * Resource manager
	 */
	private ResourceManager resourceManager;

	/**
	 * Game title
	 */
	private volatile String title;

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
	@Deprecated
	private final boolean noLock;

	/**
	 * Whether or not the game is resizable
	 */
	private boolean resizable;

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
	@Deprecated
	private ReentrantLock l = new ReentrantLock();

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
	private Map<String, Config> configs = new ConcurrentHashMap<>();

	/**
	 * Game constructor
	 * 
	 * @param title     Window title
	 * @param width     Window width
	 * @param height    Window height
	 * @param vsync     Enable/disable vsync
	 * @param resizable Enable/disable window resizing
	 * @param noLock    Deprecated - Sets whether or not locks should be used; Causes visual
	 *                  artifacts 
	 */
	public Game(String title, int width, int height, boolean vsync, boolean resizable, boolean noLock) {
		this.title = title;
		this.iWidth = width;
		this.iHeight = height;
		this.vsync = vsync;
		this.resizable = resizable;
		this.noLock = noLock;
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
		try (HighSpeedGate g = gate.tryUseClosable()) {
			if (g == null) return;
			initialize();
			eventManager.processEvents();
		}
		procLoop();
		cleanup();
	}

	/**
	 * Cleanup function
	 */
	private void cleanup() {
		soundManager.destroy();
		window.useCapabilities();
		exit();
		clearSystems();
		renderer.get().destroy();
		window.destroy();
		GL.setCapabilities(null);
		glfwTerminate();
		System.exit(0);
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
		current = this;

		window = new GLFWWindow(title, this, resizable, vsync);

		
		glfwMakeContextCurrent(window.getId());
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

		glfwMakeContextCurrent(0);
	}

	/**
	 * Creates a thread to separate the window and game loop
	 */
	private final void procLoop() {
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

		RenderRunnable rr = new RenderRunnable();
		Thread t = new Thread(() -> {
			while (window.isRunning()) {
				rr.run();
			}
		}, "Render Thread");
		
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

		while (window.isRunning()) {
			GLFW.glfwWaitEventsTimeout(1);
			Thread.onSpinWait();
		}
		
		update.cancel(true);
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
		while (window.isRunning()) {
			getTimer().update();
			ticksAlive.incrementAndGet();
			delta.set(getTimer().getDelta());
			accumulator += delta.get();
			try {

				l.lock();
				int updates = 0;
				while (accumulator > interval && updates < 5) {
					update();
					getTimer().updateUPS();
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
		window.useCapabilities();
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		if (isDebug()) {
			GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
					(IntBuffer) null, false);
			GLUtil.setupDebugMessageCallback();
		}

		window.setIcon(icon);

		while (window.isRunning()) {
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
			getTimer().updateFPS();
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
	 * @param config Config To use
	 */
	public void addConfig(String name, Config config) {
		configs.put(name.toLowerCase(), config);
	}

	/**
	 * Removes config
	 * 
	 * @param name String Name of the config
	 * @return Config The dropped config
	 */
	public Config dropConfig(String name) {
		return configs.remove(name.toLowerCase());
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
	
	public void grabContext() {
		long currentContext = GLFW.glfwGetCurrentContext();
		if (currentContext != window.getId())
			glfwMakeContextCurrent(window.getId()); // Obtains context
		window.useCapabilities();
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
	}
	
	/**
	 * Sets the delay in nanoseconds between frames
	 * 
	 * @param delay
	 */
	public static void setFrameDelayNs(int delay) {
		frameSleepNs.set(delay);
	}
	
	/**
	 * Gets the delay in nanoseconds between frames
	 * @return int The delay
	 */
	public static long getFrameDelayNs() {
		return frameSleepNs.get();
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
		return window.getInput();
	}

	/**
	 * Returns the game timer
	 * 
	 * @return Game Timer
	 */
	public GameTimer getTimer() {
		return window.getTimer();
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
	 * Returns initial game width
	 * 
	 * @return int Initial Game Width
	 */
	public int getInitWidth() {
		return iWidth;
	}

	/**
	 * Returns initial game height
	 * 
	 * @return int Initial Game Height
	 */
	public int getInitHeight() {
		return iHeight;
	}
	
	/**
	 * Returns game width
	 * 
	 * @return int Game Width
	 */
	public int getWidth() {
		return window.getWidth();
	}

	/**
	 * Returns game height
	 * 
	 * @return int Game Height
	 */
	public int getHeight() {
		return window.getHeight();
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
	 * Returns the resource manager
	 * 
	 * lazy loaded
	 * 
	 * @return ResourceManager
	 */
	public ResourceManager getResourceManager() {
		return resourceManager == null ? resourceManager = new ResourceManager() : resourceManager;
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
	public <T extends Config> T getConfig(String name) {
		return (T) configs.getOrDefault(name.toLowerCase(), null);
	}

	/**
	 * Runnable for the update thread
	 */
	private class UpdateRunnable implements Runnable {
		
		
		@Override
		public void run() {
			if (!window.isRunning()) {
				return;
			}
			
			Thread.currentThread().setName("Update Thread");
			getTimer().update(); // Update the timer
			delta.set(getTimer().getDelta()); // set delta
			accumulator += delta.get();
			try (HighSpeedGate g = gate.tryUseClosable()) {
				if (g == null) return;

				tick();
			}catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		private void tick() {
			update(); // Performs an update
			
			for (EntitySystem system : systems) {
				Entity[] entities = system.getValidEntities().toArray(Entity[]::new); // Obtains all valid entities
				
				system.apply(entities).join(); // Applies the system to all of those entities and waits for futures to complete
			}

			eventManager.processEvents();
			
			getTimer().updateUPS(); // Updates UPS counter (Updates Per Second)
			
		}
	}

	/**
	 * Runnable for the render thread
	 */
	private class RenderRunnable implements Runnable {
		
		@Override
		public void run() {
			if (!window.isRunning()) 
				return;
			if (gate.isBusy()) {
				LockSupport.parkNanos(1L); //Park for a moment to avoid overwhelming the core the render thread is on
				return;
			}
			
			long currentContext = GLFW.glfwGetCurrentContext();
			if (currentContext != window.getId())
				glfwMakeContextCurrent(window.getId()); // Obtains context
			window.useCapabilities(); // Obtains the current window's Capabilities
			window.clear(); // Clears the frame

			try {
				render();
			} finally {
				if (getRenderer().hasRendered()) 
					glfwSwapBuffers(window.getId()); // Only swaps buffers when a render has occurred
				
				getRenderer().resetRenderStatus(); // Resets the rendering status, for tracking whether or not any
													// Draws happened on this frame
				getTimer().updateFPS(); // Updates the FPS counter

				LockSupport.parkNanos(getFrameDelayNs());
			}
			
			Thread.yield();
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
