/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.service;

import java.io.File;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.eleven.model.Configuration;
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
		if (System.getProperty("jans.base") != null) {
			BASE_DIR = System.getProperty("jans.base");
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

    private final String CONFIG_FILE_NAME = "jans-eleven.json";

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
