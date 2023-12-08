package io.jans.configapi.plugin.saml.extensions;

import io.jans.configapi.plugin.saml.configuration.SamlAppInitializer;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;

public class SamlExtension implements Extension {
    
    @Inject
    private Logger log;
    
    @Inject 
    SamlAppInitializer samlAppInitializer;
    
    @PostConstruct
    public void init() {
        log.info("\n\n\n Initializing SamlExtension \n\n\n ");
        
        samlAppInitializer.onAppStart();
        
        log.info("\n\n\n Post Initializing SamlExtension \n\n\n ");
    }
}
