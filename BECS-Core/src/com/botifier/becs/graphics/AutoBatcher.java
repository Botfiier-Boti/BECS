package com.botifier.becs.graphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

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
	private ConcurrentHashMap<Texture, ArrayList<Image>> queue = new ConcurrentHashMap<>();

	/**
	 * Draws every image with texture as a batch
	 * @param r
	 */
	public void draw(Renderer r) {
		//Render everything by texture
		queue.entrySet().stream().forEach(entry -> {
			Texture t = entry.getKey();
			ArrayList<Image> images = entry.getValue();
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
		add(i, img -> {
			img.setShape(s);
			img.setColor(c);
			img.setZ(z);
			return img;
		});
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
		add(i, img -> {
			img.setPosition(x, y);
			img.setWidth(width);
			img.setHeight(height);
			img.setScale(scale);
			img.setColor(c);
			return img;
		});
	}

	/**
	 * Adds specified image to queue
	 * 
	 * used like add(i, img -\> {
	 * 		img.setShape(shape);
	 * 		return img;
	 * }
	 * 
	 * @param i Image to add
	 * @param config Consumer\<Image\> Used to manipulate the output image 
	 */
	public void add(Image i, Function<Image, Image> config) {
		Image build = new Image(i);
		
		if (config != null)
			build = config.apply(build);
		
		queue.computeIfAbsent(i.getTexture(), k -> new ArrayList<Image>()).add(build);
	}

}
