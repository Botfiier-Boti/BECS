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

	private final static Map<String, Texture> images = new ConcurrentHashMap<String, Texture>();
	private final static Map<String, Sound> sounds = new ConcurrentHashMap<String, Sound>();
	private final static Map<String, ShaderProgram> shaderPrograms = new ConcurrentHashMap<String, ShaderProgram>();
	private final static Map<String, Shader> shaders = new ConcurrentHashMap<String, Shader>();
	
	public static Texture loadTexture(String name, String loc) {
		return images.put(name.toLowerCase(), Texture.loadTexture(loc));
	}
	
	public static Texture loadOrGetTexture(String name, String loc) {
		return images.computeIfAbsent(name.toLowerCase(), t -> Texture.loadTexture(loc));
	}
	
	public static Texture putTexture(String name, Texture t) {
		return images.put(name.toLowerCase(), t);
	}
	
	public static Texture getTexture(String name) {
		return images.getOrDefault(name.toLowerCase(), null);
	}
	
	public static Texture getOrPutTexture(String name, Function<? super String, ? extends Texture> consumer) {
		return images.computeIfAbsent(name.toLowerCase(), consumer);
	}
	
	public static Sound loadSound(String name, String loc) {
		return sounds.put(name.toLowerCase(), Sound.createSound(loc, false, false));
	}
	
	public static Sound loadSound(String name, String loc, boolean loop, boolean relative) {
		return sounds.put(name.toLowerCase(), Sound.createSound(loc, loop, relative));
	}
	
	public static Sound loadOrGetSound(String name, String loc) {
		return loadOrGetSound(name.toLowerCase(), loc, false, false);
	}
	
	public static Sound loadOrGetSound(String name, String loc, boolean loop, boolean relative) {
		return sounds.computeIfAbsent(name.toLowerCase(), s -> Sound.createSound(loc, loop, relative));
	}
	
	public static Sound putSound(String name, Sound s) {
		return sounds.put(name.toLowerCase(), s);
	}
	
	public static Sound getSound(String name) {
		return sounds.getOrDefault(name.toLowerCase(), null).copy();
	}
	
	public static Sound getOrPutSound(String name, Function<? super String, ? extends Sound> consumer) {
		return sounds.computeIfAbsent(name, consumer);
	}
	
	public static ShaderProgram putShaderProgram(String name, ShaderProgram sp) {
		return shaderPrograms.put(name.toLowerCase(), sp);
	}
	
	public static ShaderProgram getShaderProgram(String name) {
		return shaderPrograms.getOrDefault(name.toLowerCase(), null);
	}
	
	public static ShaderProgram getOrPutShaderProgram(String name, Function<? super String, ? extends ShaderProgram> consumer) {
		return shaderPrograms.computeIfAbsent(name.toLowerCase(), consumer);
	}
	
	
	public static Shader loadOrGetShader(String name, int shaderType, String location) {
		return shaders.computeIfAbsent(name, s -> Shader.loadShader(shaderType, location));
	}
	
	public static Shader loadShader(String name, int shaderType, String location) {
		return shaders.put(name, Shader.loadShader(shaderType, location));
	}
	
	public static Shader putShader(String name, Shader s) {
		return shaders.put(name.toLowerCase(), s);
	}
	
	public static Shader getShader(String name) {
		return shaders.getOrDefault(name.toLowerCase(), null);
	}
	
	public static Shader getOrPutShader(String name, Function<? super String, ? extends Shader> consumer) {
		return shaders.computeIfAbsent(name.toLowerCase(), consumer);
	}
	
	public static boolean hasShader(String name) {
		return shaderPrograms.containsKey(name.toLowerCase());
	}
	
	public static boolean hasShaderProgram(String name) {
		return shaderPrograms.containsKey(name.toLowerCase());
	}
	
	public static boolean hasTexture(String name) {
		return images.containsKey(name.toLowerCase());
	}
	
	public static boolean hasSound(String name) {
		return images.containsKey(name);
	}
}
