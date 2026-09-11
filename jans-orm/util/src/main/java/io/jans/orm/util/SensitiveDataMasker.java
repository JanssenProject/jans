/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    public static final String MASKED = "******";

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i)password|pwd");

    private static final Pattern SENSITIVE_JSON_VALUE_PATTERN = Pattern
            .compile("(?i)(\"[^\"]*(?:password|pwd)[^\"]*\"\\s*:\\s*)"
                    + "(?:\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]\\s]+)");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SensitiveDataMasker() {
    }

    /**
     * Replaces the value of any JSON key containing "password" or "pwd" (case-insensitive) with {@link #MASKED}.
     * The value is parsed as JSON and traversed recursively (objects and arrays), so keys are matched against
     * their decoded form (e.g. a key serialized as {@code password} is still recognized as "password"),
     * and non-string scalar values (numbers, booleans) are masked as well. If the value is not valid JSON,
     * falls back to masking sensitive-looking key/value pairs directly in the raw text. Used to prevent secrets
     * nested inside serialized JSON configuration (e.g. redis/postgres credentials) from leaking into debug logs.
     */
    public static String maskJsonValues(String value) {
        if (value == null) {
            return null;
        }

        try {
            JsonNode root = MAPPER.readTree(value);
            if (root != null && (root.isObject() || root.isArray())) {
                maskNode(root);
                return MAPPER.writeValueAsString(root);
            }
        } catch (JsonProcessingException e) {
            // not a valid JSON document - fall back to raw-text masking below
        }

        return SENSITIVE_JSON_VALUE_PATTERN.matcher(value).replaceAll("$1\"" + MASKED + "\"");
    }

    private static void maskNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) node).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEY_PATTERN.matcher(field.getKey()).find()) {
                    field.setValue(TextNode.valueOf(MASKED));
                } else {
                    maskNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : (ArrayNode) node) {
                maskNode(element);
            }
        }
    }

}
