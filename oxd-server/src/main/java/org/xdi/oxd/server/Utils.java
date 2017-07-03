/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;

/**
 * Utility class with static methods.
 *
 * @author Yuriy Zabrovarnyy
 */
public class Utils {

    private static final int ONE_HOUR_MILLIS = 60 * 60 * 1000;

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
        return String.format("https://%s/.well-known/uma2-configuration", p_amHost);
    }

    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return URLEncoder.encode(Joiner.on(" ").join(list), "UTF-8");
    }

    public static int hoursDiff(Date earlierDate, Date laterDate) {
        if (earlierDate == null || laterDate == null) {
            return 0;
        }

        int result = (int) ((laterDate.getTime() / ONE_HOUR_MILLIS) - (earlierDate.getTime() / ONE_HOUR_MILLIS));
        return result >= 0 ? result : 0;
    }

    public static long date(Date date) {
        return date != null ? date.getTime() / 1000 : 0;
    }

    public static String encodeCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(Util.getBytes(username + ":" + password));
    }
}
