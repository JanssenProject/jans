/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.gluu.util.ArrayHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 03.29.2011
 */
public class FileConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FileConfiguration.class);

    private String fileName;
    private boolean isResource;
    private boolean loaded;

    private PropertiesConfiguration propertiesConfiguration;
    private Properties properties;

	private final ReentrantLock reloadLock = new ReentrantLock();
	private boolean isReload = false;

    public FileConfiguration(String fileName) {
        this(fileName, false);
    }

    public FileConfiguration(String fileName, boolean isResource) {
        this.fileName = fileName;
        this.isResource = isResource;
        this.loaded = false;

        if (isResource) {
            loadResourceProperties();
        } else {
            loadProperties();
        }
    }

    public FileConfiguration(String fileName, PropertiesConfiguration propertiesConfiguration) {
        this.fileName = fileName;
        this.isResource = false;
		this.propertiesConfiguration = propertiesConfiguration;

		this.loaded = true;
    }

	protected void loadProperties() {
		try {
			this.propertiesConfiguration = new PropertiesConfiguration(this.fileName);
			this.loaded = true;
		} catch (ConfigurationException ex) {
			log.error(String.format("Failed to load '%s' configuration file from config folder", this.fileName));
		} catch (Exception e) {
			log.error(String.format("Failed to load '%s' configuration file from config folder", this.fileName));
			log.error(e.getMessage(), e);
		}
	}

	protected void loadResourceProperties() {
		log.debug(String.format("Loading '%s' configuration file from resources", this.fileName));
		try {
			this.propertiesConfiguration = new PropertiesConfiguration(this.fileName);
			this.loaded = true;
		} catch (ConfigurationException ex) {
			log.error(String.format("Failed to load '%s' configuration file from resources", this.fileName));
		} catch (Exception e) {
			log.error(String.format("Failed to load '%s' configuration file from config folder", this.fileName));
			log.error(e.getMessage(), e);
		}
	}

    public void reload() {
        this.isReload = true;

        reloadLock.lock(); // block until condition holds
        try {
            if (!this.isReload) {
                return;
            }

            this.properties = null;
            this.loaded = false;

            if (this.isResource) {
                loadResourceProperties();
            } else {
                loadProperties();
            }
        } finally {
            reloadLock.unlock(); // first unlock, for some reason findbug reported this?
            this.isReload = false;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void saveProperties() {
        try {
            this.propertiesConfiguration.save();
        } catch (ConfigurationException ex) {
            log.debug(String.format("Failed to save '%s' configuration file to tomcat config folder", this.fileName));
        }
    }

    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();

            Iterator<?> keys = this.propertiesConfiguration.getKeys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                properties.put(key, getString(key));
            }
        }

        return properties;
    }

    public Properties getPropertiesByPrefix(String propertiesPrefix) {
        Properties properties = new Properties();

        Iterator<?> keys = this.propertiesConfiguration.getKeys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            if (key.startsWith(propertiesPrefix)) {
                properties.put(key.substring(propertiesPrefix.length()), getString(key));
            }
        }

        return properties;
    }

    public String getString(String key) {
        String[] values = this.propertiesConfiguration.getStringArray(key);
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

    public String getString(String key, String defaultValue) {
        try {
            return getString(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public String[] getStringArray(String key) {
        String[] result = this.propertiesConfiguration.getStringArray(key);
        if (ArrayHelper.isNotEmpty(result) && (result.length == 1) && (result[0].trim().length() == 0)) {
            result = new String[0];
        }

        return result;
    }

    public String[] getStringArray(String key, String[] defaultValue) {
        try {
            return getStringArray(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public List<String> getStringList(String key) {
        String[] values = getStringArray(key);
        List<String> result = new ArrayList<String>(values.length);
        for (String value : values) {
            result.add(value);
        }

        return result;
    }

    public int getInt(String key) {
        return this.propertiesConfiguration.getInt(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key) {
        return this.propertiesConfiguration.getBoolean(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException ex) {
            return defaultValue;
        }
    }

    public int getCountItems(String pattern) {
        int i = 1;
        while (this.propertiesConfiguration.containsKey(String.format(pattern, i))) {
            i++;
        }

        return i - 1;
    }

    public void setString(String key, String value) {
        this.propertiesConfiguration.setProperty(key, value);
    }

    public boolean isKeyExist(String key) {
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

    public PropertiesConfiguration getPropertiesConfiguration() {
        return propertiesConfiguration;
    }

}
