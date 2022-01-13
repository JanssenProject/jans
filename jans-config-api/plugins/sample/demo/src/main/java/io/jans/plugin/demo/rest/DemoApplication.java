package io.jans.plugin.demo.rest;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath("/api/v1")
public class DemoApplication extends Application {
    
        @Override
        public Set<Class<?>> getClasses() {
            HashSet<Class<?>> classes = new HashSet<Class<?>>();

            // General
            classes.add(DemoResource.class);
             return classes;
        }
    }
