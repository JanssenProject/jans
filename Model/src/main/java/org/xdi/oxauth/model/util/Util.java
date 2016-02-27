/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.HasParamName;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2012
 */

public class Util {

    public static final String UTF8_STRING_ENCODING = "UTF-8";

    public static byte[] getBytes(String p_str) throws UnsupportedEncodingException {
        return p_str.getBytes(UTF8_STRING_ENCODING);
    }

    public static List<String> asList(JSONArray p_array) throws JSONException {
        final List<String> result = new ArrayList<String>();
        if (p_array != null) {
            final int length = p_array.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    result.add(p_array.getString(i));
                }
            }
        }
        return result;
    }

    public static void addToListIfHas(List<String> p_list, JSONObject jsonObj, String p_key) throws JSONException {
        if (jsonObj != null && org.apache.commons.lang.StringUtils.isNotBlank(p_key) && jsonObj.has(p_key)) {
            JSONArray array = jsonObj.getJSONArray(p_key);
            if (p_list != null && array != null) {
                p_list.addAll(asList(array));
            }
        }
    }

    public static void addToJSONObjectIfNotNull(JSONObject p_jsonObject, String key, Object value) throws JSONException {
        if (p_jsonObject != null && value != null && StringUtils.isNotBlank(key)) {
            p_jsonObject.put(key, value);
        }
    }

    public static void addToJSONObjectIfNotNull(JSONObject p_jsonObject, String key, String[] value) throws JSONException {
        if (p_jsonObject != null && value != null && StringUtils.isNotBlank(key)) {
            p_jsonObject.put(key, new JSONArray(Arrays.asList(value)));
        }
    }

    public static String asString(List<? extends HasParamName> p_list) {
        final StringBuilder sb = new StringBuilder();
        if (p_list != null && !p_list.isEmpty()) {
            for (HasParamName p : p_list) {
                sb.append(" ").append(p.getParamName());
            }
        }
        return sb.toString().trim();
    }

    public static String listAsString(List<String> p_list) {
        StringBuilder param = new StringBuilder();
        if (p_list != null && !p_list.isEmpty()) {
            for (String item : p_list) {
                param.append(" ").append(item);
            }
        }
        return param.toString().trim();
    }

    public static boolean allNotBlank(String... p_strings) {
        if (p_strings != null && p_strings.length > 0) {
            for (String s : p_strings) {
                if (org.apache.commons.lang.StringUtils.isBlank(s)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static List<String> splittedStringAsList(String p_string, String p_delimiter) {
        final List<String> result = new ArrayList<String>();
        if (org.apache.commons.lang.StringUtils.isNotBlank(p_string) && org.apache.commons.lang.StringUtils.isNotEmpty(p_delimiter)) {
            final String[] array = p_string.split(p_delimiter);
            if (array != null && array.length > 0) {
                result.addAll(Arrays.asList(array));
            }
        }
        return result;
    }

    public static List<String> jsonArrayStringAsList(String jsonString) throws JSONException {
        final List<String> result = new ArrayList<String>();
        if (StringUtils.isNotBlank(jsonString)) {
        	JSONArray jsonArray = new JSONArray(jsonString);
        	
        	return asList(jsonArray);
        }

        return result;
    }

    public static <T> T firstItem(List<T> items) {
    	if (items == null) {
    		return null;
    	}

    	Iterator<T> iterator = items.iterator();
    	if (iterator.hasNext()) {
    		return iterator.next();
    	}
        
        return null;
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.length() == 0;
    }

}
