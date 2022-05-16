/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.rest;

import io.jans.ca.server.op.GetIssuerOperation;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
public class ApiApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ApiApplication.class);

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        LOG.info("--------------------------------DEPLOYING REST RESOURCES-------------------------------------------");
        classes.add(HealthCheckResource.class);

        classes.add(OAuth20Resource.class);
        classes.add(OpenIdConnectResource.class);
        classes.add(UMA2ResourceServerResource.class);
        classes.add(UMA2RelyingPartyResource.class);

        classes.add(RpResource.class);

        return classes;
    }
}
