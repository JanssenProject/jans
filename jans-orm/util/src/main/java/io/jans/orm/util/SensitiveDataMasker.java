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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    public static final String MASKED = "******";

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i)password|pwd");

    /**
     * Matches a raw {@code "key":value} pair. Group 1 is the {@code "key":} prefix (verbatim, escapes untouched),
     * group 2 is the raw (still-escaped) key content used only to test for sensitivity, group 3 is the value token
     * (a quoted string with escapes, or a bare scalar such as a number/boolean/null) to be replaced when sensitive.
     * The quoted-string branch also accepts an unterminated string (no closing quote before the end of input),
     * consuming through the end of input rather than stopping at an internal delimiter such as a comma, so a
     * truncated sensitive value is fully masked instead of partially leaked.
     */
    private static final Pattern RAW_JSON_KEY_VALUE_PATTERN = Pattern.compile(
            "(\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*)(\"(?:\\\\.|[^\"\\\\])*(?:\"|$)|[^,}\\]\\s]+)");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SensitiveDataMasker() {
    }

    /**
     * Replaces the value of any JSON key containing "password" or "pwd" (case-insensitive) with {@link #MASKED}.
     * The value is parsed as JSON and traversed recursively (objects and arrays), so keys are matched against
     * their decoded form (e.g. a key serialized as {@code password} is still recognized as "password"),
     * and non-string scalar values (numbers, booleans) are masked as well. If the value is not valid JSON,
     * falls back to scanning the raw text for {@code "key":value} pairs, decoding JSON escape sequences in the
     * key (e.g. an escaped {@code w} in "password") before testing for sensitivity, so a malformed/truncated
     * document still has its sensitive values masked. Used to prevent secrets nested inside serialized JSON configuration
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
        Matcher matcher = RAW_JSON_KEY_VALUE_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            if (SENSITIVE_KEY_PATTERN.matcher(unescapeJson(matcher.group(2))).find()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + "\"" + MASKED + "\""));
            }
        }
        matcher.appendTail(result);

        return result.toString();
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
