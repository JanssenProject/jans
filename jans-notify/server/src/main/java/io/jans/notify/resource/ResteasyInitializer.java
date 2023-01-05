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

import io.jans.notify.rest.MetadataRestServiceImpl;
import io.jans.notify.rest.NotifyRestServiceImpl;

/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@ApplicationPath("/restv1")
public class ResteasyInitializer extends Application {	

	@Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(MetadataRestServiceImpl.class);
        classes.add(NotifyRestServiceImpl.class);

        return classes;
    }

}