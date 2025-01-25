package com.botifier.becs.graphics.shader;

import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2fv;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniform4fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix2fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glDeleteProgram;
import static org.lwjgl.opengl.GL20C.glGetProgramiv;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glGetUniformiv;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_WORK_GROUP_SIZE;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Matrix2f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

public class ComputeShaderProgram {

	private final int id;
	@SuppressWarnings("unused")
	private int workGroupX, workGroupY, workGroupZ;
	private Map<String, Integer> uniformMap;
	private List<Integer> buffers;
	private Shader shader;

	/**
	 * ComputeShader constructor
	 * @param frameBufferImageId String FrameBufferImage id
	 */
	public ComputeShaderProgram(String shaderLoc) {
		this.id = glCreateProgram();
		shader = Shader.loadShader(GL43.GL_COMPUTE_SHADER, shaderLoc);
		buffers = new ArrayList<>();
	}

	/**
	 * Initializes the program using specified uniforms
	 * @param uniforms String... uniforms to use
	 */
	public ComputeShaderProgram init(String... uniforms) {
		useProgram();
		shader.compile();
		glAttachShader(id, shader.getID());
		link();

		IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
		glGetProgramiv(this.id, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
		workGroupX = workGroupSize.get(0);
		workGroupY = workGroupSize.get(1);
		workGroupZ = workGroupSize.get(2);

		Stream<String> stream = Arrays.stream(uniforms);
		uniformMap = stream.collect(Collectors.toMap(s -> s, s-> glGetUniformLocation(id, s)));

		freeProgram();
		return this;
	}

	/**
	 * Attaches a shader
	 * @param shader Shader To use
	 */
	public void attachShader(Shader shader) {
		glAttachShader(this.id, shader.getID());
	}

	/**
	 * Uses the program
	 */
	public void useProgram() {
		glUseProgram(id);
	}

	/**
	 * Links the program
	 */
	public void link() {
		glLinkProgram(id);
	}

	/**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, int value) {
        glUniform1i(location, value);
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Vector2f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(2);
            value.get(buffer);
            glUniform2fv(location, buffer);
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Vector3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3);
            value.get(buffer);
            glUniform3fv(location, buffer);
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Vector4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4);
            value.get(buffer);
            glUniform4fv(location, buffer);
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Matrix2f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(2 * 2);
            value.get(buffer);
            glUniformMatrix2fv(location, false, buffer);
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Matrix3f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(3 * 3);
            value.get(buffer);
            glUniformMatrix3fv(location, false, buffer);
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    public void setUniform(int location, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(4 * 4);
            value.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    /**
     * Gets the location of a uniform
     * @param uniformId String Id of the uniform
     * @return int Uniform location
     */
	public int getUniformLocation(String uniformId) {
		return uniformMap.get(uniformId);
	}

	/**
	 *
	 * @param loc int Uniform Location
	 * @param params IntBuffer Uniform info
	 */
	public void getUniformIv(int loc, IntBuffer params) {
		glGetUniformiv(id, loc, params);
	}

	/**
	 * Dispatches the compute shader
	 */
	public void dispatch(int x, int y, int z) {
		glDispatchCompute(x, y, z);
	}

	/**
	 * Binds a buffer
	 * @param type int Type of buffer
	 * @param buffer int Location of the buffer
	 */
	public void bindBufferBase(int type, int buffer) {
		glBindBufferBase(type, buffers.size(), buffer);
		buffers.add(buffer);
	}

	/**
	 * Places a memory barrier
	 * @param barriers int Barrier
	 */
	public void memoryBarrier(int barriers) {
		glMemoryBarrier(barriers);
	}

	/**
	 * Destroys stuff
	 */
	public void cleanup() {
		shader.delete();
		glDeleteProgram(this.id);
	}

	/**
	 * Returns the max work group size in x
	 * @return int Size x
	 */
	public int getMaxWorkgroupX() {
		return this.workGroupX;
	}

	/**
	 * Returns the max work group size in y
	 * @return int Size y
	 */
	public int getMaxWorkgroupY() {
		return this.workGroupY;
	}

	/**
	 * Unuses the program
	 */
	public static void freeProgram() {
		glUseProgram(0);
	}

}
