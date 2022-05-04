/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.resource;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import io.jans.notify.service.HealthCheckController;

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