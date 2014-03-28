package org.xdi.oxauth.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * @author Yuriy Movchan
 *         Date: 03.29.2011
 */
public class FileConfiguration {
    private static final Logger log = Logger.getLogger(FileConfiguration.class);

    private String fileName;
    private boolean isResource;

    protected PropertiesConfiguration propertiesConfiguration;

    Properties properties;

    public FileConfiguration(String fileName) {
        this(fileName, false);
    }

    public FileConfiguration(String fileName, boolean isResource) {
        this.fileName = fileName;
        this.isResource = isResource;

        if (isResource) {
            loadResourceProperties();
        } else {
            loadJbossProperties();
        }
    }

    protected void loadJbossProperties() {
//        String tmp = "";
//        for(String k: System.getProperties().stringPropertyNames()){
//            tmp += "\n"+k+" = "+System.getProperty(k);
//        }
        log.debug(String.format("Loading '%s' configuration file from config folder", this.fileName));
        try {
            this.propertiesConfiguration = new PropertiesConfiguration(this.fileName);
        } catch (ConfigurationException ex) {
            log.debug(String.format("Failed to load '%s' configuration file from config folder", this.fileName), ex);
        }
    }

    protected void loadResourceProperties() {
        log.debug(String.format("Loading '%s' configuration file from resources", this.fileName));
        try {
            this.propertiesConfiguration = new PropertiesConfiguration(this.fileName);
        } catch (ConfigurationException ex) {
            log.debug(String.format("Failed to load '%s' configuration file from resources", this.fileName), ex);
        }
    }

    public void reloadProperties() {
        this.properties = null;

        if (this.isResource) {
            loadResourceProperties();
        } else {
            loadJbossProperties();
        }
    }

    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();

            Iterator<?> keys = propertiesConfiguration.getKeys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                properties.put(key, getString(key));
            }
        }

        return properties;
    }

    public String getString(String key) {
        String values[] = propertiesConfiguration.getStringArray(key);
        if (values.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length - 1; i++) {
                sb.append(values[i]).append(',');
            }
            sb.append(values[values.length - 1]);

            return sb.toString();
        }

        return null;
    }

    public String[] getStringArray(String key) {
        String result[] = propertiesConfiguration.getStringArray(key);
        if ((result.length == 1) && (result[0].trim().length() == 0)) {
            result = new String[0];
        }

        return result;
    }

    public int getInt(String key) {
        return propertiesConfiguration.getInt(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        return propertiesConfiguration.getBoolean(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public boolean isKeyExist(String key) {
        @SuppressWarnings("unchecked")
        Iterator<String> keyIterator = propertiesConfiguration.getKeys();
        while (keyIterator.hasNext()) {
            String k = keyIterator.next();
            if (k.equals(key)) {
                return true;
            }
        }

        return false;
    }

    public String getKey(String value) {
        Iterator<String> keyIterator = propertiesConfiguration.getKeys();
        while (keyIterator.hasNext()) {
            String k = keyIterator.next();
            String v = propertiesConfiguration.getString(k);
            if (v.equals(value)) {
                return k;
            }
        }

        return null;
    }
}