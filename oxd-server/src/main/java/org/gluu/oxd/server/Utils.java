/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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

    public static String getUmaDiscoveryUrl(String p_amHost) {
        return String.format("https://%s/.well-known/uma2-configuration", p_amHost);
    }

    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return encode(Joiner.on(" ").join(list));
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
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

    public static boolean isValidUrl(String url) {
        if (StringUtils.isNotBlank(url)) {
            try {
                new URL(url);
                return true;
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        return false;
    }

    public static boolean isTrue(Boolean bool) {
        return bool != null && bool;
    }

    public static List<String> stringToList(String source) {
        return Arrays.asList(source.split("\\s+"));
    }
}
