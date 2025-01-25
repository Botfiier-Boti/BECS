package com.botifier.becs.config;

import java.io.File;

/**
 * Config Interface
 * @author Botifier
 *
 */
public interface IConfig {

	/**
	 * Interprets specified file into config
	 * @param f File to read
	 */
	public void readFile(File f);

}