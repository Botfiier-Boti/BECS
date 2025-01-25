package com.botifier.becs.graphics.shader;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glShaderSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2014-2017, Heiko Brumme
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
public class Shader {

	private final int id;

	public Shader(int type) {
		id  = glCreateShader(type);
	}

	public void source(CharSequence source) {
		glShaderSource(id, source);
	}

	public void compile() {
		glCompileShader(id);

		checkStatus();
	}

	private void checkStatus() {
		int status = glGetShaderi(id, GL_COMPILE_STATUS);
		if (status != GL_TRUE) {
			throw new RuntimeException(glGetShaderInfoLog(id));
		}
	}

	public void delete() {
		glDeleteShader(id);
	}

	public int getID() {
		return id;
	}

	public static Shader createShader(int type, CharSequence source) {
		Shader shader = new Shader(type);
		shader.source(source);
		shader.compile();

		return shader;
	}

	public static Shader loadShader(int type, String path) {
		StringBuilder b = new StringBuilder();
		ClassLoader cl = Shader.class.getClassLoader();
		try (InputStream in = cl.getResourceAsStream(path)) {
			BufferedReader r = new BufferedReader(new InputStreamReader(in));

			String line;

			while ((line = r.readLine()) != null) {
				b.append(line).append('\n');
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load shader file:\n" + e.getMessage()); 
		}

		CharSequence source = b.toString();

		return createShader(type, source);
	}
}
