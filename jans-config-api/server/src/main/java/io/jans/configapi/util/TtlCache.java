package io.jans.configapi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TtlCache<K, V> {

    private static class CacheEntry<V> {
        private final V value;
        private final long expiryTime;

        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Queue<K> insertionOrder = new ConcurrentLinkedQueue<>();
    private final int maxSize;

    public TtlCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        this.maxSize = maxSize;
    }

    public void put(K key, V value, long ttlMillis) {
        evictIfNeeded();

        CacheEntry<V> entry = new CacheEntry<>(value, ttlMillis);
        CacheEntry<V> previous = cache.put(key, entry);
        if (previous == null) {
            insertionOrder.offer(key);
        }
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key, entry);
            return null;
        }

        return entry.value;
    }

    public void remove(K key) {
        cache.remove(key);
        // Note: key remains in insertionOrder for lazy cleanup during eviction
    }

    private void evictIfNeeded() {
        // 1. Remove expired entries
        cache.forEach((key, entry) -> {
            if (entry.isExpired()) {
                cache.remove(key, entry);
            }
        });

        // 2. Evict oldest entries if still over capacity
        while (cache.size() >= maxSize) {
            K oldestKey = insertionOrder.poll();
            if (oldestKey == null) {
                break;
            }
            // Only remove if still the original entry; skip if key was re-inserted
            CacheEntry<V> entry = cache.get(oldestKey);
            if (entry != null && entry.isExpired()) {
                cache.remove(oldestKey, entry);
            }
            // If entry exists and not expired, it was re-inserted; don't evict
        }
    }
}
