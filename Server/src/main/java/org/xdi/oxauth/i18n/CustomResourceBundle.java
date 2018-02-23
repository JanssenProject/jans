package org.xdi.oxauth.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.faces.context.FacesContext;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Custom i18n resource loader
 *
 * @author Yuriy Movchan
 * @version 02/23/2018
 */
public class CustomResourceBundle extends ResourceBundle {

    private static final Logger LOG = Logger.getLogger(CustomResourceBundle.class);

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

    public CustomResourceBundle() {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        if ((EXTERNAL_PATH != null) && (WATCHER != null)) {
            setParent(ResourceBundle.getBundle(BASE_NAME, locale, CONTROL));
        } else {
            setParent(ResourceBundle.getBundle(BASE_NAME, locale));
        }
        
        this.lastUpdate = new Date();
    }

    private CustomResourceBundle(Path externalResource, Properties properties) {
        this.externalResource = externalResource;
        this.properties = properties;
        
        this.lastUpdate = new Date();
    }

    @Override
    protected Object handleGetObject(String key) {
        if (properties != null) {
            checkWatcher();
            
            updateLock.lock();
            try {
                if (WATCHER_LAST_UPDATE.after(this.lastUpdate)) {
                    InputStream input = null;
                    try {
                        input = new FileInputStream(externalResource.toFile());
                        properties.load(input);
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    } finally {
                        if (input != null) {
                            IOUtils.closeQuietly(input);
                        }
                    }
                    this.lastUpdate = new Date();
                }
            } finally {
                updateLock.unlock();
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

    protected static class CustomControl extends Control {

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            Path externalResource = EXTERNAL_PATH.resolve(resourceName);
            Properties properties = new Properties();

            InputStream input = null;
            try {
                input = loader.getResourceAsStream(resourceName);
                properties.load(input); // Default (internal) bundle.
            } finally {
                if (input != null) {
                    IOUtils.closeQuietly(input);
                }
            }

            input = null;
            try {
                File file = externalResource.toFile();
                if (file.exists()) {
                    input = new FileInputStream(externalResource.toFile());
                    properties.load(input); // External bundle (will overwrite same keys).
                }
            } finally {
                if (input != null) {
                    IOUtils.closeQuietly(input);
                }
            }

            return new CustomResourceBundle(externalResource, properties);
        }

    }

}
