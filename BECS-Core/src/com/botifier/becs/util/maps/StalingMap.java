package com.botifier.becs.util.maps;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.botifier.becs.util.maps.immutable.ImmutableHashMap;

/**
 * StalingMap
 * 
 * A thread safe map that uses an ImmutableHashMap as a read-optimized
 * backing store, with a mutable ConcurrentHashMap for writes.
 * Periodically rebuilds the immutable backing store to purge tombstones
 * and reclaim memory.
 * 
 * Does not support keys with null values, returns null when the key does not exist
 * 
 * WARNING: equality checks are not supported
 * 
 * AI Assisted
 * 
 * @author Botifier
 */
public class StalingMap<K, V> implements Map<K, V> {
	//Public so that for whatever nutjob wants to get the hashCode can disable this if they actually want to use it.
	public static AtomicBoolean warningMessageDisplayed = new AtomicBoolean(false);
	
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
	private static final int DEFAULT_STALE = 2000;
    private static final VarHandle SNAPSHOT;
    private static final VarHandle STALING;
    

    static {
        try {
            final MethodHandles.Lookup l = MethodHandles.lookup();
            SNAPSHOT = l.findVarHandle(StalingMap.class, "snapshot", ImmutableHashMap.class);
            STALING = l.findVarHandle(StalingMap.class, "staling", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile ImmutableHashMap<K, V> snapshot;
    private volatile boolean staling;

    private final ConcurrentHashMap<K, V> mutable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Boolean> tombstones = new ConcurrentHashMap<>();
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private final int purgeThreshold;

    public StalingMap(int purgeThreshold, Function<K,Long> hasher) {
        this.purgeThreshold = purgeThreshold;
        this.snapshot = ImmutableHashMap.of(hasher);
    }
    
    public StalingMap(Function<K,Long> hasher) {
        this(DEFAULT_STALE, hasher);
    }
    
    public StalingMap() {
        this(DEFAULT_STALE, null);
    }

    @Override
    public V get(Object key) {
    	waitStale();
        if (tombstones.containsKey(key)) return null;
        final V mutableVal = mutable.get(key);
        if (mutableVal != null) return mutableVal;
        return snapshot.get(key);
    }

    @Override
    public V put(K key, V value) {
    	waitStale();
    	
    	if (value == null)
    		return remove(key);
    	
        tombstones.remove(key);
        final V newVal = mutable.put(key, value);
        purgeCheck();
        return newVal; 
    }

    @SuppressWarnings("unchecked")
	@Override
    public V remove(Object key) {
        final V old = get(key);
        tombstones.put((K) key, Boolean.TRUE);
        purgeCheck();
        return old;
    }
    
    private final void purgeCheck() {
    	if (purgeThreshold > 0 && updateCount.incrementAndGet() >= purgeThreshold) {
            updateCount.set(0);
            stale();
        }
    }

    @SuppressWarnings("unused")
	public final void stale() {
        if (!((boolean) STALING.compareAndSet(this, false, true))) return;
        
        CompletableFuture.runAsync(() -> {
        	final ImmutableHashMap<K, V> fresh = snapshot.withWithout(mutable, tombstones);
        	
        	ImmutableHashMap<K, V> current;
        	do {
        		current = this.snapshot;  
            } while (!SNAPSHOT.compareAndSet(this, current, fresh));
        	
        	mutable.clear();
        	tombstones.clear();
    	}, EXECUTOR).whenComplete((l, i) -> {//The CICD didn't like the unnamed parameters
        	updateCount.getAndSet(0);
            STALING.setVolatile(this, false);
    	});
    }

    @Override
    public boolean containsKey(Object key) {
        if (tombstones.containsKey(key)) return false;
        return mutable.containsKey(key) || snapshot.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mutable.containsValue(value) || snapshot.containsValue(value);
    }

    @Override
    public int size() {
        return snapshot.size() + mutable.size() - tombstones.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        waitStale();
        m.forEach(this::put);
    }

    @Override
    public void clear() {
    	waitStale();
        mutable.clear();
        tombstones.clear();
        SNAPSHOT.setVolatile(this, ImmutableHashMap.of());
    }

    @Override
    public Set<K> keySet() {
        final Set<K> keys = ConcurrentHashMap.newKeySet();
        snapshot.keySet().stream()
                .filter(k -> !tombstones.containsKey(k))
                .forEach(keys::add);
        mutable.keySet().stream()
               .filter(k -> !tombstones.containsKey(k))
               .forEach(keys::add);
        return keys;
    }

    @Override
    public Collection<V> values() {
        return keySet().stream()
                       .map(this::get)
                       .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> entries = ConcurrentHashMap.newKeySet();
        keySet().forEach(k -> entries.add(Map.entry(k, get(k))));
        return entries;
    }
    
    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException(
            """
        	equals() is not supported on StalingMap.  
            This class is for high-performance spatial data only.
            Compare contents manually if needed."""
        );
    }

    @Override
    public int hashCode() {
    	if (!warningMessageDisplayed.compareAndSet(false, true))
    		System.out.println("""
    					WARNING: You should not be running this function it forces a stale. 
    					THIS WARNING WILL NOT BE SENT AGAIN FOR THE DURATION THIS RUNTIME""");
        
    	//Force a stale and wait for it, you should not have run this
    	stale();
    	waitStale();
    	
    	// Return the current snapshot's hashCode so at least it's consistent with itself
        final ImmutableHashMap<K, V> current = this.snapshot;
        return current != null ? current.hashCode() : 0;
    }
    
    public final void waitStale() {
    	while (staling) {
    		Thread.onSpinWait();
    	}
    }
}
