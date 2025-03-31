/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.keycloak.link.model.config;

import jakarta.enterprise.inject.Vetoed;

/**
 * Cache refresh configuration
 *
 * @author Yuriy Movchan Date: 07.13.2011
 */
@Vetoed
public class LinkConfiguration extends io.jans.link.model.config.shared.LinkConfiguration implements Configuration {

    private KeycloakConfiguration keycloakConfiguration;

    public KeycloakConfiguration getKeycloakConfiguration() {
        return keycloakConfiguration;
    }

    public void setKeycloakConfiguration(KeycloakConfiguration keycloakConfiguration) {
        this.keycloakConfiguration = keycloakConfiguration;
    }

}
