package org.gluu.oxtrust.service.radius;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.service.config.ConfigurationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.MappingException;
import org.gluu.radius.model.ServerConfiguration;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;

@ApplicationScoped
public class GluuRadiusConfigService implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 3720989420960664644L;

	private static final String configurationEntryDN = "oxradius_ConfigurationEntryDN";

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private ConfigurationFactory<?> configurationFactory;

    @Inject
    private Logger log;

    public ServerConfiguration getServerConfiguration() {

        if(containsRadiusServerConfiguration() == false)
            return null;
        
        try {
            return persistenceEntryManager.find(ServerConfiguration.class,getServerConfigurationDn());
        }catch(MappingException e) {
            log.error("Failed to load radius server configuration",e);
        }
        return null;
    }

    public void updateServerConfiguration(ServerConfiguration serverConfiguration) {

        if(serverConfiguration == null || containsRadiusServerConfiguration() == false)
            return;
        
        serverConfiguration.setDn(getServerConfigurationDn());
        persistenceEntryManager.merge(serverConfiguration);
    }

    private boolean containsRadiusServerConfiguration() {
        
        String configurationDn = getServerConfigurationDn();
        if(StringHelper.isEmpty(configurationDn))
            return false;
        return persistenceEntryManager.contains(configurationDn,ServerConfiguration.class);
    }

    private String getServerConfigurationDn() {
        FileConfiguration fileConfiguration = configurationFactory.getBaseConfiguration();
        return fileConfiguration.getString(configurationEntryDN);
    }
}