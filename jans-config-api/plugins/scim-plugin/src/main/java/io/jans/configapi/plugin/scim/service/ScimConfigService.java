package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.configapi.plugin.scim.model.config.ScimAppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConf;
import io.jans.orm.PersistenceEntryManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigService {
    
    @Inject
    Logger log;
   
    @Inject
    PersistenceEntryManager persistenceManager;
    
    @Inject
    ScimConfigurationFactory scimConfigurationFactory;        
    
   
   public ScimAppConfiguration getConfiguration() {
        return scimConfigurationFactory.getAppConfiguration();
    }
   
   public ScimConf findConf() {
       final String dn = scimConfigurationFactory.getConfigurationDn();
       return persistenceManager.find(dn, ScimConf.class, null);
   }

   public void merge(ScimConf conf) {
       conf.setRevision(conf.getRevision() + 1);
       persistenceManager.merge(conf);
   }
   
   public ScimAppConfiguration find() {
       final ScimConf conf = findConf();
       return conf.getDynamicConf();
   }
    
 }
