/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class StringUtils {

    public static final String UTF8_STRING_ENCODING = "UTF-8";
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";

    public static String getErrorResponse(String error, String errorDescription) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ERROR, error);
            jsonObject.put(ERROR_DESCRIPTION, errorDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    public static List<String> toList(JSONArray jsonArray) {
        List<String> list = new ArrayList<String>();

        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getString(i));
                }
            }
        } catch (Exception ex) {
        }

        return list;
    }
}
