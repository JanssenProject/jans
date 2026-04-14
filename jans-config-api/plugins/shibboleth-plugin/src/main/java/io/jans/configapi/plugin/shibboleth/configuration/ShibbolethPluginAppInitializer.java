/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import org.slf4j.Logger;

@ApplicationScoped
@Named("shibbolethPluginAppInitializer")
public class ShibbolethPluginAppInitializer {

    @Inject
    Logger log;

    @Inject
    ShibbolethPluginConfigurationFactory shibbolethPluginConfigurationFactory;

    public void onAppStart() {
        log.info("=============  Initializing Shibboleth Plugin ========================");

        // configuration
        this.shibbolethPluginConfigurationFactory.create();

        log.info("==============  Shibboleth Plugin IS UP AND RUNNING ===================");
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  Shibboleth Plugin STOPPED  ==========================");
        log.info("init:{}", init);
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public ShibbolethPluginConfigurationFactory getShibbolethPluginConfigurationFactory() {
        return shibbolethPluginConfigurationFactory;
    }

}
