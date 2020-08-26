/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.ErrorResponseCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

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
                if (url.contains("#")) {
                    throw new HttpException(ErrorResponseCode.URI_HAS_FRAGMENT_COMPONENT);
                }
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

    public static String mapAsStringWithEncodedValues(Map<String, String> p_map) {
        if (p_map != null && p_map.size() != 0) {
            return p_map.entrySet().stream().map(e -> {
                try {
                    return e.getKey() + "=" + encode(e.getValue());
                } catch (UnsupportedEncodingException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.joining("&"));
        } else {
            return "";
        }
    }

    public static Date addTimeToDate(Date date, int timeValue, int timeFormat) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(timeFormat, timeValue);
        return cal.getTime();
    }

    public static Properties loadPropertiesFromFile(String filename, Properties props) {

        try (FileInputStream fileistream = new FileInputStream(filename)) {
            if (props == null) {
                props = new Properties();
            }
            props.load(fileistream);
            return props;
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Could not load properties from file " + filename, e);
        }
    }
}
