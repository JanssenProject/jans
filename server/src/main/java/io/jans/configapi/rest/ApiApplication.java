/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest;

import io.jans.configapi.configuration.ObjectMapperContextResolver;
import io.jans.configapi.rest.resource.auth.*;
import io.jans.configapi.rest.resource.scim.*;
import io.jans.configapi.rest.health.ApiHealthCheck;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationPath("/api/v1")
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();

        // General
        classes.add(ObjectMapperContextResolver.class);
        classes.add(ApiHealthCheck.class);

        // oAuth Config
        classes.add(AcrsResource.class);
        classes.add(AttributesResource.class);
        classes.add(CacheConfigurationResource.class);
        classes.add(ClientsResource.class);
        classes.add(ConfigResource.class);
        classes.add(ConfigSmtpResource.class);
        classes.add(CouchbaseConfigurationResource.class);
        classes.add(CustomScriptResource.class);
        classes.add(Fido2ConfigResource.class);
        classes.add(JwksResource.class);
        classes.add(LdapConfigurationResource.class);
        classes.add(LoggingResource.class);
        classes.add(ScopesResource.class);
        classes.add(UmaResourcesResource.class);
        classes.add(StatResource.class);
        
        //scim
        //classes.add(ScimResource.class);

        return classes;
    }
}
