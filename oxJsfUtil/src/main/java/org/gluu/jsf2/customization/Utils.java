package org.gluu.jsf2.customization;

import java.io.File;

import org.gluu.util.StringHelper;

/**
 * Created by eugeniuparvan on 5/1/17.
 */
public final class Utils {
    private static final String SERVER_BASE_PATH = "server.base";
    private static final String CUSTOM_PAGES_PATH = "/custom/pages";

    private Utils() { }

    public static boolean isCustomPagesDirExists() {
        String externalResourceBase = getCustomPagesPath();
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            File folder = new File(externalResourceBase);
            boolean result = folder.exists() && folder.isDirectory();

            return result;
        } else {
            return false;
        }
    }

    public static String getCustomPagesPath() {
        String externalResourceBase = System.getProperty(SERVER_BASE_PATH);
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += CUSTOM_PAGES_PATH;
        }

        return externalResourceBase;
    }

}
