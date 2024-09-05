/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.configuration;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import org.slf4j.Logger;

@ApplicationScoped
@Named("samlAppInitializer")
public class SamlAppInitializer {

    @Inject
    Logger log;

    @Inject
    SamlConfigurationFactory samlConfigurationFactory;


    public void onAppStart() {
        log.info("=============  Initializing SAML Plugin ========================");

        // configuration
        this.samlConfigurationFactory.create();

        log.info("==============  SAML Plugin IS UP AND RUNNING ===================");
    }
  

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  SAML Plugin STOPPED  ==========================");
        log.info("init:{}", init);
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public SamlConfigurationFactory getSamlConfigurationFactory() {
        return samlConfigurationFactory;
    }

   
}
