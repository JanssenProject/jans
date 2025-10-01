/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.util;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import io.jans.configapi.plugin.lock.model.config.LockConfigSource;

@ApplicationScoped
public class LockUtil {

    @Inject
    Logger logger;

    @Inject
    LockConfigSource lockConfigSource;

    public Map<String, String> getProperties() {
        logger.debug("   LockUtil - lockConfigSource.getProperties():{}", lockConfigSource.getProperties());
        return lockConfigSource.getProperties();
    }

    public Set<String> getPropertyNames() {
        logger.debug("   LockUtil - lockConfigSource.getPropertyNames():{}", lockConfigSource.getPropertyNames());
        return lockConfigSource.getPropertyNames();
    }
}
