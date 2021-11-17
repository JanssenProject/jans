package com.spl.plugin.helloworld.ext;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Extension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldExtension implements Extension {
    
    private static final Logger log = LoggerFactory.getLogger(HelloWorldExtension.class.getName());
    
    void AfterDeploymentValidation(
            @Observes AfterDeploymentValidation adv) {
        log.debug("HelloWorldExtension::AfterDeploymentValidation() ********* ");
     }

    
}
