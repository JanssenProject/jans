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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    public static final String MASKED = "******";

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile("(?i)password|pwd|secret|token|tkn|passphrase");

    /**
     * Matches a raw {@code "key":value} pair. Group 1 is the {@code "key":} prefix (verbatim, escapes untouched),
     * group 2 is the raw (still-escaped) key content used only to test for sensitivity, group 3 is the value token
     * (a quoted string with escapes, or a bare scalar such as a number/boolean/null) to be replaced when sensitive.
     * The quoted-string branch only treats a {@code "} as the closing quote when it is followed by a valid JSON
     * delimiter (comma, {@code }}, {@code ]}, whitespace) or end of input; an unescaped {@code "} that is not
     * followed by such a delimiter is treated as content rather than a terminator, so it does not cause the match
     * (and therefore the masking) to stop partway through the value. Absent any valid terminator, the match runs
     * through the end of input, so a truncated or malformed sensitive value is fully masked instead of partially
     * leaked. All repetitions are possessive so matching is iterative and does not overflow the stack on long input.
     * A bare scalar never contains a quote or an object/array delimiter; an opener matches on its own, so a nonsensitive
     * key's nested container is still scanned member by member, while a sensitive key's container is masked whole.
     * Because a scalar stops at a quote, malformed input such as {@code {"cfg":null{"password":"x"}}} still leaves the
     * nested pair available for masking.
     */
    private static final Pattern RAW_JSON_KEY_VALUE_PATTERN = Pattern.compile(
            "(\"((?:[^\"\\\\]++|\\\\[\\s\\S]?)*+)\"\\s*:\\s*)"
                    + "(\"(?:[^\"\\\\]++|\\\\[\\s\\S]?|\"(?!\\s*(?:[,}\\]]|$)))*+(?:\"(?=\\s*(?:[,}\\]]|$))|$)|[^,{}\\[\\]\\s\"]++|[\\[{])");

    private static final int MAX_EMBEDDED_JSON_DEPTH = 5;

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

        if (value instanceof char[]) {
            return MASKED;
        }

        if (value instanceof Map || value instanceof Collection
                || (value.getClass().isArray() && !(value instanceof byte[]))) {
            try {
                JsonNode node = MAPPER.valueToTree(value);
                maskNode(node, 0);
                return MAPPER.writeValueAsString(node);
            } catch (IllegalArgumentException | JsonProcessingException | StackOverflowError e) {
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
     * and non-string scalar values (numbers, booleans) are masked as well. If the value is not valid JSON,
     * falls back to scanning the raw text for {@code "key":value} pairs, decoding JSON escape sequences in the
     * key (e.g. an escaped {@code w} in "password") before testing for sensitivity, so a malformed/truncated
     * document still has its sensitive values masked. Used to prevent secrets nested inside serialized JSON configuration
     * (e.g. redis/postgres credentials) from leaking into debug logs.
     */
    public static String maskJsonValues(String value) {
        return maskJsonValues(value, 0);
    }

    private static String maskJsonValues(String value, int depth) {
        if (value == null) {
            return null;
        }

        try {
            JsonNode root = MAPPER.readTree(value);
            if (root != null && (root.isObject() || root.isArray())) {
                maskNode(root, depth);
                return MAPPER.writeValueAsString(root);
            }
        } catch (JsonProcessingException e) {
            // not a valid JSON document - fall back to raw-text masking below
        } catch (StackOverflowError e) {
            return MASKED;
        }

        try {
            return maskRawText(value);
        } catch (StackOverflowError e) {
            return MASKED;
        }
    }

    private static String maskRawText(String value) {
        Matcher matcher = RAW_JSON_KEY_VALUE_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        int last = 0;
        int from = 0;

        while (from < value.length() && matcher.find(from)) {
            if (isSensitiveName(unescapeJson(matcher.group(2)))) {
                String token = matcher.group(3);
                boolean opener = "[".equals(token) || "{".equals(token);
                int end = opener ? findContainerEnd(value, matcher.start(3)) : matcher.end();
                result.append(value, last, matcher.start()).append(matcher.group(1))
                        .append('"').append(MASKED).append('"');
                last = end;
                from = end;
            } else {
                from = matcher.end();
            }
        }
        result.append(value, last, value.length());

        return result.toString();
    }

    private static int findContainerEnd(String value, int start) {
        Deque<Character> openers = new ArrayDeque<>();
        boolean inString = false;

        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '[' || c == '{') {
                openers.push(c);
            } else if (c == ']' || c == '}') {
                char expected = (c == ']') ? '[' : '{';
                if (openers.isEmpty() || openers.pop() != expected) {
                    // mismatched closer: structure is untrustworthy, mask through the end of input
                    return value.length();
                }
                if (openers.isEmpty()) {
                    return i + 1;
                }
            }
        }

        return value.length();
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

    private static void maskNode(JsonNode node, int depth) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveName(field.getKey())) {
                    field.setValue(TextNode.valueOf(MASKED));
                } else if (isEmbeddedJson(field.getValue(), depth)) {
                    field.setValue(TextNode.valueOf(maskJsonValues(field.getValue().textValue(), depth + 1)));
                } else {
                    maskNode(field.getValue(), depth);
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                JsonNode element = array.get(i);
                if (isEmbeddedJson(element, depth)) {
                    array.set(i, TextNode.valueOf(maskJsonValues(element.textValue(), depth + 1)));
                } else {
                    maskNode(element, depth);
                }
            }
        }
    }

    /** A string leaf holding a serialized JSON object/array (e.g. a JSON document stored as a string value). */
    private static boolean isEmbeddedJson(JsonNode node, int depth) {
        if (!node.isTextual() || depth >= MAX_EMBEDDED_JSON_DEPTH) {
            return false;
        }

        String text = node.textValue().trim();
        return text.startsWith("{") || text.startsWith("[");
    }

}
