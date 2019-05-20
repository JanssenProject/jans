/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.service;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxeleven.model.Configuration;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version May 20, 2016
 */
@ApplicationScoped
@Named
public class ConfigurationFactory {

	@Inject
	private Logger log;

	static {
		if (System.getProperty("gluu.base") != null) {
			BASE_DIR = System.getProperty("gluu.base");
		} else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	private static final String BASE_DIR;
	private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

    private final String CONFIG_FILE_NAME = "oxeleven-config.json";

	private String confDir;
    private String configFilePath;

    private Configuration configuration;
	
    @PostConstruct
    public void init() {
		this.confDir = confDir();
		this.configFilePath = this.confDir + CONFIG_FILE_NAME;
        this.configuration = loadConfiguration();
    }

	private Configuration loadConfiguration() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(this.configFilePath), Configuration.class);
        } catch (Exception ex) {
            log.error("Failed to load configuration from LDAP. Please fix it!!!.", ex);
        }

		return null;
    }

    private String confDir() {
        return DIR;
    }

	@Produces
	@ApplicationScoped
    public Configuration getConfiguration() {
        return configuration;
    }

}
