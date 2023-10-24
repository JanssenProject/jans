package io.jans.idp.keycloak.config;

import io.jans.idp.keycloak.util.Constants;

import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.component.ComponentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JansConfigSource implements ConfigSource {

    private static Logger logger = LoggerFactory.getLogger(JansConfigSource.class);
    private static final String CONFIG_FILE_NAME = "jans-keycloak-storage-api.properties";
    
    private String configFilePath = null;
    private Properties properties = null;
    private Map<String, String> propertiesMap = new HashMap<>();

    public JansConfigSource() {
        this.configFilePath = System.getProperty(Constants.JANS_CONFIG_PROP_PATH);
        logger.info("this.configFilePath:{}", configFilePath);
       
        if (StringUtils.isBlank(configFilePath)) {
            throw new ComponentValidationException(
                    "Configuration property file path `System property` not set, please verify.");
        }
        
        this.loadProperties();
    }

    @Override
    public Map<String, String> getProperties() {
        logger.info("\n\n Getting properties \n\n");
        return propertiesMap;
    }

    @Override
    public Set<String> getPropertyNames() {
        logger.debug("\n\n Getting Property Names \n\n");
        try {
            return properties.stringPropertyNames();

        } catch (Exception e) {
            logger.error("Unable to read properties from CONFIG_FILE_NAME:{} - error is :{}", CONFIG_FILE_NAME, e);
        }
        return Collections.emptySet();
    }

    @Override
    public int getOrdinal() {
        return 800;
    }

    @Override
    public String getValue(String name) {
        try {
            logger.trace("JansConfigSource()::getValue() - name:{} - :{}", name, properties.getProperty(name));
            return properties.getProperty(name);
        } catch (Exception e) {
            logger.error("Unable to read properties from file:{} - error is :{} ", CONFIG_FILE_NAME, e);
        }

        return null;
    }

    @Override
    public String getName() {
        return CONFIG_FILE_NAME;
    }

    public String getQualifiedFileName() {
        String fileSeparator = FileSystems.getDefault().getSeparator();
        logger.info("\n\n JansConfigSource()::getQualifiedFileName() - fileSeparator:{}", fileSeparator);
        return this.configFilePath + fileSeparator + CONFIG_FILE_NAME;
    }

    private Properties loadProperties() {
        logger.info("\n\n\n ***** JansConfigSource::loadProperties() - Properties form Config.Scope ");

        // Get file path
        String filePath = getQualifiedFileName();
        logger.info("\n\n\n ***** JansConfigSource::loadProperties() - properties:{}, filePath:{}", properties, filePath);

        if (StringUtils.isBlank(filePath)) {
            logger.error("Property filePath is null!");
            throw new ComponentValidationException("Config property filePath is null!!!");
        }

        // load the file handle for main.properties
        try (FileInputStream file = new FileInputStream(filePath)) {
            logger.info(" JansConfigSource::loadProperties() - file:{}", file);

            // load all the properties from this file
            properties = new Properties();
            properties.load(file);
            properties.stringPropertyNames().stream()
                    .forEach(key -> propertiesMap.put(key, properties.getProperty(key)));

            logger.debug("JansConfigSource()::loadProperties() - properties :{}", properties);

            if (properties.isEmpty()) {
                logger.error("Could not load config properties!");
                throw new ComponentValidationException("Could not load config properties!!!");
            }

            printProperties(properties);

        } catch (Exception ex) {
            logger.error("Failed to load property file", ex);
            throw new ComponentValidationException("Failed to load property file!!!");
        }

        return properties;
    }

    private static void printProperties(Properties prop) {
        if (prop == null || prop.isEmpty()) {
            return;
        }
        prop.keySet().stream().map(key -> key + ": " + prop.getProperty(key.toString())).forEach(logger::debug);
    }

}
