package io.jans.configapi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TtlCache<K, V> {

    private static class CacheEntry<V> {
        V value;
        long expiryTime;
        long createdAt;

        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
            this.createdAt = System.currentTimeMillis();
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
        cache.put(key, entry);
        insertionOrder.offer(key);
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
            cache.remove(oldestKey);
        }
    }
}
