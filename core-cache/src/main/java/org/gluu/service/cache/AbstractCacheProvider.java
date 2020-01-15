package org.gluu.service.cache;

import java.util.Date;

/**
 * @author yuriyz on 02/21/2017.
 */
public abstract class AbstractCacheProvider<T> extends CacheProvider<T> {

    public abstract void create();

    public abstract void destroy();

    /*
     * Default clean up haven't any logic because it uses specialized server to store data
     * 
     * @see org.gluu.service.cache.CacheProvider#cleanup(java.util.Date)
     */
    public void cleanup(final Date now) {}

}
