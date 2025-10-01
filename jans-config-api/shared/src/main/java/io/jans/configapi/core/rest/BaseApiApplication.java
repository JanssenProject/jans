/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.rest;

import io.jans.configapi.core.configuration.ObjectMapperContextResolver;

import java.util.Set;
import jakarta.ws.rs.core.Application;

public class BaseApiApplication extends Application {

    public Set<Class<?>> addCommonClasses(Set<Class<?>> classes) {
        if (classes == null) {
            return classes;
        }

        // General Application level class
        classes.add(ObjectMapperContextResolver.class);

        return classes;
    }
}
