/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import javax.xml.bind.annotation.XmlEnum;

/**
 * @author yuriyz on 02/21/2017.
 */
@XmlEnum(String.class)
public enum CacheProviderType {
    IN_MEMORY, MEMCACHED, REDIS, NATIVE_PERSISTENCE
}
