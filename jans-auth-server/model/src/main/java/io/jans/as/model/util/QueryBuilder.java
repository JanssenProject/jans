/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Yuriy Zabrovarnyy
 */
public class QueryBuilder {

    private static final Logger LOG = Logger.getLogger(QueryBuilder.class);

    private final StringBuilder builder;

    public QueryBuilder() {
        this(new StringBuilder());
    }

    public QueryBuilder(StringBuilder builder) {
        this.builder = builder;
    }

    public static QueryBuilder instance() {
        return new QueryBuilder();
    }

    public String build() {
        return builder.toString();
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    public void appendIfNotNull(String key, Object value) {
        if (value != null) {
            append(key, value.toString());
        }
    }

    public void append(String key, String value) {
        try {
            if (StringUtils.isNotBlank(value)) {
                if (builder.length() > 0) {
                    appendAmpersand();
                }
                builder.append(key).append("=").append(URLEncoder.encode(value, Util.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    public void appendAmpersand() {
        builder.append("&");
    }

    @Override
    public String toString() {
        return build();
    }
}

