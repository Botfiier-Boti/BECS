package com.botifier.becs.graphics;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2014-2015, Heiko Brumme
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class VBO {

	private final int id;

	public VBO() {
		id = glGenBuffers();
	}

	public void bind(int target) {
		glBindBuffer(target, getId());
	}

	public void uploadData(int target, float[] data, int usage) {
		glBufferData(target, data, usage);
	}

	public void uploadData(int target, FloatBuffer data, int usage) {
		glBufferData(target, data, usage);
	}

	public void uploadData(int target, ByteBuffer data, int usage) {
		glBufferData(target, data, usage);
	}

	public void uploadData(int target, long size, int usage) {
		glBufferData(target, size, usage);
	}

	public void uploadSubData(int target, long offset, FloatBuffer data) {
		glBufferSubData(target, offset, data);
	}

	public void uploadData(int target, IntBuffer data, int usage) {
		glBufferData(target, data, usage);
	}

	public void delete() {
		glDeleteBuffers(getId());
	}

	public int getID() {
		return getId();
	}

	public int getId() {
		return id;
	}

}
