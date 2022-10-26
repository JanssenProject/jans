/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.i18n;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.faces.context.FacesContext;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom i18n resource loader
 *
 * @author Yuriy Movchan
 * @version 02/23/2018
 */
public class ExtendedResourceBundle extends ResourceBundle {

	private static final Logger LOG = LoggerFactory.getLogger(ExtendedResourceBundle.class);

    private static final String BASE_NAME = "messages";
    private static final Control CONTROL = new CustomControl();

    private static Path EXTERNAL_PATH;
    private static WatchKey WATCHER = null;
    private static Date WATCHER_LAST_UPDATE = new Date();

    static {
        if (System.getProperty("server.base") != null) {
            Path customPath = Paths.get(System.getProperty("server.base") + "/custom/i18n");
            File file = customPath.toFile();
            if (file.exists() && file.isDirectory()) {
                EXTERNAL_PATH = customPath;
            }
        }
        try {
            if (EXTERNAL_PATH != null) {
                WATCHER = EXTERNAL_PATH.register(FileSystems.getDefault().newWatchService(), StandardWatchEventKinds.ENTRY_MODIFY);
            }
        } catch (IOException ex) {
            LOG.error("Failed to initialize custom i18n messages watcher service");
        }
    }

    private final ReentrantLock updateLock = new ReentrantLock();
    private Path externalResource;
    private Properties properties;
    private Date lastUpdate;

    public ExtendedResourceBundle() {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        setParent(ResourceBundle.getBundle(getBaseName(), locale, CONTROL));
    }

    public ExtendedResourceBundle(String baseName, Locale locale) {
        setParent(ResourceBundle.getBundle(baseName, locale, CONTROL));
    }

    private ExtendedResourceBundle(Path externalResource, Properties properties) {
        this.externalResource = externalResource;
        this.properties = properties;

        this.lastUpdate = new Date();
    }

    @Override
    protected Object handleGetObject(String key) {
        if (properties != null) {
            if ((EXTERNAL_PATH != null) && (WATCHER != null)) {
                checkWatcher();

                updateLock.lock();
                try {
                    if (WATCHER_LAST_UPDATE.after(this.lastUpdate)) {
                        loadPropertiesFromFile(properties, externalResource);
                        this.lastUpdate = new Date();
                    }
                } finally {
                    updateLock.unlock();
                }
            }

            return properties.get(key);
        }

        return parent.getObject(key);
    }

    private void checkWatcher() {
        if (!WATCHER.pollEvents().isEmpty()) {
            WATCHER_LAST_UPDATE = new Date();
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration<String> getKeys() {
        if (properties != null) {
            Set keys = properties.keySet();
            return Collections.enumeration(keys);
        }

        return parent.getKeys();
    }

    public String getBaseName() {
        return BASE_NAME;
    }

    protected static void loadPropertiesFromFile(Properties properties, Path externalResource) {
        if (externalResource == null) {
            return;
        }

        InputStreamReader input = null;
        try {
            File file = externalResource.toFile();
            if (file.exists()) {
                input = new FileReader(externalResource.toFile());
                properties.load(input); // External bundle (will overwrite same keys).
            }
        } catch (IOException ex) {
            LOG.error("Failed to read properties file", ex);
        } finally {
            if (input != null) {
                IOUtils.closeQuietly(input);
            }
        }
    }

    protected static class CustomControl extends Control {

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            Properties properties = new Properties();

            InputStream input = null;
            try {
                input = loader.getResourceAsStream(resourceName);
                
                // Fall back to default bundle 
                if (input == null) {
                	String defaultResourceName = toResourceName(toBundleName(baseName, Locale.ROOT), "properties");
                    input = loader.getResourceAsStream(defaultResourceName);
                }
                InputStreamReader inputReader = new InputStreamReader(input, "UTF-8");
                properties.load(inputReader); // Default (internal) bundle.
            } finally {
                if (input != null) {
                    IOUtils.closeQuietly(input);
                }
            }

            Path externalResource = null;
            if (EXTERNAL_PATH != null) {
                externalResource = EXTERNAL_PATH.resolve(resourceName);
            }
            loadPropertiesFromFile(properties, externalResource);

            return new ExtendedResourceBundle(externalResource, properties);
        }

    }

}
