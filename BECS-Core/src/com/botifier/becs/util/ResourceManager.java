package com.botifier.becs.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.graphics.shader.Shader;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.sound.Sound;

/**
 * ResourceManager
 * 
 * TODO: Consider making this local and storing in Game
 * 
 * @author Botifier
 */
public class ResourceManager {

	private final Map<String, Texture> images = new ConcurrentHashMap<String, Texture>();
	private final Map<String, Sound> sounds = new ConcurrentHashMap<String, Sound>();
	private final Map<String, ShaderProgram> shaderPrograms = new ConcurrentHashMap<String, ShaderProgram>();
	private final Map<String, Shader> shaders = new ConcurrentHashMap<String, Shader>();

	public Texture loadTexture(String name, String loc) {
		return images.put(name.toLowerCase(), Texture.loadTexture(loc));
	}

	public Texture loadOrGetTexture(String name, String loc) {
		return images.computeIfAbsent(name.toLowerCase(), t -> Texture.loadTexture(loc));
	}

	public Texture putTexture(String name, Texture t) {
		return images.put(name.toLowerCase(), t);
	}

	public Texture getTexture(String name) {
		return images.getOrDefault(name.toLowerCase(), null);
	}

	public Texture getOrPutTexture(String name, Function<? super String, ? extends Texture> consumer) {
		return images.computeIfAbsent(name.toLowerCase(), consumer);
	}

	public Sound loadSound(String name, String loc) {
		return sounds.put(name.toLowerCase(), Sound.createSound(loc, false, false));
	}

	public Sound loadSound(String name, String loc, boolean loop, boolean relative) {
		return sounds.put(name.toLowerCase(), Sound.createSound(loc, loop, relative));
	}

	public Sound loadOrGetSound(String name, String loc) {
		return loadOrGetSound(name.toLowerCase(), loc, false, false);
	}

	public Sound loadOrGetSound(String name, String loc, boolean loop, boolean relative) {
		return sounds.computeIfAbsent(name.toLowerCase(), s -> Sound.createSound(loc, loop, relative));
	}

	public Sound putSound(String name, Sound s) {
		return sounds.put(name.toLowerCase(), s);
	}

	public Sound getSound(String name) {
		return sounds.getOrDefault(name.toLowerCase(), null).copy();
	}

	public Sound getOrPutSound(String name, Function<? super String, ? extends Sound> consumer) {
		return sounds.computeIfAbsent(name.toLowerCase(), consumer);
	}

	public ShaderProgram putShaderProgram(String name, ShaderProgram sp) {
		if (name == null)
			return null;
		return shaderPrograms.put(name.toLowerCase(), sp);
	}

	public ShaderProgram getShaderProgram(String name) {
		if (name == null)
			return null;
		return shaderPrograms.getOrDefault(name.toLowerCase(), null);
	}

	public ShaderProgram getOrPutShaderProgram(String name,
			Function<? super String, ? extends ShaderProgram> consumer) {
		return shaderPrograms.computeIfAbsent(name.toLowerCase(), consumer);
	}

	public Shader loadOrGetShader(String name, int shaderType, String location) {
		return shaders.computeIfAbsent(name.toLowerCase(), s -> Shader.loadShader(shaderType, location));
	}

	public Shader loadShader(String name, int shaderType, String location) {
		return shaders.put(name.toLowerCase(), Shader.loadShader(shaderType, location));
	}

	public Shader putShader(String name, Shader s) {
		return shaders.put(name.toLowerCase(), s);
	}

	public Shader getShader(String name) {
		return shaders.getOrDefault(name.toLowerCase(), null);
	}

	public Shader getOrPutShader(String name, Function<? super String, ? extends Shader> consumer) {
		return shaders.computeIfAbsent(name.toLowerCase(), consumer);
	}

	public boolean hasShader(String name) {
		return shaderPrograms.containsKey(name.toLowerCase());
	}

	public boolean hasShaderProgram(String name) {
		return shaderPrograms.containsKey(name.toLowerCase());
	}

	public boolean hasTexture(String name) {
		return images.containsKey(name.toLowerCase());
	}

	public boolean hasSound(String name) {
		return images.containsKey(name.toLowerCase());
	}
}
