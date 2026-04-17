package com.botifier.becs.util.maps.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.botifier.becs.util.Pair;
import com.botifier.becs.util.records.PairNode;

/**
 * ImmutableHashMap
 * 
 * A high performance immutable hash map.
 * 
 * NOTE: Not resistant to hash collision attacks.
 * Intended for use with known, trusted key types only.
 * Do not use with user supplied keys or untrusted input.
 * 
 * AI Assisted
 * 
 * @author Botifier
 */
public record ImmutableHashMap<K, V>(PairNode<K, V>[] data, int size, float loadFactor, Function<Object, Long> hasher) implements Map<K, V> {
	
	public ImmutableHashMap {
		Objects.requireNonNull(data);
		if (size < 0)
			throw new IllegalArgumentException("Map cannot hold negative entries");
		if (size > data.length)
			throw new IllegalArgumentException(String.format("Map cannot hold more entries than it has array space:\nsize: %d\narray length:%d", size, data.length));
	}
	
	ImmutableHashMap(PairNode<K, V>[] data, int size, float loadFactor) {
		this(data, size, loadFactor, o -> (long) o.hashCode());
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> ImmutableHashMap<K, V> from(Map<K, V> source, Function<K, Long> hasher) {
        final int cap = nextPowerOfTwo(source.size() * 2);
        
		final PairNode<K, V>[] data = PairNode.newTable(cap);
		
        int size = 0;
        for (Entry<K, V> e : source.entrySet()) {
            if (e.getKey() == null) 
            	continue;
            final long hash = hash(e.getKey(), hasher);
            final int idx = idx(cap, hash);
            data[idx] = new PairNode<>(e.getKey(), e.getValue(), hash, data[idx]);
            size++;
        }
        
        return new ImmutableHashMap<>(data, size, 0.5f, (Function<Object, Long>)hasher);
    }
	
	public static <K, V> ImmutableHashMap<K, V> from(Map<K, V> source) {
        return from(source, null);
    }
	
	public ImmutableHashMap<K, V> with(@Nonnull K key, @Nonnull V value) {
	    final long hash = hash(key, hasher);
	    final int newSize = size + 1;
	    
	    if (newSize > data.length * loadFactor) 
	        return withAll(Map.of(key, value));
	    
	    final PairNode<K, V>[] newData = Arrays.copyOf(data, data.length);
	    final int idx = idx((data.length - 1), hash);
	    newData[idx] = new PairNode<>(key, value, hash, newData[idx]);
	    return new ImmutableHashMap<>(newData, newSize, loadFactor, hasher);
	}
	
	public ImmutableHashMap<K, V> withAll(Map<K, V> source) {
		if (source == null || source.size() == 0)
			return this;
		
	    final int newSize = size + source.size();
	    final int cap = nextPowerOfTwo((int)(newSize / loadFactor));
	    final PairNode<K, V>[] newData = cap == data.length 
	    							   ? Arrays.copyOf(data, data.length)
	    							   : PairNode.newTable(cap);
	    
	    // copy existing if resizing
	    if (cap != data.length) {
	        for (PairNode<K, V> node : data) {
	            PairNode<K, V> cur = node;
	            while (cur != null) {
	                final int idx = idx(cap-1, cur.hash());
	                newData[idx] = new PairNode<>(cur.key(), cur.value(), cur.hash(), newData[idx]);
	                cur = cur.next();
	            }
	        }
	    }
	    
	    // add new entries
	    for (Entry<K, V> e : source.entrySet()) {
	        if (e.getKey() == null) 
	        	continue;
	        final long hash = hash(e.getKey(), hasher);
	        final int idx = idx(cap-1, hash);
	        newData[idx] = new PairNode<>(e.getKey(), e.getValue(), hash, newData[idx]);
	    }
	    return new ImmutableHashMap<>(newData, newSize, loadFactor, hasher);
	}
	
	public ImmutableHashMap<K, V> without(K key) {
	    final long hash = hash(key, hasher);
	    final int idx = idx(data.length - 1, hash);
	    final PairNode<K, V> node = data[idx];
	    
	    if (node == null) return this;
	    
	    // not in map, nothing to do
	    if (!containsKey(key)) return this;
	    
	    final PairNode<K, V>[] newData = Arrays.copyOf(data, data.length);
	    
	    // rebuild the chain at idx excluding the target key
	    newData[idx] = null;
	    PairNode<K, V> cur = node;
	    while (cur != null) {
	        if (cur.hash() != hash || !cur.key().equals(key)) {
	            newData[idx] = new PairNode<>(cur.key(), cur.value(), cur.hash(), newData[idx]);
	        }
	        cur = cur.next();
	    }
	    
	    return new ImmutableHashMap<>(newData, size - 1, loadFactor, hasher);
	}

	public ImmutableHashMap<K, V> withoutAll(Set<K> keys) {
	    if (keys == null || keys.isEmpty()) return this;
	    
	    ImmutableHashMap<K, V> result = this;
	    for (K key : keys) {
	        result = result.without(key);
	    }
	    return result;
	}
	
	public ImmutableHashMap<K, V> withWithout(Map<K, V> additions, Map<K, Boolean> tombstones) {
	    if ((additions == null || additions.isEmpty()) && 
	        (tombstones == null || tombstones.isEmpty())) 
	        return this;
	    
	    // calculate new size
	    final long removals = tombstones == null ? 0 : 
	        tombstones.keySet().stream().filter(this::containsKey).count();
	    final long actualAdditions = additions == null ? 0 :
	        additions.keySet().stream().filter(k -> !tombstones.containsKey(k)).count();
	    final int newSize = (int)(size - removals + actualAdditions);
	    
	    final int cap = nextPowerOfTwo((int)(newSize / loadFactor));
	    final PairNode<K, V>[] newData = PairNode.newTable(cap);
	    
	    // copy existing entries excluding tombstones
	    for (PairNode<K, V> node : data) {
	        PairNode<K, V> cur = node;
	        while (cur != null) {
	            if (tombstones == null || !tombstones.containsKey(cur.key())) {
	                final int idx = idx(cap - 1, cur.hash());
	                newData[idx] = new PairNode<>(cur.key(), cur.value(), cur.hash(), newData[idx]);
	            }
	            cur = cur.next();
	        }
	    }
	    
	    // add new entries excluding tombstones
	    if (additions != null) {
	        for (Entry<K, V> e : additions.entrySet()) {
	            if (e.getKey() == null) continue;
	            if (tombstones != null && tombstones.containsKey(e.getKey())) continue;
	            final long hash = hash(e.getKey(), hasher);
	            final int idx = idx(cap - 1, hash);
	            newData[idx] = new PairNode<>(e.getKey(), e.getValue(), hash, newData[idx]);
	        }
	    }
	    
	    return new ImmutableHashMap<>(newData, newSize, loadFactor, hasher);
	}
	
	public static <K, V> ImmutableHashMap<K, V> of() {
	    return new ImmutableHashMap<>(PairNode.newTable(1), 0, 0.5f);
	}
	
	//Its not unchecked it's a downcast
	@SuppressWarnings("unchecked")
	public static <K, V> ImmutableHashMap<K, V> of(Function<K, Long> hasher) {
		if (hasher == null) return of();
		return new ImmutableHashMap<K, V>(PairNode.newTable(1), 0, 0.5f, (Function<Object, Long>) hasher);
	}
	
	@SafeVarargs
	public static <K, V> ImmutableHashMap<K, V> of(Pair<K, V>... pairs) {
	    return of(0.5f, pairs);
	}
	
	@SafeVarargs
	public static <K, V> ImmutableHashMap<K, V> of(float loadFactor, Pair<K, V>... pairs) {
		if (loadFactor <= 0 || loadFactor > 1)
		    throw new IllegalArgumentException("Load factor must be between 0 and 1 exclusive");
	    final int cap = nextPowerOfTwo((int)(pairs.length / loadFactor));
	    final PairNode<K, V>[] data = PairNode.newTable(cap);
	    int size = 0;
	    for (Pair<K, V> p : pairs) {
	        if (p == null || p.key() == null) 
	        	continue;
	        final int idx = idx(cap - 1, p.hash());
	        data[idx] = new PairNode<>(p.key(), p.value(), p.hash(), data[idx]);
	        size++;
	    }
	    return new ImmutableHashMap<>(data, size, loadFactor);
	}
	
	public V getByHash(long hash) {
		PairNode<K, V> node = data[idx(data.length - 1, hash)];
		
		while (node != null) {
			if (node.hash() == hash)
				return node.value();

			node = node.next();
		}

		return null;
	}

	@Override
	public V get(Object key) {
		final PairNode<K, V> first;
		final long hash = hashUnsafe(key, hasher);
		final int pos = data.length - 1;
		final int idx = idx(pos, hash);

		if ((first = data[idx]) != null) {
			if (match(first.key(), first.hash(), key, hash))
				return first.value();

			PairNode<K, V> next;
			if ((next = first.next()) != null) {
				do {
					if (match(next.key(), next.hash(), key, hash))
						return next.value();
				} while ((next = next.next()) != null);
			}
		}
		return null;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (PairNode<K, V> node : data) {
			PairNode<K, V> cur = node;
			while (cur != null) {
				if (cur.value() != null && cur.value().equals(value))
					return true;
				cur = cur.next();
			}
		}
		return false;
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<K> keySet() {
		final Set<K> keys = new HashSet<>(size);
		for (PairNode<K, V> node : data) {
			PairNode<K, V> cur = node;
			while (cur != null) {
				keys.add(cur.key());
				cur = cur.next();
			}
		}
		return Collections.unmodifiableSet(keys);
	}

	@Override
	public Collection<V> values() {
		final List<V> vals = new ArrayList<>(size);
		for (PairNode<K, V> node : data) {
			PairNode<K, V> cur = node;
			while (cur != null) {
				vals.add(cur.value());
				cur = cur.next();
			}
		}
		return Collections.unmodifiableList(vals);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		final Set<Entry<K, V>> entries = new HashSet<>(size);
		for (PairNode<K, V> node : data) {
			PairNode<K, V> cur = node;
			while (cur != null) {
				entries.add(Map.entry(cur.key(), cur.value()));
				cur = cur.next();
			}
		}
		return Collections.unmodifiableSet(entries);
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (!(obj instanceof Map<?, ?> other)) return false;
	    if (size() != other.size()) return false;
	    if (hashCode() != other.hashCode()) return false;

	    for (PairNode<K, V> node : data) {
	    	PairNode<K, V> cur = node;
	    	while (cur != null) {
	    		Object otherValue = other.get(cur.key());
	            if (!Objects.equals(cur.value(), otherValue)) {
	                return false;
	            }
	    		cur = cur.next();
	    	}
	    }
	    return true;
	}

	@Override
	public int hashCode() {
	    return Objects.hash(data, size);
	}

	private final boolean match(Object o1, long hash1, Object o2, long hash2) {
		if (hash1 != hash2) return false;
		if (o1 == o2) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}

	private static final int nextPowerOfTwo(int n) {
		if (n <= 1) return 1;
		return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
	}
	
	private static final int idx(int cap, long hash) {
		return  cap & (int) (hash ^ 0xFFFFFFFF);
	}
	
	private static final <K> long hash(K key, Function<K, Long> hasher) {
		return hasher != null ? hasher.apply(key) : key.hashCode();
	}
	
	private static final long hashUnsafe(Object key, Function<Object, Long> hasher) {
		return hasher != null ? hasher.apply(key) : key.hashCode();
	}
	
}
