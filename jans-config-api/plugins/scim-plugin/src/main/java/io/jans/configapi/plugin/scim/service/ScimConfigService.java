package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.configapi.plugin.scim.model.config.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigService {
    
    @Inject
    Logger log;
   
    @Inject
    private PersistenceEntryManager persistenceEntryManager;
    
    @Inject
    ScimConfigurationFactory scimConfigurationFactory;        
    
   
   public AppConfiguration getConfiguration() {
        return scimConfigurationFactory.getAppConfiguration();
    }
    
 }
