/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides functionality to parse query strings.
 *
 * @author Javier Rojas Blum Date: 09.29.2011
 */
public class QueryStringDecoder {

    /**
     * Avoid instance creation
     */
    private QueryStringDecoder() {
    }

    /**
     * Decodes a query string and returns a map with the parsed query string
     * parameters as keys and its values.
     *
     * @param queryString The query string.
     * @return A map with the parsed query string parameters and its values.
     */
    public static Map<String, String> decode(String queryString) {
        Map<String, String> map = new HashMap<String, String>();

        if (queryString != null) {
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] nameValue = param.split("=");
                String name = nameValue.length > 0 ? nameValue[0] : "";
                String value = nameValue.length > 1 ? nameValue[1] : "";
                if (StringUtils.isNotBlank(name)) {
                    map.put(name, value);
                }
            }
        }

        return map;
    }
}