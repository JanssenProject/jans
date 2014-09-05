/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

/**
 * @author Javier Rojas Blum Date: 08.10.2012
 */
public class StringUtils {

    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";

    public static String nullToEmpty(String str) {
        if (str == null) {
            return EMPTY_STRING;
        } else {
            return str;
        }
    }

    public static boolean equals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        } else if (str1 == null && str2 != null) {//note: str2!=null is always NOT null, see previous 'if' statement*/
            return false;
        } else if (str1 != null && str2 == null) {  //note: str1!=null is ALWAYS true
            return false;
        } else {
            return str1 != null && str1.equals(str2);
        }
    }

    /**
     * Method to join array elements of type string
     *
     * @param inputArray Array which contains strings
     * @param glueString String between each array element
     * @return String containing all array elements separated by glue string.
     */
    public static String implode(String[] inputArray, String glueString) {
        String output = EMPTY_STRING;

        if (inputArray != null && inputArray.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(inputArray[0]);

            for (int i = 1; i < inputArray.length; i++) {
                sb.append(glueString);
                sb.append(inputArray[i]);
            }

            output = sb.toString();
        }

        return output;
    }

    /**
     * Method to join a list of elements of type string
     *
     * @param inputList  List which contains strings
     * @param glueString String between each array element
     * @return String containing all array elements separated by glue string.
     */
    public static String implode(List inputList, String glueString) {
        String output = EMPTY_STRING;

        if (inputList != null && !inputList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(inputList.get(0));

            for (int i = 1; i < inputList.size(); i++) {
                sb.append(glueString);
                sb.append(inputList.get(i));
            }

            output = sb.toString();
        }

        return output;
    }

    public static List<String> spaceSeparatedToList(String spaceSeparatedString) {
        List<String> list = new ArrayList<String>();

        if (isNotBlank(spaceSeparatedString)) {
            list = Arrays.asList(spaceSeparatedString.split(StringUtils.SPACE));
        }

        return list;
    }

    public static JSONArray toJSONArray(List inputList) {
        JSONArray jsonArray = new JSONArray();

        if (inputList != null && !inputList.isEmpty()) {
            jsonArray = new JSONArray(inputList);
        }

        return jsonArray;
    }

    public static List<String> toList(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<String>();

        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        }
        return list;
    }

    public static Date parseSilently(String p_string) {
        try {
            return new Date(Long.parseLong(p_string));
        } catch (Exception e) {
            return null;
        }
    }

    public static void addQueryStringParam(StringBuilder p_queryStringBuilder, String key, Object value) throws UnsupportedEncodingException {
        if (p_queryStringBuilder != null && isNotBlank(key) && value != null) {
            if (p_queryStringBuilder.toString().length() > 0) {
                p_queryStringBuilder.append("&");
            }
            p_queryStringBuilder.append(key).append("=")
                    .append(URLEncoder.encode(value.toString(), Util.UTF8_STRING_ENCODING));
        }
    }

    public static void addQueryStringParam(StringBuilder p_queryStringBuilder, String key, Collection value) throws UnsupportedEncodingException {
        if (p_queryStringBuilder != null && isNotBlank(key) && value != null && !value.isEmpty()) {
            if (p_queryStringBuilder.toString().length() > 0) {
                p_queryStringBuilder.append("&");
            }
            p_queryStringBuilder.append(key).append("=")
                    .append(URLEncoder.encode(value.toString(), Util.UTF8_STRING_ENCODING));
        }
    }
}