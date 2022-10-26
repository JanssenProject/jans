/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides functionality to parse query strings.
 *
 * @author Javier Rojas Blum
 * @version November 24, 2017
 */
public class QueryStringDecoder {

    private static final Logger log = Logger.getLogger(QueryStringDecoder.class);

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
                    try {
                        map.put(name, URLDecoder.decode(value, Util.UTF8_STRING_ENCODING));
                    } catch (Exception e) {
                        log.error(String.format("Error encoding query param, key: '%s', value: '%s'", name, value), e);
                    }
                }
            }
        }

        return map;
    }
}