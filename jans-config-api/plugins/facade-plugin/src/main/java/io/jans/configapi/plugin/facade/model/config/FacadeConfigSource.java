/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.model.config;

import io.jans.exception.ConfigurationException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FacadeConfigSource implements ConfigSource {

    private static Logger log = LoggerFactory.getLogger(FacadeConfigSource.class);
    private static final String FILE_CONFIG = "facade.properties";
    private Properties properties = null;
    Map<String, String> propertiesMap = new HashMap<>();

    public FacadeConfigSource() {
        this.loadProperties();
    }

    @Override
    public Map<String, String> getProperties() {
        log.debug("Getting properties");
        return propertiesMap;
    }

    @Override
    public Set<String> getPropertyNames() {
        log.debug("Getting Property Names");
        try {
            return properties.stringPropertyNames();

        } catch (Exception e) {
            log.error("Unable to read properties from file: " + FILE_CONFIG, e);
        }
        return Collections.emptySet();
    }

    /** Below environment variables (300) and system properties (400), so they override this file. */
    @Override
    public int getOrdinal() {
        return 250;
    }

    @Override
    public String getValue(String name) {
        log.debug("FacadeConfigSource()::getValue() - name:{}", name);
        try {
            return properties.getProperty(name);
        } catch (Exception e) {
            log.error("Unable to read properties from file: " + FILE_CONFIG, e);
        }

        return null;
    }

    @Override
    public String getName() {
        return FILE_CONFIG;
    }

    private Properties loadProperties() {
        // Load the properties file
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try ( InputStream inputStream = loader.getResourceAsStream(FILE_CONFIG)) {
            properties = new Properties();
            properties.load(inputStream);
            properties.stringPropertyNames().stream().forEach(key -> propertiesMap.put(key, properties.getProperty(key)));
            return properties;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load configuration from "+ FILE_CONFIG, e);
        }
    }

}
