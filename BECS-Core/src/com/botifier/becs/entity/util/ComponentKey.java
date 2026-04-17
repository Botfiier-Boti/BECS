package com.botifier.becs.entity.util;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record ComponentKey<T>(String name, Class<T> type) {
	/**
	 * Caches the compatibility of certain classes
	 */
	private static final ConcurrentHashMap<Class<?>, Set<Class<?>>> compatabilityCache = new ConcurrentHashMap<>();

	public ComponentKey {
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);
	}

	@Override
	public String toString() {
		return "ComponentKey{name=%s, type=%s}".formatted(name, type.getSimpleName());
	}

	/**
	 * Checks if a class is compatible with another and caches that information if
	 * so.
	 * 
	 * @param expected Class<?> Class that actual should be compatible with
	 * @param actual   Class<?> Class to check
	 * @return boolean Whether or not expected can be assigned actual
	 */
	public boolean isCompatibleType(Class<?> actual) {
		Set<Class<?>> compat = compatabilityCache.computeIfAbsent(type(), type -> {
			Set<Class<?>> compatibleTypes = ConcurrentHashMap.newKeySet();
			compatibleTypes.add(type);
			compatibleTypes.add(resolveType(type));
			return compatibleTypes;
		});

		boolean contains = compat.contains(actual);
		if (!contains && type().isAssignableFrom(actual)) {
			compat.add(actual);
			contains = true;
		}

		return contains;
	}
	
	private static Class<?> resolveType(Class<?> clazz) {
		if (clazz.isPrimitive())
			return getWrapperClass(clazz);
		return getPrimitiveClass(clazz);
	}

	private static Class<?> getWrapperClass(Class<?> primitiveType) {
		if (primitiveType == int.class)
			return Integer.class;
		if (primitiveType == long.class)
			return Long.class;
		if (primitiveType == float.class)
			return Float.class;
		if (primitiveType == double.class)
			return Double.class;
		if (primitiveType == boolean.class)
			return Boolean.class;
		if (primitiveType == char.class)
			return Character.class;
		if (primitiveType == byte.class)
			return Byte.class;
		if (primitiveType == short.class)
			return Short.class;
		return primitiveType;
	}

	/**
	 * Returns the primitive version of primitive wrapper classes TODO: Move this
	 * somewhere else
	 * 
	 * @param primitiveType Class<?> Wrapper class
	 * @return Class<?> The primitive version of the wrapper
	 */
	private static Class<?> getPrimitiveClass(Class<?> primitiveType) {
		if (primitiveType == Integer.class)
			return int.class;
		if (primitiveType == Long.class)
			return long.class;
		if (primitiveType == Float.class)
			return float.class;
		if (primitiveType == Double.class)
			return double.class;
		if (primitiveType == Boolean.class)
			return boolean.class;
		if (primitiveType == Character.class)
			return char.class;
		if (primitiveType == Byte.class)
			return byte.class;
		if (primitiveType == Short.class)
			return short.class;
		return primitiveType;
	}
}
