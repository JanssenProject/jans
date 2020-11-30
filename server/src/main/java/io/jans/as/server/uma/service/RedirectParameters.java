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

    private final static Logger LOGGER = LoggerFactory.getLogger(RedirectParameters.class);

    private final Map<String, Set<String>> map = new HashMap<String, Set<String>>();

    public RedirectParameters() {
    }

    public void add(String paramName, String paramValue) {
        Set<String> valueSet = map.get(paramName);
        if (valueSet != null) {
            valueSet.add(paramValue);
        } else {
            Set<String> value = new HashSet<String>();
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
        String queryString = "";
        for (Map.Entry<String, Set<String>> param : map.entrySet()) {
            Set<String> values = param.getValue();
            if (StringUtils.isNotBlank(param.getKey()) && values != null && !values.isEmpty()) {
                for (String value : values) {
                    if (StringUtils.isNotBlank(value)) {
                        try {
                            queryString += param.getKey() + "=" + URLEncoder.encode(value, "UTF-8") + "&";
                        } catch (UnsupportedEncodingException e) {
                            LOGGER.error("Failed to encode value: " + value, e);
                        }
                    }
                }
            }
        }
        queryString = StringUtils.removeEnd(queryString, "&");
        return queryString;
    }

}
