package com.botifier.becs.util;

/**
 * Pair interface
 * @param <K>
 * @param <V>
 */
public interface Pair<K, V> {
    K key();
    V value();
    long hash();
    
    default String string() {
    	return key() + ": " + value();
    }
    
}