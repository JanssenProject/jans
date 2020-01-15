package org.gluu.service.cache;

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
