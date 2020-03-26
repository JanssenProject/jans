package org.gluu.service.cache;

import java.util.Date;

public abstract class CacheProvider<T> implements CacheInterface {

    /**
     * @return - the cache the cache provider delegates to
     */
    public abstract T getDelegate();

    /*
     * Method to check if there is key in cache 
     */
    public abstract boolean hasKey(String key);

    /**
     * Fetches an object for the given key from the cache and returns it if found.
     * Only the specified cache region will be searched.
     *
     * @param key - a key to identify the object.
     * @return - the object if found or null if not
     */
    public abstract Object get(String key);

    /*
     * Put value with specified key without expiration 
     */
    @Deprecated
    public abstract void put(String key, Object object);

    public abstract void put(int expirationInSeconds, String key, Object object);

    /**
     * Removes an object from the cache. The object is removed from the specified
     * cache region under the given key.

     * @param key - a key to identify the object
     */
    public abstract void remove(String key);

    /**
     * Removes all objects from cache
     */
    public abstract void clear();

    /**
     * Clean objects from cache regions till specified date
     */
    public abstract void cleanup(final Date now);
	
	public abstract CacheProviderType getProviderType();

}
