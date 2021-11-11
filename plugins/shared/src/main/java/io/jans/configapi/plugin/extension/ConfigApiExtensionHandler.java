package io.jans.configapi.plugin.extension;

import javax.enterprise.inject.spi.Extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigApiExtensionHandler implements Extension {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigApiExtensionHandler.class.getName());

    private Map<String, ConfigApiExtension> configApiExtensions = new HashMap<String, ConfigApiExtension>();

    public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat) {
        final AnnotatedTypeConfigurator<X> cat = pat.configureAnnotatedType();

      
    }
    
}
