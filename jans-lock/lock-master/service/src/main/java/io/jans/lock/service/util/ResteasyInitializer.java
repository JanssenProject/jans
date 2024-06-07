/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.lock.service.util;

import java.util.HashSet;
import java.util.Set;

import io.jans.lock.service.ws.rs.ConfigurationRestWebService;
import io.jans.lock.service.ws.rs.audit.AuditRestWebServiceImpl;
import io.jans.lock.service.ws.rs.sse.SseRestWebServiceImpl;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


/**
 * Integration with Resteasy
 * 
 * @author Yuriy Movchan Date: 06/06/2024
 */
@ApplicationPath("/v1")
public class ResteasyInitializer extends Application {	

	@Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(AuditRestWebServiceImpl.class);
        classes.add(ConfigurationRestWebService.class);
        classes.add(SseRestWebServiceImpl.class);

        return classes;
    }

}
