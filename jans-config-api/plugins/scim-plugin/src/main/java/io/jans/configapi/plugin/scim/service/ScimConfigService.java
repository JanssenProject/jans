package io.jans.configapi.plugin.scim.service;

//import io.jans.configapi.plugin.scim.configuration.ScimConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.scim.model.GluuConfiguration;

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
        
    
    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
   public GluuConfiguration getConfiguration() {
        return getConfiguration(null);
    }
    
    
    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
    public GluuConfiguration getConfiguration(String[] returnAttributes) {
        GluuConfiguration result = null;
        result = persistenceEntryManager.find(getDnForConfiguration(), GluuConfiguration.class, returnAttributes);
        return result;
    }
    
    /**
     * Update configuration entry
     * 
     * @param configuration
     *            GluuConfiguration
     */
  public void updateConfiguration(GluuConfiguration configuration) {
        try {
            persistenceEntryManager.merge(configuration);
        } catch (Exception e) {
            log.info("", e);
        }
    }
    
    /**
     * Build DN string for configuration
     * 
     * @return DN string for specified configuration or DN for configurations branch
     *         if inum is null
     * @throws Exception
     */
    public String getDnForConfiguration() {
        //String baseDn = organizationService.getBaseDn();
        String baseDn = "ou=jans-scim,ou=configuration,o=jans";
        return String.format("ou=configuration,%s", baseDn);
    }
}
