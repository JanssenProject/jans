/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.custom.lib;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.util.StringHelper;
import org.slf4j.Logger;

/**
 * Custom libraries load helper
 *
 * @author Yuriy Movchan Date: 07/10/2017
 */
@ApplicationScoped
public class CustomLibrariesLoader implements Serializable {

    private static final long serialVersionUID = 3918267172467576424L;

    private static final String SERVER_BASE_PATH = "server.base";
    private static final String CUSTOM_LIBS_PATH = "/custom/libs";

    @Inject
    protected Logger log;

    public void init() {
        loadCustomLibraries();
    }

    private void loadCustomLibraries() {
        try {
            String customLibrariesPath = getCustomLibrariesPath();
            if (StringHelper.isEmpty(customLibrariesPath)) {
                return;
            }

            // Get the method URLClassLoader#addURL(URL)
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            // Make it accessible as the method is protected
            method.setAccessible(true);

            ClassLoader webAppClassLoader = Thread.currentThread().getContextClassLoader();
            String[] paths = { customLibrariesPath };
            for (String path : paths) {
                File parent = new File(path);
                File[] jars = parent.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.endsWith(".jar");
                    }
                });

                if (jars == null) {
                    continue;
                }

                for (File jar : jars) {
                    method.invoke(webAppClassLoader, jar.toURI().toURL());
                    log.debug("Loaded custom library '{}'", jar.toURI().toURL());
                }
            }

            // Restore previous state
            method.setAccessible(false);
        } catch (Exception ex) {
            log.error("Failed to register custom libraries");
        }
    }

    public static String getCustomLibrariesPath() {
        String externalResourceBase = System.getProperty(SERVER_BASE_PATH);
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += CUSTOM_LIBS_PATH;

            return externalResourceBase;
        }

        return null;
    }

}
