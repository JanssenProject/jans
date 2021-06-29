package io.jans.configapi.configuration;

import io.jans.exception.ConfigurationException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;

public class CustomConfigSource implements ConfigSource {

    @Inject
    private Logger log;

    private final static String FILE_CONFIG = "application.properties";
    private Properties properties = null;

    public CustomConfigSource() {
        this.loadProperties();
    }

    @Override
    public Map<String, String> getProperties() {
        try {

            if (properties == null) {
                this.loadProperties();
            }

            Map<String, String> map = new HashMap<>();
            properties.stringPropertyNames().stream().forEach(key -> map.put(key, properties.getProperty(key)));
            return map;

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to read properties from file: " + FILE_CONFIG, e);
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        try {
            if (properties == null) {
                this.loadProperties();
            }
            return properties.stringPropertyNames();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to read properties from file: " + FILE_CONFIG, e);
        }

        return null;
    }

    @Override
    public int getOrdinal() {
        return 800;
    }

    @Override
    public String getValue(String s) {
        try {
            if (properties == null) {
                this.loadProperties();
            }
            return properties.getProperty(s);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to read properties from file: " + FILE_CONFIG, e);
        }

        return null;
    }

    @Override
    public String getName() {
        return FILE_CONFIG;
    }

    private Properties loadProperties() {
        try {
            // Load the properties file
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = loader.getResourceAsStream(FILE_CONFIG);
            properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            log.error("Failed to load configuration from {}", FILE_CONFIG, e);
            throw new ConfigurationException("Failed to load configuration from " + FILE_CONFIG, e);
        }
    }

}
