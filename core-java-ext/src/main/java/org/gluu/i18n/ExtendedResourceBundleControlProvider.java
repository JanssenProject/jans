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
import java.util.spi.ResourceBundleControlProvider;

/**
 * Custom resource bundle loader java extension
 *
 * @author Yuriy Movchan
 * @version 02/07/2020
 */
public class ExtendedResourceBundleControlProvider implements ResourceBundleControlProvider {

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
        if (baseName.startsWith("com.sun") || baseName.startsWith("org.apache")) {
//            System.out.println("Using default control to load bundle with baseName: " + baseName);
        	return null;
        }

        System.out.println("Preparing control to load bundle with baseName: " + baseName);

//    	return null;
        return new CustomControl();
    }

    protected static class CustomControl extends Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            Properties properties = new Properties();

            try (InputStream input = loader.getResourceAsStream(resourceName)) {
                InputStreamReader inputReader = new InputStreamReader(input, "UTF-8");
                properties.load(inputReader); // Default (internal) bundle.
            }
            
            if (properties.isEmpty()) {
                System.out.println("Using default control to load bundle with baseName: " + baseName);
            	return null;
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