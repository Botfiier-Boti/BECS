package com.botifier.becs.graphics;

import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;

public class RBO {
	/**
	 * RBO id
	 */
	private final int id;

	/**
	 * RenderBuffer constructor
	 */
	public RBO() {
		id = glGenRenderbuffers();
	}

	/**
	 * Binds the RenderBuffer
	 */
	public void bind() {
		glBindRenderbuffer(GL_RENDERBUFFER, id);
	}

	/**
	 * Unbinds the RenderBuffer
	 * Doesn't matter if this one is bound
	 * It unbinds whatever is there
	 */
	public void unbind() {
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
	}

	/**
	 * Deletes the RenderBuffer
	 */
	public void delete() {
		glDeleteRenderbuffers(id);
	}

	/**
	 * Returns the RenderBuffer id
	 * @return int The id
	 */
	public int getId() {
		return id;
	}
}
