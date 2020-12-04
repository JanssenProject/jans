/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

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
     * @see io.jans.service.cache.CacheProvider#cleanup(java.util.Date)
     */
    public void cleanup(final Date now) {}

}
