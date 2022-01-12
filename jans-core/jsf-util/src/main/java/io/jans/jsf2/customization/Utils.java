/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.customization;

import java.io.File;

import io.jans.util.StringHelper;

/**
 * Created by eugeniuparvan on 5/1/17.
 */
public final class Utils {
    private static final String SERVER_BASE_PATH = "server.base";
    private static final String CUSTOM_PAGES_PATH = "/custom/pages";
    private static final String CUSTOM_LOCALIZATION_PATH = "/custom/i18n";

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

    public static boolean isCustomLocalizationDirExists() {
        String externalResourceBase = getCustomLocalizationPath();
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

    public static String getCustomLocalizationPath() {
        String externalResourceBase = System.getProperty(SERVER_BASE_PATH);
        if (StringHelper.isNotEmpty(externalResourceBase)) {
            externalResourceBase += CUSTOM_LOCALIZATION_PATH;
        }

        return externalResourceBase;
    }

}
