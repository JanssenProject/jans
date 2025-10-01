/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.util;

import io.jans.configapi.plugin.fido2.model.config.Fido2ConfigSource;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class Fido2Util {

    @Inject
    Logger logger;

    @Inject
    Fido2ConfigSource fido2ConfigSource;

    public Map<String, String> getProperties() {
        logger.debug("   Fido2Util - fido2ConfigSource.getProperties():{}", fido2ConfigSource.getProperties());
        return fido2ConfigSource.getProperties();
    }

    public Set<String> getPropertyNames() {
        logger.debug("   Fido2Util - ido2ConfigSource.getPropertyNames():{}", fido2ConfigSource.getPropertyNames());
        return fido2ConfigSource.getPropertyNames();
    }
}
