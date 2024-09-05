/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.kc.link.util;

import io.jans.configapi.plugin.kc.link.model.config.KcLinkConfigSource;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class KcLinkUtil {

    @Inject
    Logger logger;

    @Inject
    KcLinkConfigSource kcLinkConfigSource;

    public Map<String, String> getProperties() {
        logger.debug("   KcLinkUtil - kcLinkConfigSource.getProperties():{}", kcLinkConfigSource.getProperties());
        return kcLinkConfigSource.getProperties();
    }

    public Set<String> getPropertyNames() {
        logger.debug("   KcLinkUtil - kcLinkConfigSource.getPropertyNames():{}", kcLinkConfigSource.getPropertyNames());
        return kcLinkConfigSource.getPropertyNames();
    }
}
