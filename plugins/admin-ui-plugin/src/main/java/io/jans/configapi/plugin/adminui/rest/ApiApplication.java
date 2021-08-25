/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.adminui.rest;

import io.jans.configapi.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.configapi.plugin.adminui.rest.license.LicenseResource;
import io.jans.configapi.plugin.adminui.rest.logging.AuditLoggerResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationPath("/")
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();

        // General
        classes.add(OAuth2Resource.class);
        classes.add(AuditLoggerResource.class);
        classes.add(LicenseResource.class);
        return classes;
    }
}
