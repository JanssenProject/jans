package org.xdi.service.cache;

/**
 * @author yuriyz on 02/21/2017.
 */
public abstract class AbstractCacheProvider<T> extends CacheProvider<T> {

    public abstract void create();

    public abstract void destroy();
}
