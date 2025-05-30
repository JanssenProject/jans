/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.server.service;

import java.io.Serializable;

import org.slf4j.Logger;

import io.jans.link.model.config.AppConfiguration;
import io.jans.link.model.config.Conf;
import io.jans.link.service.config.ConfigurationFactory;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * GluuConfiguration service
 * 
 * @author Reda Zerrad Date: 08.10.2012
 */
@ApplicationScoped
public class LinkConfigurationService implements Serializable {

    private static final long serialVersionUID = 8842838732456296435L;

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;
    
    @Inject 
    private ConfigurationFactory configurationFactory;

    /**
     * Update configuration entry
     * 
     * @param configuration
     *            GluuConfiguration
     */
    public void updateConfiguration(AppConfiguration configuration) {
        try {
        	Conf conf = getConfiguration(null);
        	conf.setDynamic(configuration);
            persistenceEntryManager.merge(conf);
        } catch (Exception e) {
            log.info("", e);
        }
    }

    /**
     * Get configuration
     * 
     * @return Configuration
     * @throws Exception
     */
    public Conf getConfiguration(String[] returnAttributes) {
    	Conf result = null;
        result = persistenceEntryManager.find(getDnForConfiguration(), Conf.class, returnAttributes);
        
        return result;
    }

    /**
     * Get all configurations
     * 
     * @return List of attributes
     * @throws Exception
     */
	public AppConfiguration getLinkConfiguration() {
		String dn = getDnForConfiguration();
		Conf conf = persistenceEntryManager.find(dn, Conf.class, null);
		return conf.getDynamic();
	}

    /**
     * Build DN string for configuration
     * 
     * @return DN string for specified configuration or DN for configurations branch
     *         if inum is null
     * @throws Exception
     */
    public String getDnForConfiguration() {
    	String dn = configurationFactory.getBaseConfiguration().getString("link_ConfigurationEntryDN");
        return dn;
    }


}
