package org.gluu.service.cache;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author yuriyz on 02/21/2017.
 */
@XmlEnum(String.class)
public enum CacheProviderType {
    IN_MEMORY, MEMCACHED, REDIS, NATIVE_PERSISTENCE
}
