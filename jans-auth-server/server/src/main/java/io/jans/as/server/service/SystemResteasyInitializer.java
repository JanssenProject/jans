/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.server.ws.rs.controller.HealthCheckController;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration with Resteasy
 *
 * @author Yuriy Movchan
 * @version 0.1, 11/13/2020
 */
@ApplicationPath("/sys")
public class SystemResteasyInitializer extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HealthCheckController.class);

        return classes;
    }

}