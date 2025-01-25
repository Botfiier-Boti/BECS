package com.botifier.becs.util;

/**
 * A runnable with a target
 * NOTE: Not actually a runnable
 * @param <T> The type of origin and target
 */
public interface ParameterizedRunnable<T>{

	public void run(T origin, T target);

}
