package org.gluu.service.cache;

import java.util.Date;

public abstract class CacheProvider<T> implements CacheInterface {

    /**
     * @return - the cache the cache provider delegates to
     */
    public abstract T getDelegate();

    /**
     * Fetches an object for the given key from the cache and returns it if found.
     * Only the specified cache region will be searched.
     *
     * @param key - a key to identify the object.
     * @return - the object if found or null if not
     */
    public abstract Object get(String key);

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
