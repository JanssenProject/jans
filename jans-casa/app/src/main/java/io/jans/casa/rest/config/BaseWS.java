package io.jans.casa.rest.config;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.core.PersistenceService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
class BaseWS {

    @Inject
    MainSettings mainSettings;

    @Inject
    ConfigurationHandler confHandler;

    @Inject
    PersistenceService persistenceService;
    
}
