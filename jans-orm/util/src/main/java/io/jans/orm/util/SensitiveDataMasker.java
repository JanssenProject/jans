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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    public static final String MASKED = "******";

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i)password|pwd|secret|token");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SensitiveDataMasker() {
    }

    /**
     * Tells whether a key or attribute name denotes a credential: contains "password", "pwd", "secret" or "token"
     * (case-insensitive).
     */
    public static boolean isSensitiveName(String name) {
        return name != null && SENSITIVE_KEY_PATTERN.matcher(name).find();
    }

    /**
     * Returns a printable form of the value with sensitive data masked. Native maps, collections and arrays are
     * inspected structurally (their {@code toString()} output is not JSON); any other value is treated as text.
     * If a container cannot be inspected, the whole value is masked.
     */
    public static String maskValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map || value instanceof Collection
                || (value.getClass().isArray() && !(value instanceof byte[]))) {
            try {
                JsonNode node = MAPPER.valueToTree(value);
                maskNode(node);
                return MAPPER.writeValueAsString(node);
            } catch (IllegalArgumentException | JsonProcessingException e) {
                return MASKED;
            }
        }

        return maskJsonValues(value.toString());
    }

    /**
     * Returns the entries of {@code props} whose keys are in {@code allowedKeys}; everything else is dropped.
     * Use it to log configuration without leaking credentials embedded in non-allowlisted values.
     */
    public static Map<String, String> allowlistProperties(Properties props, String... allowedKeys) {
        Map<String, String> result = new TreeMap<>();
        if (props == null) {
            return result;
        }

        for (String key : allowedKeys) {
            String value = props.getProperty(key);
            if (value != null) {
                result.put(key, isSensitiveName(key) ? MASKED : value);
            }
        }

        return result;
    }

    /**
     * Replaces the value of any JSON key containing "password", "pwd", "secret" or "token" (case-insensitive) with {@link #MASKED}.
     * The value is parsed as JSON and traversed recursively (objects and arrays), so keys are matched against
     * their decoded form (e.g. a key serialized as {@code password} is still recognized as "password"),
     * and non-string scalar values (numbers, booleans) are masked as well. Input that is not a JSON object or
     * array is masked fail-closed: everything after the first sensitive name is replaced (the whole value when
     * the name only appears after decoding JSON escapes), so malformed or truncated documents cannot leak. Used to prevent secrets nested inside serialized JSON configuration
     * (e.g. redis/postgres credentials) from leaking into debug logs.
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

        return maskRawText(value);
    }

    private static String maskRawText(String value) {
        Matcher matcher = SENSITIVE_KEY_PATTERN.matcher(value);
        if (!matcher.find()) {
            return isSensitiveName(unescapeJson(value)) ? MASKED : value;
        }

        // A key that is only sensitive once JSON escapes are decoded may precede the literal match
        if (isSensitiveName(unescapeJson(value.substring(0, matcher.start())))) {
            return MASKED;
        }

        return value.substring(0, matcher.end()) + MASKED;
    }

    private static String unescapeJson(String rawEscapedText) {
        StringBuilder sb = new StringBuilder(rawEscapedText.length());

        for (int i = 0; i < rawEscapedText.length(); i++) {
            char c = rawEscapedText.charAt(i);
            if (c == '\\' && i + 1 < rawEscapedText.length()) {
                char next = rawEscapedText.charAt(i + 1);
                if (next == 'u' && i + 5 < rawEscapedText.length()) {
                    try {
                        sb.append((char) Integer.parseInt(rawEscapedText.substring(i + 2, i + 6), 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException e) {
                        // not a valid unicode escape - keep the backslash as-is
                    }
                } else if ("\"\\/bfnrt".indexOf(next) >= 0) {
                    sb.append(next);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    private static void maskNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) node).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveName(field.getKey())) {
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
