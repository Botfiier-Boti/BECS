package com.botifier.becs.util;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.botifier.becs.graphics.images.Texture;
import com.botifier.becs.graphics.shader.ShaderProgram;
import com.botifier.becs.sound.Sound;

public class ResourceManager {

	private final static Map<String, Texture> images = new ConcurrentHashMap<String, Texture>();
	private final static Map<String, Sound> sounds = new ConcurrentHashMap<String, Sound>();
	private final static Map<String, ShaderProgram> shaders = new ConcurrentHashMap<String, ShaderProgram>();
	
	public static void loadTexture(String name, String loc) {
		images.put(name.toLowerCase(), Texture.loadTexture(loc));
	}
	
	public static void putTexture(String name, Texture t) {
		images.put(name.toLowerCase(), t);
	}
	
	public static Texture getTexture(String name) {
		return images.getOrDefault(name.toLowerCase(), null);
	}
	
	public static void putSound(String name, Sound s) {
		sounds.put(name.toLowerCase(), s);
	}
	
	public static Sound getSound(String name) {
		return sounds.getOrDefault(name.toLowerCase(), null).copy();
	}
	
	public static void putShaderProgram(String name, ShaderProgram sp) {
		shaders.put(name.toLowerCase(), sp);
	}
	
	public static ShaderProgram getShader(String name) {
		return shaders.getOrDefault(name.toLowerCase(), null);
	}
}
