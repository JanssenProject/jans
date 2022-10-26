/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * @author yuriyz on 02/21/2017.
 */
@XmlEnum(String.class)
public enum CacheProviderType {
    IN_MEMORY, MEMCACHED, REDIS, NATIVE_PERSISTENCE
}
