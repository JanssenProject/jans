/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service.custom.lib;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.util.StringHelper;
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
                    log.debug("Loaded custom librarty '{}'", jar.toURI().toURL());
                }
            }

            // Restore previous state
            method.setAccessible(false);
        } catch (Exception ex) {
            log.error("Failed to register custom librarties");
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
