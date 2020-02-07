package org.gluu.i18n;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom i18n resource bundle with realod support
 *
 * @author Yuriy Movchan
 * @version 02/07/2020
 */
public class ExtendedResourceBundle extends ResourceBundle {

    private WatchKey watcher = null;
    private Date watcherLastUpdate = new Date();

    private String baseName;
    private Path externalResource;
    private Properties properties;
    private Date lastUpdate;

    private final ReentrantLock updateLock = new ReentrantLock();

    public ExtendedResourceBundle(String baseName, Path externalResource, Properties properties) throws IOException {
        this.baseName = baseName;
        this.externalResource = externalResource;
        this.properties = properties;

        this.lastUpdate = new Date();
        this.watcher = externalResource.register(FileSystems.getDefault().newWatchService(), StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    protected Object handleGetObject(String key) {
        if (properties != null) {
            if ((externalResource != null) && (watcher != null)) {
                checkWatcher();

                updateLock.lock();
                try {
                    if (watcherLastUpdate.after(this.lastUpdate)) {
                        loadPropertiesFromFile(properties, externalResource);
                        this.lastUpdate = new Date();
                    }
                } catch (IOException ex) {
                	System.err.println("Failed to reload message bundle:" + externalResource);
				} finally {
                    updateLock.unlock();
                }
            }

            return properties.get(key);
        }

        return parent.getObject(key);
    }

    private void checkWatcher() {
        if (!watcher.pollEvents().isEmpty()) {
            watcherLastUpdate = new Date();
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
        return baseName;
    }

    protected static void loadPropertiesFromFile(Properties properties, Path externalResource) throws IOException {
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
        	System.err.println("Failed to load message bundle:" + externalResource);
            throw ex;
        } finally {
        	try {
				input.close();
			} catch (Exception ex) {}
        }
    }

}
