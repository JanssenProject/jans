package io.jans.configapi.plugin.scim.service;

import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.configapi.plugin.scim.model.config.AppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConfigurationEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigService {
    
    @Inject
    Logger log;
   
    @Inject
    PersistenceEntryManager persistenceManager;
    
    @Inject
    ScimConfigurationFactory scimConfigurationFactory;        
    
   
   public AppConfiguration getConfiguration() {
        return scimConfigurationFactory.getAppConfiguration();
    }
   
   public ScimConfigurationEntry findConf() {
       final String dn = scimConfigurationFactory.getConfigurationDn();
       return persistenceManager.find(dn, ScimConfigurationEntry.class, null);
   }

   public void merge(ScimConfigurationEntry conf) {
       conf.setRevision(conf.getRevision() + 1);
       persistenceManager.merge(conf);
   }
   
   
   public AppConfiguration find() {
       final ScimConfigurationEntry conf = findConf();
       return conf.getDynamicConf();
   }
   
   public String getPersistenceType() {
       return scimConfigurationFactory.getBaseConfiguration().getString("persistence.type");
   }
    
 }
