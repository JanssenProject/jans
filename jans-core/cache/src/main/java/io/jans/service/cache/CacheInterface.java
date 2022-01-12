/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface CacheInterface {

    Object get(String key);

    void put(int expirationInSeconds, String key, Object object);

   void remove(String key);

   void clear();

    void cleanup(final Date now);
}
