package com.botifier.becs.config;

/**
 * Config Interface
 * @author Botifier
 *
 */
public interface IConfig {

	/**
	 * Interprets specified file into config
	 * @param file String File to read
	 * @return IConfig The loaded config
	 */
	public IConfig readFile(String file);
	
	/**
	 * Reads the config or returns the default
	 * @param file String File to read
	 * @param defaultConfig IConfig The default
	 * @return IConfig The result
	 */
	public IConfig readFileOrDefault(String file, IConfig defaultConfig);
	
	
	/**
	 * Writes config into file
	 * @param f String File to write to
	 */
	public void writeFile(String file);

}