package com.botifier.becs.config;

import java.io.File;
import java.util.HashMap;

/**
 * Physics Config
 * @author Botifier
 *
 */
public class PhysicsConfig implements IConfig {

	/**
	 * Stores values pertaining to PhysicsSystem
	 */
	private static HashMap<String, Object> values = new HashMap<>();

	/**
	 * Check if map contains value
	 * @param s Value to check
	 * @return Whether or not the value exists within the map
	 */
	public static boolean contains(String s) {
		return values.containsKey(s.toLowerCase());
	}

	/**
	 * Places value at specified name
	 * @param s Where to place
	 * @param value To place
	 */
	public static void put(String s, Object value) {
		values.put(s.toLowerCase(), value);
	}

	/**
	 * Places value if it is absent
	 * @param s Where to place
	 * @param value To place
	 */
	public static void putIfAbsent(String s, Object value) {
		values.putIfAbsent(s.toLowerCase(), value);
	}

	/**
	 * Generic getValue
	 * @param <T> Class type to return
	 * @param name Where to look
	 * @return The value cast as T
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getT(String name) throws ClassCastException {
		Object o = getValue(name); //I wish casting generic classes was less of a pain

		return (T) o;
	}

	/**
	 * Gets value as an object
	 * @param name Where to look
	 * @return Value as an object
	 */
	public static Object getValue(String name) {
		if (!contains(name)) {
			System.out.println(String.format("ERROR: %s does not exist.", name));
			throw new NullPointerException();
		}
		return values.get(name.toLowerCase());
	}

	/**
	 * Gets the class type of value at specified location
	 * @param name Where to look
	 * @return
	 */
	public static Class<?> getValueType(String name) {
		Object o = getValue(name);
		return o != null ? o.getClass() : null;
	}

	/**
	 * Returns value as a string
	 * @param name Where to look
	 * @return Value as a String
	 * @throws ClassCastException
	 */
	public static String getString(String name) throws ClassCastException{
		return (String) getValue(name);
	}
	/**
	 * Returns value as a char
	 * @param name Where to look
	 * @return Value as a char
	 * @throws ClassCastException
	 */
	public static char getChar(String name) throws ClassCastException{
		return (char) getValue(name);
	}
	/**
	 * Returns value as a double
	 * @param name Where to look
	 * @return Value as a double
	 * @throws ClassCastException
	 */
	public static double getDouble(String name) throws ClassCastException{
		return (double) getValue(name);
	}
	/**
	 * Returns value as a float
	 * @param name Where to look
	 * @return Value as a float
	 * @throws ClassCastException
	 */
	public static float getFloat(String name) throws ClassCastException{
		return (float) getValue(name);
	}
	/**
	 * Returns value as a long
	 * @param name Where to look
	 * @return Value as a long
	 * @throws ClassCastException
	 */
	public static long getLong(String name) throws ClassCastException {
		return (long) getValue(name);
	}
	/**
	 * Returns value as a integer
	 * @param name Where to look
	 * @return Value as an int
	 * @throws ClassCastException
	 */
	public static int getInteger(String name) throws ClassCastException {
		return (int) getValue(name);
	}
	/**
	 * Returns value as a short
	 * @param name Where to look
	 * @return Value as a short
	 * @throws ClassCastException
	 */
	public static short getShort(String name) throws ClassCastException {
		return (short) getValue(name);
	}
	/**
	 * Returns value as a byte
	 * @param name Where to look
	 * @return Value as a byte
	 * @throws ClassCastException
	 */
	public static int getByte(String name) throws ClassCastException {
		return (byte) getValue(name);
	}

	/**
	 * Returns value as a boolean
	 * @param name Where to look
	 * @return Value as a boolean
	 * @throws ClassCastException
	 */
	public static boolean getBoolean(String name) throws ClassCastException {
		return (boolean) getValue(name);
	}

	/**
	 * Checks if specified object is the same as the one at location name
	 * @param name Where to look
	 * @param check To check
	 * @return Whether they point to the same object or not
	 */
	public static boolean is(String name, Object check) {
		Object re = getValue(name);
		return re != null && (check == re || check.equals(re)) ? true : false;
	}

	/**
	 * Lists all of the values within the HashMap
	 * @return String listing all of the values in the HashMap
	 */
	public static String listValues() {
		StringBuilder sb = new StringBuilder("-=Physics Config=-\n");
		
		for (String s2 : values.keySet()) {
			sb.append(String.format("%s : (%s) %s,\n", s2, getValueType(s2).getSimpleName(), getValue(s2)));
		}
		return sb.toString();
	}

	/**
	 * Unfinished file reader
	 */
	@Override
	public void readFile(File f) {
		//TODO: Actually write this
	}

	@Override
	public String toString() {
		return listValues();
	}
}
