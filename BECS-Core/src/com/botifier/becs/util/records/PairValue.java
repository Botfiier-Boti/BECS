package com.botifier.becs.util.records;

import com.botifier.becs.util.Pair;

/**
 * Pair Value  record, use Pair(K, V) unless you have a pre-computed hash
 * @param <K>
 * @param <V>
 */
public record PairValue<K, V>(K key, V value, long hash) implements Pair<K, V> {
	
	public PairValue(K key, V value) {
		this(key, value, key.hashCode());
	}
	
	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}
	
	@Override
	public String toString() {
		return string();
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> PairValue<K, V>[] newTable(int cap) {
		return new PairValue[cap];
	}
}
