/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.util;

import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    public static final String MASKED = "******";

    private static final Pattern SENSITIVE_JSON_VALUE_PATTERN = Pattern
            .compile("(?i)(\"[^\"]*(?:password|pwd)[^\"]*\"\\s*:\\s*)"
                    + "(?:\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]\\s]+)");

    private SensitiveDataMasker() {
    }

    /**
     * Replaces the value of any JSON key containing "password" or "pwd" (case-insensitive) with {@link #MASKED}.
     * Handles quoted string values (including escaped characters, e.g. escaped quotes) as well as non-string
     * scalar values (numbers, booleans, null). Used to prevent secrets nested inside serialized JSON
     * configuration (e.g. redis/postgres credentials) from leaking into debug logs.
     */
    public static String maskJsonValues(String value) {
        if (value == null) {
            return null;
        }

        return SENSITIVE_JSON_VALUE_PATTERN.matcher(value).replaceAll("$1\"" + MASKED + "\"");
    }

}
