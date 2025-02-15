package com.botifier.becs.graphics;

import java.awt.Color;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.botifier.becs.graphics.images.Image;
import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.util.shapes.Shape;


/**
 * Automatic Sprite Batching class
 * @author Botifier
 *
 */
public class AutoBatcher {

	/**
	 * Batch Queue
	 */
	private HashMap<Texture, ConcurrentLinkedQueue<Image>> queue = new HashMap<>();

	/**
	 * Draws every image with texture as a batch
	 * @param r
	 */
	public void draw(Renderer r) {
		//Render everything by texture
		queue.entrySet().stream().forEach(entry -> {
			Texture t = entry.getKey();
			ConcurrentLinkedQueue<Image> images = entry.getValue();
			t.bind();
			r.begin();
			images.stream().filter(i -> i != null).forEach(i -> {
			   				   if (i.getShape() != null) {
			   					   i.getShape().drawImageNoBegin(r, i, i.getColor());
			   				   } else {
								i.drawNoBegin(r);
							}
			   			   });
			r.end();
		});
		//Clear the queue as it is done
		queue.clear();
	}

	/**
	 * Adds specified image to queue
	 * @param i Image to add
	 * @param s Shape to use
	 */
	public void add(Image i, Shape s) {
		add(i, s, Color.white, i.getZ());
	}

	/**
	 * Adds specified image to Queue
	 * @param i Image to add
	 * @param s Shape to use
	 * @param c Color to use
	 */
	public void add(Image i, Shape s, Color c) {
		add(i, s, c, i.getZ());
	}

	/**
	 * Adds specified image using specified shape and z position to queue
	 * @param i Image to add
	 * @param s Shape to use
	 * @param c Color to use
	 * @param z int to be
	 */
	public void add(Image i, Shape s, Color c, float z) {
		if (!queue.containsKey(i.getTexture())) {
			queue.put(i.getTexture(), new ConcurrentLinkedQueue<>());
		}
		ConcurrentLinkedQueue<Image> toDraw = queue.get(i.getTexture());
		Image build = new Image(i.getTexture());
		build.setZ(z);
		build.setShape(s);
		build.setColor(c);
		toDraw.add(build);
	}

	/**
	 * Adds specified image to queue
	 * @param i Image to use
	 * @param x X location
	 * @param y Y location
	 */
	public void add(Image i, float x, float y) {
		add(i, x, y, i.getTexture().getWidth(), i.getTexture().getHeight(), i.getScale(), i.getColor());
	}

	/**
	 * Adds specified image to queue
	 * @param i Image to add
	 * @param x X location
	 * @param y Y location
	 * @param c Color to use
	 */
	public void add(Image i, float x, float y, Color c) {
		add(i, x, y, i.getTexture().getWidth(), i.getTexture().getHeight(), i.getScale(), c);
	}

	/**
	 * Adds specified image to queue
	 * @param i Image to add
	 * @param x X location
	 * @param y Y location
	 * @param width Image width
	 * @param height Image height
	 * @param c Color to use
	 */
	public void add(Image i, float x, float y, float width, float height, Color c) {
		add(i, x, y, width, height, 1, c);
	}

	/**
	 * Adds specified image to queue
	 * @param i Image to add
	 * @param x X location
	 * @param y Y location
	 * @param width Image width
	 * @param height Image height
	 * @param scale width and height multiplier
	 * @param c Color to use
	 */
	public void add(Image i, float x, float y, float width, float height, float scale, Color c) {
		queue.computeIfAbsent(i.getTexture(), k -> new ConcurrentLinkedQueue<Image>()).add(createImage(i, x, y, width, height, scale, c));
	}

	/**
	 * Creates a copy Image at a specified location and custom data
	 *
	 * @param i Image to use
	 * @param x X location
	 * @param y Y location
	 * @param width Image width
	 * @param height Image height
	 * @param scale Width and height multiplier
	 * @param c Color to use
	 * @return Resulting image
	 */
	private Image createImage(Image i, float x, float y, float width, float height, float scale, Color c) {
		Image build = new Image(i.getTexture());
		build.setPosition(x, y);
		build.setHeight(height);
		build.setWidth(width);
		build.setScale(scale);
		build.setColor(c);
		return build;
	}

}
