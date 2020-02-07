package org.gluu.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.logging.Logger;
import java.util.spi.ResourceBundleControlProvider;

/**
 * Custom resource bundle loader java extension
 *
 * @author Yuriy Movchan
 * @version 02/07/2020
 */
public class ExtendedResourceBundleControlProvider implements ResourceBundleControlProvider {

	private static final Logger LOG = Logger.getLogger(ExtendedResourceBundleControlProvider.class.getName());

	private static Path EXTERNAL_PATH;

	static {
        if (System.getProperty("server.base") != null) {
            Path customPath = Paths.get(System.getProperty("server.base") + "/custom/i18n");
            File file = customPath.toFile();
            if (file.exists() && file.isDirectory()) {
                EXTERNAL_PATH = customPath;
            }
        }
    }

    public ResourceBundle.Control getControl(String baseName) {
        return new CustomControl();
    }

    protected static class CustomControl extends Control {

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            LOG.info("Attempting to laa bundel with baseName: " + baseName);

            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            Properties properties = new Properties();

            try (InputStream input = loader.getResourceAsStream(resourceName)) {
                InputStreamReader inputReader = new InputStreamReader(input, "UTF-8");
                properties.load(inputReader); // Default (internal) bundle.
            }

            Path externalResource = null;
            if (EXTERNAL_PATH != null) {
                externalResource = EXTERNAL_PATH.resolve(resourceName);
            }
            ExtendedResourceBundle.loadPropertiesFromFile(properties, externalResource);

            return new ExtendedResourceBundle(baseName, externalResource, properties);
        }

    }

}