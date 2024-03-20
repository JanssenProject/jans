package io.jans.configapi.plugin.kc.link.model.config;

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
public class KcLinkConfigSource implements ConfigSource {

    private static Logger log = LoggerFactory.getLogger(KcLinkConfigSource.class);
    private static final String FILE_CONFIG = "kc-link.properties";
    private Properties properties = null;
    Map<String, String> propertiesMap = new HashMap<>();

    public KcLinkConfigSource() {
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

    @Override
    public int getOrdinal() {
        return 800;
    }

    @Override
    public String getValue(String name) {
        log.debug("KcLinkConfigSource()::getValue() - name:{}", name);
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
