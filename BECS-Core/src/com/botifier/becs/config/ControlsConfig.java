package com.botifier.becs.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.botifier.becs.util.Input;

/**
 * Controls Config
 * @author Botifier
 *
 */
public class ControlsConfig implements IConfig {

	/**
	 * HashMap mapping keys to specific controls
	 */
	private static HashMap<String, List<Integer>> controlsConfig = new HashMap<>();

	/**
	 * Returns all controls by name
	 * @param name To search
	 * @return List of Key Codes
	 */
	public static List<Integer> getControl(String name) {
		return controlsConfig.get(name.toLowerCase()); //Changes the name parameter to lower-case so that it becomes case-insensitive
	}

	/**
	 * Checks whether or not a key tied to the specified control was pressed
	 * @param i Input manager
	 * @param name To check
	 * @return Boolean whether a key was pressed pertaining to specified command
	 */
	public static boolean pressed(Input i, String name) {
		if (!controlsConfig.containsKey(name.toLowerCase())) { //Checks if the name exists on the map
			return false;
		}
		//Gets the list of key-codes assigned to a control name
		List<Integer> l = controlsConfig.get(name.toLowerCase());
		for (int in : l) {
			if (i.isKeyPressed(in)) { //Returns true if one of the key-codes is pressed
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether or not a key tied to the specified control is down
	 * @param i Input manager
	 * @param name
	 * @return Boolean whether a key is down pertaining to specified command
	 */
	public static boolean down(Input i, String name) {
		if (!controlsConfig.containsKey(name.toLowerCase())) { //Checks if the name exists on the map
			return false;
		}
		//Gets the list of key-codes assigned to a control name
		List<Integer> l = controlsConfig.get(name.toLowerCase());
		for (int in : l) {
			if (i.isKeyDown(in)) { //Returns true if one of the key-codes is held-down
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if specified key code is valid within the specified command name
	 * @param name To check
	 * @param key_code To check
	 * @return Whether or not the key code exists within the specified command
	 */
	public static boolean keyValid(String name, int key_code) {
		if (!controlsConfig.containsKey(name.toLowerCase())) { //Checks if the name exists on the map
			return false;
		}
		//Gets the list of key-codes assigned to a control name
		List<Integer> l = controlsConfig.get(name.toLowerCase());
		for (int i : l) {
			if (i == key_code) { //Returns true if the key is assigned to the supplied control name
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds one or more key codes to specified command name
	 * @param name Where to place
	 * @param keys keys to add
	 */
	public static void addControl(String name, int... keys) {
		//Checks if the control name already exists
		if (controlsConfig.containsKey(name.toLowerCase())) {
			//Adds all of the supplied keys to the code's list
			for (int i : keys) {
				controlsConfig.get(name.toLowerCase()).add(i);
			}
		} else {
			//Creates a new ArrayList for storing key-codes
			List<Integer> l = new ArrayList<>();
			//Add the keys to the list
			for (int i : keys) {
				l.add(i);
			}
			//Places the control name and list of codes to the map
			controlsConfig.put(name.toLowerCase(), l);
		}
	}

	/**
	 * Removes a key command by name
	 * @param name To remove
	 */
	public static void removeControl(String name) {
		controlsConfig.remove(name.toLowerCase());
	}

	/**
	 * Unfinished File reader
	 */
	@Override
	public ControlsConfig readFile(String f) {
		//TODO: Remember to write this
		return null;
	}
	
	@Override
	public ControlsConfig readFileOrDefault(String f, IConfig defaultConfig) {
		return null;
	}

	@Override
	public void writeFile(String f) {
		// TODO Auto-generated method stub
		
	}
}