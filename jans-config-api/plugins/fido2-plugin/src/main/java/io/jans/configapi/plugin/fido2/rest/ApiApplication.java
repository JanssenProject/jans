package io.jans.configapi.plugin.fido2.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/fido2")
public class ApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();

        classes.add(Fido2ConfigResource.class);
        classes.add(Fido2RegistrationResource.class);
        
        return classes;
    }
}
