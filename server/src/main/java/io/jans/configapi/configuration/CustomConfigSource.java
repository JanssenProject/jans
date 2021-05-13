package io.jans.configapi.configuration;

import io.jans.exception.ConfigurationException;
import io.jans.orm.util.properties.FileConfiguration;
import java.io.FileInputStream;
import java.io.IOException;
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

    private final String FILE_CONFIG = "application.properties";
    private FileConfiguration appProperties;

    public CustomConfigSource() {
        System.out.println(" CustomConfigSource::constructor() - Entry \n\n");
    }

    @Override
    public Map<String, String> getProperties() {
        try {
            Properties properties = this.readPropertiesFile(FILE_CONFIG);

            Map<String, String> map = new HashMap<>();
            properties.stringPropertyNames().stream().forEach(key -> map.put(key, properties.getProperty(key)));
            System.out.println("\n\n CustomConfigSource::getProperties() - Exit - map = " + map + "\n\n");
            return map;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        System.out.println("\n\n CustomConfigSource::getPropertyNames() - Entry \n\n");
        try {
            Properties properties = this.readPropertiesFile(FILE_CONFIG);
            return properties.stringPropertyNames();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getOrdinal() {
        return 800;
    }

    @Override
    public String getValue(String s) {

        System.out.println("\n\n CustomConfigSource::getValue() - s = " + s + "\n\n");
        try {

            Properties properties = this.readPropertiesFile(FILE_CONFIG);
            return properties.getProperty(s);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getName() {
        return "FileSystemConfigSource";
    }

    private Properties readPropertiesFile(String fileName) {
        try {
            // Load the properties file
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            System.out.println("\n\n CustomConfigSource::readPropertiesFile() - loader 1 = " + loader + "\n\n");
            InputStream inputStream = loader.getResourceAsStream(fileName);
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Exception ex) {
            log.error("Failed to load configuration from {}", fileName, ex);
            throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
        }
    }

}
