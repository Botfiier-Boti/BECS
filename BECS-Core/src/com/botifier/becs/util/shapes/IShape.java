package com.botifier.becs.util.shapes;

import java.awt.Color;

import org.joml.Vector2f;

import com.botifier.becs.graphics.Renderer;
import com.botifier.becs.graphics.images.Image;

public interface IShape {

	/**
	 * Checks if the shape contains specified point
	 * @param v Vector2f To check
	 * @return boolean If the point is within
	 */
	public boolean contains(Vector2f v);

	/**
	 * Checks if the shape contains specified point
	 * @param x float X
	 * @param y float Y
	 * @return boolean If the point is within
	 */
	public boolean contains(float x, float y);

	/**
	 * Draws the outline of the shape
	 * @param r Renderer To use
	 * @param c Color To use
	 */
	public void draw(Renderer r, Color c);

	/**
	 * Draws a filled version of the shape
	 * @param r Renderer To use
	 * @param c Color To use
	 */
	public void drawFilled(Renderer r, Color c);

	/**
	 * Uses an image to draw the shape
	 * @param r Renderer To use
	 * @param i Image To use
	 * @param c Color To use
	 * @param flipped boolean If the image should be flipped
	 */
	public void drawImage(Renderer r, Image i, Color c, boolean flipped);

	/**
	 * Draws the outline of the shape without begin and end
	 * @param r Renderer To use
	 * @param c Color To use
	 */
	public void drawRaw(Renderer r, Color c);

	/**
	 * Rotates the shape
	 * @param angle float Angle
	 */
	public void rotate(float angle);

	/**
	 * Sets the shapes rotation
	 * @param angle float Angle
	 */
	public void setAngle(float angle);

	/**
	 * DrawRaw but with an Image
	 * @param r Renderer To use
	 * @param i Image To use
	 * @param c Color To use
	 */
	public void drawImageNoBegin(Renderer r, Image i, Color c);

	/**
	 * Converts the shape into a Polygon
	 * @return Polygon result
	 */
	public Polygon toPolygon();

}
