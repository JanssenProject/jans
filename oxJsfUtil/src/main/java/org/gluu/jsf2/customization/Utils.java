package org.gluu.jsf2.customization;

import org.xdi.util.StringHelper;

import java.io.File;

/**
 * Created by eugeniuparvan on 5/1/17.
 */
public class Utils {
    private static final String CATALINA_BASE_PATH = "catalina.base";
    private static final String CUSTOM_PAGES_PATH = "/custom/pages";

    public static boolean isCustomPagesDirExists() {
        String externalResourceBase = getCustomPagesPath();
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            File folder = new File(externalResourceBase);
            if (folder.exists() && folder.isDirectory())
                return true;
            else
                return false;
        } else {
            return false;
        }
    }

    public static String getCustomPagesPath() {
        String externalResourceBase = System.getProperty(CATALINA_BASE_PATH);
        if (StringHelper.isNotEmpty(externalResourceBase))
            externalResourceBase += CUSTOM_PAGES_PATH;
        return externalResourceBase;
    }
}
