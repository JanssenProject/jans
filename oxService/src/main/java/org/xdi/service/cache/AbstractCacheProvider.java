package org.xdi.service.cache;

import org.jboss.seam.cache.CacheProvider;

/**
 * @author yuriyz on 02/21/2017.
 */
public abstract class AbstractCacheProvider<T> extends CacheProvider<T> {

    public abstract void create();

    public abstract void destroy();
}
