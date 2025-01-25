package com.botifier.becs.graphics;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH32F_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

import com.botifier.becs.Game;
import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.graphics.shader.Shader;
import com.botifier.becs.graphics.shader.ShaderProgram;

public class FBO {
	/**
	 * FBO id
	 */
	private final int id;
	/**
	 * FBO output texture
	 */
	private Texture frameBufferTexture;
	/**
	 * The RenderBuffer
	 */
	private RBO depth;
	/**
	 * The Vertex buffer
	 */
	private VBO vbo;
	/**
	 * The Vertex array
	 */
	private VAO vao;
	/**
	 * The shader locations
	 */
	private String vertex, frag;
	/**
	 * The shader program
	 */
	private ShaderProgram program;

	/**
	 * FBO constructor
	 * @param vertex String Vertex shader location
	 * @param frag String Fragment shader location
	 */
	public FBO(String vertex, String frag) {
		id = glGenFramebuffers();
		depth = new RBO();
		this.vertex = vertex;
		this.frag = frag;
	}

	/**
	 * Binds the FrameBuffer
	 * Redirects rendering to the FrameBuffer image
	 */
	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER, id);
	}

	/**
	 * Unbinds the FrameBuffer
	 */
	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	/**
	 * Initializes the FrameBuffer
	 * @return FBO itself
	 */
	public FBO init() {
		bind();
		genTextures();
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameBufferTexture.getId(), 0);

		int width = Game.getCurrent().getWidth();
		int height = Game.getCurrent().getHeight();

		depth.bind();
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH32F_STENCIL8, width, height);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth.getId());
		depth.unbind();

		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer incomplete: "+status); 
		}

		float[] buff = {-1, -1,
						 1, -1,
						-1,  1,
						 1,  1};

		vao = new VAO();
		vao.bind();

		vbo = new VBO();
		vbo.bind(GL_ARRAY_BUFFER);
		vbo.uploadData(GL_ARRAY_BUFFER, buff, GL_DYNAMIC_DRAW);
		glEnableVertexAttribArray(0);


		genShader();
		unbind();
		return this;
	}

	/**
	 * Resizes the FrameBuffer image
	 * @param width int New width
	 * @param height int New height
	 */
	public void resize(int width, int height) {
		bind();
		frameBufferTexture.bind();
		frameBufferTexture.uploadData(width, height, null);
		glBindTexture(GL_TEXTURE_2D, 0);

		depth.bind();
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH32F_STENCIL8, width, height);
		depth.unbind();
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth.getId());
		unbind();
	}

	/**
	 * Generates the FrameBuffer texture
	 */
	public void genTextures() {
		if (frameBufferTexture != null) {
			return;
		}
		int width = Game.getCurrent().getWidth();
		int height =  Game.getCurrent().getHeight();
		Texture t = Texture.createTexture(width, height, null);
		t.bind();
		glBindTexture(GL_TEXTURE_2D, 0);
		frameBufferTexture = t;
	}

	/**
	 * Loads the Shaders
	 * Fragment location: fragColor
	 * Attributes:
	 * - aPos (size = 2, stride = 0, offset = 1)
	 */
	public void genShader() {
		Shader v = Shader.loadShader(GL_VERTEX_SHADER, vertex);
		Shader f = Shader.loadShader(GL_FRAGMENT_SHADER, frag);

		program = new ShaderProgram();
		getShaderProgram().attachShader(v);
		getShaderProgram().attachShader(f);
		getShaderProgram().bindFragmentDataLocation(0, "fragColor");
		getShaderProgram().link();

		getShaderProgram().use();
		int aPos = getShaderProgram().getAttributeLocation("aPos");
		getShaderProgram().enableVertexAttribute(aPos);
		getShaderProgram().pointVertexAttribute(aPos, 2, 0, 0);

	}

	/**
	 * Draws the FrameBuffer
	 * @param r Renderer to use
	 */
	public void draw(Renderer r){
		getShaderProgram().use();

		glColorMask(true, true, true, true);
		//glClear(GL_COLOR_BUFFER_BIT);

		getShaderProgram().setUniform(getShaderProgram().getUniformLocation("fbo_texture"), 0);
		glActiveTexture(GL_TEXTURE0);
		frameBufferTexture.bind();

		vao.bind();

		vbo.bind(GL_ARRAY_BUFFER);

		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindTexture(GL_TEXTURE_2D, 0);
		glEnableVertexAttribArray(0);

	}

	/**
	 * Deletes the FrameBuffer and everything associated
	 */
	public void delete() {
		glDeleteFramebuffers(id);
		getFrameBufferTexture().delete();
		vbo.delete();
		vao.delete();
		depth.delete();
		getShaderProgram().delete();
	}

	/**
	 * Returns the FrameBuffer texture
	 * @return Texture FrameBuffer texture
	 */
	public Texture getFrameBufferTexture() {
		return frameBufferTexture;
	}

	/**
	 * Returns the ShaderProgram
	 * @return ShaderProgram ShaderProgram
	 */
	public ShaderProgram getShaderProgram() {
		return program;
	}

}
