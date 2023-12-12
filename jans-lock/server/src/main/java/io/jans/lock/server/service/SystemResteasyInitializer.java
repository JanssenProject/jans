/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.server.service;

import java.util.HashSet;
import java.util.Set;

import io.jans.lock.ws.rs.HealthCheckController;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan Date: 12/12/2023
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
