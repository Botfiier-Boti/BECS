package com.botifier.becs.tests;


import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//import java.awt.Color;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
//import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import com.botifier.becs.Game;
import com.botifier.becs.WorldState;
import com.botifier.becs.config.ControlsConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponent;
import com.botifier.becs.entity.EntityComponentManager;
import com.botifier.becs.entity.systems.ArrowKeyControlsSystem;
import com.botifier.becs.entity.systems.PhysicsSystem;
import com.botifier.becs.events.listeners.PhysicsListener;
import com.botifier.becs.graphics.AutoBatcher;
import com.botifier.becs.graphics.FBO;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.graphics.text.Font;
import com.botifier.becs.sound.Sound;
import com.botifier.becs.util.CollisionUtil.PolygonOutput;
import com.botifier.becs.util.EntityRunnable;
import com.botifier.becs.util.ParameterizedRunnable;
import com.botifier.becs.util.SpatialEntityMap;
import com.botifier.becs.util.render.Camera;
import com.botifier.becs.util.shapes.Circle;
import com.botifier.becs.util.shapes.Line;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;
import com.botifier.becs.util.shapes.Shape;

//https://www.lwjgl.org/guide Hello world lwjgl 3
public class HelloWorld extends Game{
	Font f;
	Vector2f[] vs;
	private Image i, i2, i3;
	float of;
	float dir = 1;
	int size = 0;
	boolean dire = false;
	Entity wew;
	Entity e;
	Entity center;
	AutoBatcher auto = new AutoBatcher();
	PhysicsSystem ps = new PhysicsSystem(this);
	ShaderProgram sp;
	FBO fbo;

	Polygon test;
	RotatableRectangle rotRect, rotRect2, rotRect3, rotRect4, rotRect5;
	Circle c;
	Line temp;
	ArrayList<Vector2f> previousLocs = new ArrayList<>();
	Camera camera;

	public HelloWorld() {
		super("Hello World", 800, 800, false, true, false);
	}

	@Override
	public void init() {

		ControlsConfig.addControl("UP", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP);
		ControlsConfig.addControl("DOwN", GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
		ControlsConfig.addControl("LEFT", GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
		ControlsConfig.addControl("RIGHT", GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);
		ControlsConfig.addControl("CONFIRM", GLFW.GLFW_KEY_SPACE);
		EntityComponentManager.createComponent("Bullet", Boolean.class);

		//System.out.println("Hello World! LWJGL Java "+Version.getVersion());

		//fbo = new FBO("framebuffer.vert", "framebuffer.frag").init();

		i = new Image("Tile.png");
		i.setScale(1f);
		i2 = new Image("christmasfroggy.png");
		i2.setScale(0.5f);
		//i2.setShaderProgram(sp);
		i3 = new Image("WhitePixel.png");
		
		try {
			i2.getTexture().write(new File("froggy.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}


		Sound so = Sound.createSound("sounds/alien_crow.wav", false, true);
		//so.playCopy();
		//so.playCopy();
		
		e = new TestPlayer(0, 0);
		Entity.addEntity(e);

		for (int i = 0; i < 4; i++) {
			final int localI = i;
			Entity e2 = new Entity("Davy") {
				@Override
				public Entity init() {
					Image e = new Image(i2);
					RotatableRectangle rr = new RotatableRectangle(-600, -200+localI*i2.getHeight(), i2.getWidth(), i2.getHeight());
					e.setShape(rr);
					this.addComponent("Image", e);
					this.addComponent("Position", new Vector2f(-600, -200+localI*rr.getHeight()));
					this.addComponent("Velocity", new Vector2f(0f, 0));
					this.addComponent("CollisionShape", rr);
					this.addComponent("Collidable", true);
					//e2.addComponent("IgnoreWith", "Collidable");

					Entity local = this;
					this.addComponent("Interactable", new EntityRunnable() {
						@Override
						public void run(Entity origin, Entity target) {
							if (target == local) {
								System.out.println("hee");
							}
						}
					});
					this.addComponent("Solid", true);
					return this;
				}
			};
			
			
			Entity.addEntity(e2);
		}
		for (int i = 0; i < 1; i++) {
			final int localI = i;
			Entity e2 = new Entity("Davy") {
				@Override
				public Entity init() {
					Image e = new Image(i2);
					RotatableRectangle rr = new RotatableRectangle(-600, -600+localI*i2.getHeight()-1000, i2.getWidth(), i2.getHeight());
					e.setShape(rr);
					this.addComponent("Image", e);
					this.addComponent("Position", new Vector2f(-600, -600+localI*rr.getHeight()-1000));
					this.addComponent("Velocity", new Vector2f(0, 0));
					this.addComponent("CollisionShape", rr);
					this.addComponent("Collidable", true);
					this.addComponent("Solid", true);
					return this;
				}
			};
			Entity.addEntity(e2);
		}
		for (int i = 0; i < 1; i++) {
			final int localI = i;
			wew = new Entity("Davy") {
				@Override
				public Entity init() {
					float angle = (float) Math.toRadians(22.5f);
					Image e = new Image(i2);
					RotatableRectangle rr = new RotatableRectangle(-400+((float)Math.cos(angle))*localI*i2.getWidth(), -400+((float)Math.sin(angle))*localI*i2.getHeight(), i2.getWidth(), i2.getHeight());
					rr.setAngle(angle);
					e.setShape(rr);
					this.addComponent("Image", e);
					this.addComponent("Position", new Vector2f(-400+localI*rr.getWidth()*((float)Math.cos(angle)), -400+localI*rr.getHeight()*((float)Math.sin(angle))));
					//e2.addComponent("Velocity", new Vector2f(0, 0));
					this.addComponent("CollisionShape", rr);
					this.addComponent("Collidable", true);
					this.addComponent("Solid", true);
					return this;
				}
			};
			Entity.addEntity(wew);
		}
		
		IntStream.range(0, 10000).parallel().forEach(i -> {
			final int localI = i;
			Entity e2 = new Entity("Davy") {
				@Override
				public Entity init() {
					Image e = new Image(i2);
					e.setScale(0.2f);
					RotatableRectangle rr =  new RotatableRectangle(-800+localI*-e.getWidth(), -400, e.getWidth(), e.getHeight());
					e.setShape(rr);
					e.setZ(-1);
					addComponent("Image", e);
					addComponent("Position", new Vector2f(-800+localI*-rr.getWidth(), -400));
					addComponent("CollisionShape", rr);
					addComponent("Collidable", true);
					addComponent("IgnoreWith", "Bullet");
					addComponent("Bullet", true);
					addComponent("Solid", true);
					//addComponent("PhysicsEnabled", new PhysicsListener(e.getUUID()));
					//addComponent("ArrowKeyControlled", true);
					return this;
				}
			};
			Entity.addEntity(e2);
		});

		addSystem(ps);
		addSystem(new ArrowKeyControlsSystem(this));
		


		rotRect = new RotatableRectangle(400, 400, 128, 128, (float) (Math.PI*0));
		rotRect2 = new RotatableRectangle(getWidth(), 0, 1600, 4);
		rotRect3 = new RotatableRectangle(0, getHeight(), 1600, 4);
		rotRect4 = new RotatableRectangle(getWidth(), getHeight(), 1600, 4);
		rotRect5 = new RotatableRectangle(0, 0, 50, 50);

		//c = new Circle(0, 0, 100);
		center = e;

		size = 256;
		vs = new Vector2f[size * size];
		//Vector2f[][] ve = new Vector2f[size][size];
		int pos = 0;
		for (int i = 0; i < size; i ++) {
			for (int e = 0; e < size; e ++) {
				vs[pos] = new Vector2f(e*(this.i.getScale()*this.i.getTexture().getWidth()), i*(this.i.getScale()*this.i.getTexture().getHeight()));
				pos++;
			}
		}
		/*for (Entity e : Entity.getEntities()) {
			if (hasComponent(e, "CollisionShape")) {
				EntityComponent<Shape> shapeComp = e.getComponent("CollisionShape");
				IShape rr = shapeComp.get();
				rr.rotate(0);
			}
		}*/

		EntityComponent<Shape> shapeComp = e.getComponent("CollisionShape");
		if (shapeComp != null) {
			Shape s = shapeComp.get();

			float x1 = s.getCenter().x - s.getDimensions().x;
			float y1 = s.getCenter().y - s.getDimensions().y;

			float x2 = s.getCenter().x + s.getDimensions().x + 40;
			float y2 = s.getCenter().y + s.getDimensions().y + 40;


			test = Polygon.createPolygon(new Vector2f(x1, y1), new Vector2f(x2, y2), new Vector2f(x1, y2), new Vector2f(x2, y1));
			temp = new Line(0, 0, 1, 1);
		}

		
		camera = new Camera(this, new Vector2f(0, 0), this.getWidth(), this.getHeight());
		camera.setFollowEntity(center);

		
		getRenderer().setZoom(1f);
		getRenderer().setCameraCenter(new Vector2f(0, 0));
	}

	@Override
	public void update() {

		//rotRect.setRotation(Math2.calcAngle(rotRect.getCenter(), getInput().getMousePos()));
		//rotRect2.setRotation(Math2.calcAngle(rotRect2.getCenter(), getInput().getMousePos()));
		//rotRect3.setRotation(Math2.calcAngle(rotRect3.getCenter(), getInput().getMousePos()));
		//rotRect4.setRotation(Math2.calcAngle(rotRect4.getCenter(), getInput().getMousePos()));
		//Vector2f velocity = VelocityComponent.getVelocity(center);

		//float x3 = s.getCenter().x + s.getDimensions().x/2 + velocity.x;
		//float y3 = s.getCenter().y + s.getDimensions().y/2 + velocity.y;
		//test = Polygon.createPolygon(new Vector2f(x1, y1), new Vector2f(x1, y2), new Vector2f(x2, y2), new Vector2f(x2, y1));

		//rotRect5.setCenter(getInput().getMousePos());

		//EntityComponent<Vector2f> posComp = e.getComponent("Position");

		//if (posComp.get().x <);

		if (getInput().isKeyPressed(GLFW.GLFW_KEY_C)) {
			wew.removeComponent("Position");
		}
		
		setTitle("FPS: "+getTimer().getFPS()+", UPS: "+getTimer().getUPS());
	}

	@Override
	public void draw(Renderer r, WorldState state, RotatableRectangle camera, float alpha) {
		this.camera.draw(r);
		
		if (center != null) {
			EntityComponent<Vector2f> velComp = center.getComponent("Velocity");
			EntityComponent<Vector2f> posComp = center.getComponent("Position");
			Vector2f v = velComp.get();
			Vector2f p = posComp.get();
			r.writeText("Velocity: "+v.x+", "+v.y, -r.getCurrentWidth()/2+20, r.getCurrentHeight()/2-20);
			r.writeText("Position: "+p.x+", "+p.y, -r.getCurrentWidth()/2+20, r.getCurrentHeight()/2-40);
		}
		/*
		if (center != null) {
			if (center.hasComponent("Position")) {
				EntityComponent<Vector2f> posComp = center.getComponent("Position");
				Vector2f v = posComp.get();
				center.getComponent("Position").get();
				r.setCameraCenter(v);
			}
		}*/

		//i.drawBatched(getRenderer(), vs);

		//camera.draw(r, Color.red);
		/*EntityComponent<Vector2f> posComp = e.getComponent("Position");
		EntityComponent<Vector2f> velComp = e.getComponent("Velocity");
		EntityComponent<Shape> shapeComp = e.getComponent("CollisionShape");

		test = shapeComp.get().toPolygon().move(velComp.get());
		test = test.mergeNoRepeat(shapeComp.get().toPolygon());*/

		//i.drawBatched(r, vs);

		//SpatialEntityMap sem = state.sem;
		//Set<Entity> draw = sem.getEntitiesIn(camera.toPolygon());
		//Image.WHITE_PIXEL.bind();
		//r.begin();
		/*
		sem.getGrid().keySet().forEach(k -> {
			RotatableRectangle rr = new RotatableRectangle(k.x*sem.getCellSize()+sem.getCellSize()/2, k.y*sem.getCellSize()+sem.getCellSize()/2, sem.getCellSize(), sem.getCellSize());
			r.getAutoBatcher().add(Image.WHITE_PIXEL, rr, Color.yellow, -2);
		});*/
		//r.end();
		
		/*
		draw.stream().forEach(e -> {
						  		e.setAutoBatch(true);
						  		e.draw(r);
						  });*/
		
		//renderDebugCollisions(r, sem);
		/*
		sem.getGrid().keySet().parallelStream().forEach(k -> {
			int cellSize = sem.getCellSize();
			float cX = k.x * cellSize + cellSize;
			float cY = k.y * cellSize + cellSize;

			if (r.getFrustumIntersection().testPoint(cX, cY, -1)) {
				RotatableRectangle rr = new RotatableRectangle(cX, cY, cellSize, cellSize);
				auto.add(Image.WHITE_PIXEL, rr, Color.blue, -1);
			}
		});*/

	}

	public void renderDebugCollisions(Renderer r, SpatialEntityMap sem) {
		EntityComponent<Vector2f> posComp = e.getComponent("Position");
		EntityComponent<Vector2f> velComp = e.getComponent("Velocity");
		EntityComponent<Shape> shapeComp = e.getComponent("CollisionShape");

		Polygon test = shapeComp.get().toPolygon().move(velComp.get());
		test = test.mergeNoRepeat(shapeComp.get().toPolygon());

		Set<Entity> tes =  sem.getEntitiesIn(test.toPolygon());
		List<Entity> te = tes.stream().filter(e -> e.getUUID() != this.e.getUUID()).sorted((a, b) -> {
			EntityComponent<Vector2f> posCompA = a.getComponent("Position");
			EntityComponent<Vector2f> posCompB = b.getComponent("Position");

			float distA  = posCompA.get().distance(posComp.get());
			float distB  = posCompB.get().distance(posComp.get());

			return Float.compare(distA, distB);
		}).collect(Collectors.toList());

		if (test != null) {
			Color c2 = Color.blue;
			Vector2f sub = new Vector2f(0);
			Polygon next = shapeComp.get().toPolygon().move(new Vector2f(velComp.get()));
			Polygon tempTest = shapeComp.get().toPolygon().mergeNoRepeat(next);

			for (Entity e : te) {
				if (e.getUUID() == this.e.getUUID()) {
					continue;
				}
				EntityComponent<Shape> sc = e.getComponent("CollisionShape");
				Shape s = sc.get();
				Polygon t = s.toPolygon();
				//r.getAutoBatcher().add(Image.WHITE_PIXEL, s, Color.yellow);



				PolygonOutput pOut = tempTest.intersectsSAT(t);
				PolygonOutput pOut2 = t.intersectsSAT(tempTest);
				if (pOut != null) {
					float dist = t.getCenter().distance(shapeComp.get().getCenter());

					float angle = (float) Math.atan2(velComp.get().y, velComp.get().x);
					Vector2f ex2 = sc.get().toPolygon().getEdgePoint(angle, shapeComp.get().getCenter());
					//t.draw(r, c2.brighter());
					Image.WHITE_PIXEL.bind();
					r.begin();
					r.drawRectangle(ex2.x, ex2.y, 1, 10, 10, Color.magenta);
					r.end();
					Vector2f pen = new Vector2f(pOut.getNormal()).mul(pOut.getDepth());
					Vector2f outer = new Vector2f(shapeComp.get().getCenter().add(pen));

					Line l = new Line(shapeComp.get().getCenter(), outer);
					l.draw(r, Color.red);

					sub.sub(pen);
					next = shapeComp.get().toPolygon().move(new Vector2f(velComp.get()).add(sub));
					tempTest = shapeComp.get().toPolygon().mergeNoRepeat(next);
				}
				if (pOut2 != null) {
					Vector2f pen = new Vector2f(pOut2.getNormal()).mul(pOut2.getDepth());
					Vector2f outer = new Vector2f(shapeComp.get().getCenter().add(pen));
					Line l = new Line(t.getCenter(), outer);
					l.draw(r, Color.orange);

				}
			}

			float mag = velComp.get().length();
			float mag2 = sub.length();

			//Reduces fullVejAdj's magnitude if it is too high
			//Prevents bouncing backwards, sort of.
			if (mag2 > mag) {
				sub.normalize(mag);
			}

			test.draw(r, Color.white);
			Vector2f cen = new Vector2f(posComp.get());
			Vector2f nex = new Vector2f(cen).add(velComp.get());
			Line l2 = new Line(cen, nex);
			//l2.draw(r, Color.green);
			Line l = new Line (cen, nex.add(sub));
			l.draw(r, Color.blue);

			//test = shapeComp.get().toPolygon();
			//test = test.mergeNoRepeat(tempTest);
			//test.draw(r, Color.yellow);
			r.writeText("Center: "+test.getCenter().x+", "+test.getCenter().y, test.getCenter().x, test.getCenter().y);
			tempTest.draw(r, c2);

			EntityComponent<ArrayList<Vector2f>> trailComp = e.getComponent("Trailer");
			if (trailComp != null) {
				for (int i = 0; i < trailComp.get().size(); i++) {
					Vector2f v = trailComp.get().get(i);
					Shape s = shapeComp.get().clone();
					s.setCenter(v.x, v.y);
					this.auto.add(Image.WHITE_PIXEL, s, new Color(255, 100, 100, 50));
					if (i < trailComp.get().size() - 1) {
						Line ll = new Line(v, trailComp.get().get(i+1));
						this.auto.add(Image.WHITE_PIXEL, ll, Color.cyan);
					} else {
						Line ll = new Line(v, posComp.get());
						this.auto.add(Image.WHITE_PIXEL, ll, Color.cyan);
					}
				}
			}

			float angle = (float) Math.atan2(velComp.get().y, velComp.get().x);


			Vector2f e1 = shapeComp.get().toPolygon().getEdgePoint(angle);

			Image.WHITE_PIXEL.bind();
			r.begin();
			r.drawRectangle(e1.x, e1.y, 1, 10, 10, Color.magenta);
			r.end();
		}
	}

	@Override
	public void exit() {
		
	}
}
