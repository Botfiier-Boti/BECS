package com.botifier.becs.config;

/**
 * Config Interface
 * @author Botifier
 *
 */
public interface Config {

	/**
	 * Interprets specified file into config
	 * @param file String File to read
	 * @return IConfig The loaded config
	 */
	public Config readFile(String file);
	
	/**
	 * Reads the config or returns the default
	 * @param file String File to read
	 * @param defaultConfig IConfig The default
	 * @return IConfig The result
	 */
	public Config readFileOrDefault(String file, Config defaultConfig);
	
	
	/**
	 * Writes config into file
	 * @param f String File to write to
	 */
	public void writeFile(String file);

}