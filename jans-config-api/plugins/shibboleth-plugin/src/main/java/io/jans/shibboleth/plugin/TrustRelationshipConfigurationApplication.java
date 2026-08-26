package io.jans.shibboleth.plugin;

import java.util.HashSet;
import java.util.Set;

import io.jans.shibboleth.trust.api.config.rs.TrustRelationshipsConfigurationResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/v1/shibboleth/trust/config")
public class TrustRelationshipConfigurationApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> classes = new HashSet<>();
        classes.add(TrustRelationshipsConfigurationResource.class);
        return classes;
    }
}
