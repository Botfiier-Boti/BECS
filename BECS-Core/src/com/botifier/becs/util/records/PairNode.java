package com.botifier.becs.util.records;

import com.botifier.becs.util.Pair;

/**
 * Pair Node record, use PairNode(K, V) unless you have a pre-computed hash
 * @param <K>
 * @param <V>
 */
public record PairNode<K, V>(K key, V value, long hash, PairNode<K,V> next) implements Pair<K, V> {
	
    public PairNode(K key, V value) {
        this(key, value,  key.hashCode(), null);
    }
    
    @Override
    public String toString() {
    	return string();
    }
    
    @Override
	public int hashCode() {
        return key.hashCode() ^ value.hashCode();
	}
    
    @SuppressWarnings("unchecked")
    public static <K, V> PairNode<K, V>[] newTable(int cap) {
        return new PairNode[cap];
    }
}