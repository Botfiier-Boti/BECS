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
		//Binds the framebuffer
		bind();
		//Generates texures
		genTextures();
		//Attaches the framebuffer
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, frameBufferTexture.getId(), 0);

		//Obtain the dimensions
		int width = Game.getCurrent().getWidth();
		int height = Game.getCurrent().getHeight();

		//Bind the depth render buffer
		depth.bind();
		//Set the dimensions of the render buffer
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH32F_STENCIL8, width, height);
		//Attaches the render buffer to the frame buffer
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth.getId());
		//Unbinds the render buffer
		depth.unbind();

		//Checks if the frame buffer is complete
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			throw new RuntimeException("Framebuffer incomplete: "+status); 
		}

		//Frame buffer dimensions
		float[] buff = {-1, -1,
						 1, -1,
						-1,  1,
						 1,  1};

		//Creates and binds a new VAO
		vao = new VAO();
		vao.bind();

		//Creates and binds a new VBO on GL_ARRAY_BUFFER
		vbo = new VBO();
		vbo.bind(GL_ARRAY_BUFFER);
		//Uploads the dimension data to the VBO
		vbo.uploadData(GL_ARRAY_BUFFER, buff, GL_DYNAMIC_DRAW);
		//Unbinds VAO
		glEnableVertexAttribArray(0);
		
		//Generates the shaders
		genShader();
		
		//Unbinds the frame buffer
		unbind();
		return this;
	}

	/**
	 * Resizes the FrameBuffer image
	 * @param width int New width
	 * @param height int New height
	 */
	public void resize(int width, int height) {
		//Binds the frame buffer
		bind();
		//Binds the frame buffer's texture
		frameBufferTexture.bind();
		//Resizes the frame buffer's texture
		frameBufferTexture.uploadData(width, height, null);
		//Unbinds the texture
		glBindTexture(GL_TEXTURE_2D, 0);

		//Binds the render buffer
		depth.bind();
		//Updates the buffer dimensions
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH32F_STENCIL8, width, height);
		//Unbinds the render buffer
		depth.unbind();
		
		//Reattach the render buffer
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth.getId());
		//Unbinds the frame buffer
		unbind();
	}

	/**
	 * Generates the FrameBuffer texture
	 */
	public void genTextures() {
		//Can't generate textures if the texture wasn't created
		if (frameBufferTexture != null) {
			return;
		}
		//Obtains the window dimensions
		int width = Game.getCurrent().getWidth();
		int height =  Game.getCurrent().getHeight();
		
		//Creates a new texture with the supplied dimensions
		Texture t = Texture.createTexture(width, height, null);
		
		//Sets the frame buffer texture variable
		frameBufferTexture = t;
	}

	/**
	 * Loads the Shaders
	 * Fragment location: fragColor
	 * Attributes:
	 * - aPos (size = 2, stride = 0, offset = 1)
	 */
	public void genShader() {
		//Loads the vertex and fragment shaders
		Shader v = Shader.loadShader(GL_VERTEX_SHADER, vertex);
		Shader f = Shader.loadShader(GL_FRAGMENT_SHADER, frag);

		//Creates a new shader program
		program = new ShaderProgram();
		//Attaches the shaders
		getShaderProgram().attachShader(v);
		getShaderProgram().attachShader(f);
		//Binds fragBuffer to 0
		getShaderProgram().bindFragmentDataLocation(0, "fragColor");
		//Links the shader program
		getShaderProgram().link();

		//Use the shader program
		getShaderProgram().use();
		//obtain the aPos attribute location
		int aPos = getShaderProgram().getAttributeLocation("aPos");
		//Enable aPos
		getShaderProgram().enableVertexAttribute(aPos);
		//Sets the aPos pointer
		getShaderProgram().pointVertexAttribute(aPos, 2, 0, 0);

	}

	/**
	 * Draws the FrameBuffer
	 * @param r Renderer to use
	 */
	public void draw(Renderer r){
		//Start using the shader program
		getShaderProgram().use();

		//Sets the color mask
		glColorMask(true, true, true, true);
		//glClear(GL_COLOR_BUFFER_BIT);

		//Updates the fbo_texture uniform
		getShaderProgram().setUniform(getShaderProgram().getUniformLocation("fbo_texture"), 0);
		//Sets the active texture to GL_TEXTURE0
		glActiveTexture(GL_TEXTURE0);
		//Binds the frameBuffer texture
		frameBufferTexture.bind();

		//Binds the VAO
		vao.bind();

		//Binds the VBO to GL_ARRAY_BUFFER
		vbo.bind(GL_ARRAY_BUFFER);

		//Draws the frame buffer texture
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		//Unbinds the buffer, texture and VAO
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
