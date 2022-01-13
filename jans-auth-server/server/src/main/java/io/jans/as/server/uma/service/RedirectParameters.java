/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yuriyz on 06/21/2017.
 */
public class RedirectParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectParameters.class);

    private final Map<String, Set<String>> map = new HashMap<>();

    public void add(String paramName, String paramValue) {
        Set<String> valueSet = map.get(paramName);
        if (valueSet != null) {
            valueSet.add(paramValue);
        } else {
            Set<String> value = new HashSet<>();
            value.add(paramValue);
            map.put(paramName, value);
        }
    }

    public void remove(String paramName) {
        map.remove(paramName);
    }

    public Map<String, Set<String>> map() {
        return map;
    }

    public String buildQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, Set<String>> param : map.entrySet()) {
            Set<String> values = param.getValue();
            if (StringUtils.isNotBlank(param.getKey()) && values != null && !values.isEmpty()) {
                appendValues(queryStringBuilder, param, values);
            }
        }
        String queryString = queryStringBuilder.toString();
        queryString = StringUtils.removeEnd(queryString, "&");
        return queryString;
    }

    private void appendValues(StringBuilder queryStringBuilder, Map.Entry<String, Set<String>> param, Set<String> values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                try {
                    queryStringBuilder.append(param.getKey()).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("Failed to encode value: " + value, e);
                }
            }
        }
    }

}
