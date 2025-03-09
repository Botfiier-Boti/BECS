package com.botifier.becs.tests;

import java.awt.Color;
import java.util.List;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import com.botifier.becs.Game;
import com.botifier.becs.WorldState;
import com.botifier.becs.config.ControlsConfig;
import com.botifier.becs.entity.Entity;
import com.botifier.becs.entity.EntityComponentManager;
import com.botifier.becs.entity.systems.ArrowKeyControlsSystem;
import com.botifier.becs.entity.systems.PhysicsSystem;
import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.util.shapes.Circle;
import com.botifier.becs.util.shapes.Line;
import com.botifier.becs.util.shapes.Polygon;
import com.botifier.becs.util.shapes.RotatableRectangle;
import com.botifier.becs.util.shapes.Shape;

public class CollisionTest extends Game {
	Entity p;
	Entity e;
	Polygon test;
	Polygon mouse;

	public CollisionTest() {
		super("Collision Test", 480, 480, false, false);
	}

	@Override
	public void init() {
		ControlsConfig.addControl("UP", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP);
		ControlsConfig.addControl("DOwN", GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN);
		ControlsConfig.addControl("LEFT", GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT);
		ControlsConfig.addControl("RIGHT", GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT);
		ControlsConfig.addControl("CONFIRM", GLFW.GLFW_KEY_SPACE);

		addSystem(new PhysicsSystem(this));
		addSystem(new ArrowKeyControlsSystem(this));

		p = new Entity("Mover");
		e = new Entity("Stander");

		p.addComponent("CollisionShape", new RotatableRectangle(200, 200, 40, 40));
		p.addComponent("Position", new Vector2f(200, 200));
		p.addComponent("Velocity", new Vector2f(0, 0));
		e.addComponent("ArrowKeyControlled", true);
		p.addComponent("Speed", 2.0f);

		e.addComponent("CollisionShape", new RotatableRectangle(150, 150, 40, 40));
		e.addComponent("Position", new Vector2f(150, 150));
		e.addComponent("Velocity", new Vector2f(0, 0));
		e.addComponent("Speed", 2.0f);
		//e.destroy();
		test = Polygon.createPolygon(new Vector2f(0, 0), new Vector2f(0, 40), new Vector2f(40, 0));

	}

	@Override
	public void update() {

	}

	@Override
	public void draw(Renderer r, WorldState ws, RotatableRectangle camera, float alpha) {
		r.setCameraCenter(new Vector2f(0, 0));
		if (p != null) {
			Vector2f v = (Vector2f) p.getComponent("Position").get();
			Vector2f v2 = new Vector2f(v);

			r.setCameraCenter(v2);
		}

		//p.draw(r);

		for (Entity en : Entity.getEntities()) {
			if (!EntityComponentManager.hasComponent(en, "CollsionShape")) {
				continue;
			}

			Shape s = (Shape) en.getComponent("CollsionShape").get();

			s.draw(r, Color.white);
		}
		test = ((RotatableRectangle)p.getComponent("CollisionShape").get()).toPolygon();
		RotatableRectangle rr = ((RotatableRectangle) p.getComponent("CollisionShape").get()).clone();
		Vector2f v = (Vector2f) p.getComponent("Velocity").get();
		rr.moveCenter(v);

		Polygon hold = ((RotatableRectangle)p.getComponent("CollisionShape").get()).toPolygon();
		Polygon hold2 = rr.toPolygon();

		test = test.mergeNoRepeat(rr.toPolygon());

		Color c = Color.white;
		if (test.contains(getInput().getMousePos())) {
			c = Color.yellow;
		}

		test.draw(r, c);

		for (int i = 0; i < test.getPoints().length; i++) {
			r.writeText(i+"", test.getPoint(i).x, test.getPoint(i).y);
		}

		Circle ci = new Circle(200, 200, 100);
		//Polygon p = ci.toPolygon();
		//rr.draw(r, Color.yellow);
		Vector2f h = new Vector2f(v).normalize();
		hold = hold.move(h);
		hold2 = hold2.move(h.negate());

		//hold.draw(r, Color.red);
		//hold2.draw(r, Color.blue);

		List<Line> edges = hold.getEdges();
		List<Line> edges2 = hold2.getEdges();

		/*for (Line l : edges) {
			for (Line l2 : edges2) {
				if (l.intersects(l2)) {
					l.draw(r, Color.yellow);
					l2.draw(r, Color.green);
				}
				Vector2f po = l.getPointOfIntersection(l2);
				if (po != null) {
					Image.WHITE_PIXEL.bind();
					r.begin();
					r.drawRectangle(po.x-8, po.y-8, 16, 16, Color.green);
					r.end();
				}
			}
		}*/

		r.tempResetCamera();
		//r.writeText("FPS: "+getTimer().getFPS(), 0, getHeight() - 24);
		h = (Vector2f) this.p.getComponent("Velocity").get();
		r.writeText("Velocity: "+h.x+", "+h.y, 0, getHeight() - 48);
		r.writeText("Points: "+test.getPoints().length, 0, getHeight() - 72);
		//p.draw(r, Color.white);
		RotatableRectangle mouse = new RotatableRectangle(getInput().getMousePosUnmod(), 40, 40);

		Polygon clip = test.clip(mouse.toPolygon());
		test.draw(r, Color.white);
		if (clip != null) {
			clip.draw(r, Color.red);

			float dx = clip.getWidth();
			float dy = clip.getHeight();

			if (mouse.getCenter().y > test.getCenter().y) {
				mouse.moveCenter(0, dy);
			} else if (mouse.getCenter().y < test.getCenter().y) {
				mouse.moveCenter(0, -dy);
			}

			if (mouse.getCenter().x > test.getCenter().x) {
				mouse.moveCenter(dx, 0);
			} else if (mouse.getCenter().x < test.getCenter().x) {
				mouse.moveCenter(-dx, 0);
			}
		}
		mouse.draw(r, Color.white);
	}

	@Override
	public void exit() {

	}

}
