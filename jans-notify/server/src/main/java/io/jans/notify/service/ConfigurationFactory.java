/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.util.properties.FileConfiguration;
import io.jans.notify.exception.ConfigurationException;
import io.jans.notify.model.conf.AccessConfiguration;
import io.jans.notify.model.conf.Configuration;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.File;
import java.io.IOException;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@ApplicationScoped
@Named
public class ConfigurationFactory {

	@Inject
	private Logger log;

	@Inject
	private ApplicationService applicationService;

	static {
		if (System.getProperty("jans.base") != null) {
			BASE_DIR = System.getProperty("jans.base");
		} else if ((System.getProperty("catalina.base") != null)
				&& (System.getProperty("catalina.base.ignore") == null)) {
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

	private final String SALT_FILE_NAME = "salt";
	private final String CONFIG_FILE_NAME = "oxnotify-config.json";
	private final String ACCESS_FILE_NAME = "oxnotify-access.json";

	private String confDir;
	private String configFilePath;
	private String accessFilePath;

	private String saltFilePath;
	private String cryptoConfigurationSalt;

	private Configuration configuration;
	private AccessConfiguration accessConfiguration;

	@PostConstruct
	public void init() {
		this.confDir = confDir();
		this.saltFilePath = confDir + SALT_FILE_NAME;
		this.configFilePath = confDir + CONFIG_FILE_NAME;
		this.accessFilePath = confDir + ACCESS_FILE_NAME;
		loadCryptoConfigurationSalt();
	}

	public void create() {
		this.configuration = loadConfiguration();
	}

	private Configuration loadConfiguration() {
		try {
			ObjectMapper mapper = applicationService.createJsonMapper();
			this.configuration = mapper.readValue(new File(this.configFilePath), Configuration.class);
			this.accessConfiguration = mapper.readValue(new File(this.accessFilePath), AccessConfiguration.class);

			return configuration;
		} catch (IOException ex) {
			log.error("Failed to load configuration from. Please fix it!!!.", ex);
			throw new ConfigurationException("Failed to load configuration from LDAP.");
		}
	}

	public void loadCryptoConfigurationSalt() {
		try {
			FileConfiguration cryptoConfiguration = createFileConfiguration(this.saltFilePath, true);

			this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
		} catch (Exception ex) {
			log.error("Failed to load configuration from {}", saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			FileConfiguration fileConfiguration = new FileConfiguration(fileName);

			return fileConfiguration;
		} catch (Exception ex) {
			if (isMandatory) {
				log.error("Failed to load configuration from {}", fileName, ex);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}

	private String confDir() {
		return DIR;
	}

	@Produces
	@ApplicationScoped
	public Configuration createConfiguration() {
		return configuration;
	}

	@Produces
	@ApplicationScoped
	public AccessConfiguration createAccessConfiguration() {
		return accessConfiguration;
	}

}
