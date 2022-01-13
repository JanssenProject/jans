package io.jans.ca.plugin.adminui.utils;

import com.google.common.base.Joiner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

public class CommonUtils {
    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return encode(Joiner.on(" ").join(list));
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

}