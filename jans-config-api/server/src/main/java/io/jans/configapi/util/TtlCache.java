package io.jans.configapi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TtlCache<K, V> {

    private static class CacheEntry<V> {
        V value;
        long expiryTime;

        /**
         * Creates a cache entry that holds the given value and will expire after the specified TTL.
         *
         * @param value the value to cache
         * @param ttlMillis time-to-live in milliseconds; the entry will expire ttlMillis after construction
         */
        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        /**
         * Determines whether the cache entry has passed its expiration timestamp.
         *
         * @return true if the current system time is greater than the entry's expiry time, false otherwise.
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();

    /**
     * Stores a value in the cache under the given key with the specified time-to-live.
     *
     * @param key the cache key
     * @param value the value to store
     * @param ttlMillis the time-to-live in milliseconds after which the entry is considered expired
     */
    public void put(K key, V value, long ttlMillis) {
        cache.put(key, new CacheEntry<>(value, ttlMillis));
    }

    /**
     * Retrieve the cached value for the given key if present and not expired.
     *
     * If the entry exists but has expired, it is removed from the cache before returning.
     *
     * @return the cached value associated with the key, or `null` if no entry exists or the entry has expired
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    /**
     * Remove the cache entry for the specified key.
     *
     * If no entry is associated with the key, this method does nothing.
     *
     * @param key the key whose mapping should be removed from the cache
     */
    public void remove(K key) {
        cache.remove(key);
    }
}