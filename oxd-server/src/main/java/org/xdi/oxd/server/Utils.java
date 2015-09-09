/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

/**
 * Utility class with static methods.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class Utils {

    /**
     * Application mode string
     */
    private static final String APP_MODE = System.getProperty("app.mode");

    /**
     * Avoid instance creation.
     */
    private Utils() {
    }

    /**
     * Returns whether app is in test mode.
     *
     * @return whether app is in test mode
     */
    public static boolean isTestMode() {
        return "test".equals(APP_MODE);
    }

    public static String getDiscoveryUrl(String p_amHost) {
        return String.format("https://%s/.well-known/openid-configuration", p_amHost);
    }

    public static String getUmaDiscoveryUrl(String p_amHost) {
        return String.format("https://%s/.well-known/uma-configuration", p_amHost);
    }
}
