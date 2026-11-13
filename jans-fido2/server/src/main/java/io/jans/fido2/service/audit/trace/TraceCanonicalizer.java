/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON Canonicalization Scheme (RFC 8785) for TRACE records.
 *
 * <p>Member names are sorted by UTF-16 code units, strings are escaped as RFC 8785 requires, and numbers are
 * formatted with the ECMAScript Number-to-String rules over IEEE 754 doubles. Non-finite values are rejected.
 */
public final class TraceCanonicalizer {

    private TraceCanonicalizer() {
    }

    /**
     * @param node JSON value to canonicalize
     * @return the RFC 8785 canonical form as UTF-8 bytes
     * @throws IllegalArgumentException if the value holds a non-finite number or an unsupported node type
     */
    public static byte[] canonicalBytes(JsonNode node) {
        return canonicalString(node).getBytes(StandardCharsets.UTF_8);
    }

    public static String canonicalString(JsonNode node) {
        StringBuilder out = new StringBuilder();
        write(node, out);
        return out.toString();
    }

    /**
     * Formats a double with the ECMAScript Number-to-String rules used by RFC 8785 section 3.2.2.2.
     *
     * @param value finite double
     * @return canonical text, for example "1e+30", "4.5", "0.000001" or "1e-7"
     */
    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Non-finite number cannot be canonicalized");
        }
        if (value == 0.0) {
            return "0";
        }
        if (value < 0) {
            return "-" + formatNumber(-value);
        }
        // Find the shortest decimal that round-trips to the same double. Ties in length are resolved by
        // rounding the exact binary value, which picks the closest candidate.
        BigDecimal exact = new BigDecimal(value);
        BigDecimal shortest = null;
        for (int precision = 1; precision <= 17; precision++) {
            BigDecimal candidate = exact.round(new MathContext(precision, RoundingMode.HALF_EVEN));
            if (candidate.doubleValue() == value) {
                shortest = candidate;
                break;
            }
        }
        if (shortest == null) {
            shortest = exact.round(new MathContext(17, RoundingMode.HALF_EVEN));
        }
        shortest = shortest.stripTrailingZeros();
        String digits = shortest.unscaledValue().abs().toString();
        int k = digits.length();
        // n is the position of the decimal point relative to the first digit (ECMAScript's n)
        int n = k - shortest.scale();

        StringBuilder out = new StringBuilder();
        if (k <= n && n <= 21) {
            out.append(digits);
            appendZeros(out, n - k);
        } else if (0 < n && n <= 21) {
            out.append(digits, 0, n).append('.').append(digits.substring(n));
        } else if (-6 < n && n <= 0) {
            out.append("0.");
            appendZeros(out, -n);
            out.append(digits);
        } else {
            int e = n - 1;
            out.append(digits.charAt(0));
            if (k > 1) {
                out.append('.').append(digits.substring(1));
            }
            out.append('e').append(e < 0 ? '-' : '+').append(Math.abs(e));
        }
        return out.toString();
    }

    private static void appendZeros(StringBuilder out, int count) {
        for (int i = 0; i < count; i++) {
            out.append('0');
        }
    }

    private static void write(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) {
            out.append("null");
        } else if (node.isBoolean()) {
            out.append(node.booleanValue());
        } else if (node.isTextual()) {
            writeString(node.textValue(), out);
        } else if (node.isNumber()) {
            out.append(formatNumber(node.doubleValue()));
        } else if (node.isArray()) {
            out.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                write(node.get(i), out);
            }
            out.append(']');
        } else if (node.isObject()) {
            // RFC 8785 sorts members by UTF-16 code units, which is String.compareTo order
            Map<String, JsonNode> members = new TreeMap<>();
            node.fields().forEachRemaining(entry -> members.put(entry.getKey(), entry.getValue()));
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonNode> member : members.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(member.getKey(), out);
                out.append(':');
                write(member.getValue(), out);
            }
            out.append('}');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value for TRACE canonicalization: " + node.getNodeType());
        }
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                out.append("\\\"");
            } else if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\b') {
                out.append("\\b");
            } else if (c == '\f') {
                out.append("\\f");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }
}
