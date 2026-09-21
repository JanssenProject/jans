/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * In-house, dependency-free implementation of RFC 8785 (JSON Canonicalization Scheme, JCS) over
 * Jackson {@link JsonNode} trees. Used for TRACE signature input, content digests, receipt hashes
 * and composite keys.
 *
 * <p>Rules implemented (RFC 8785 section 3.2):
 * <ul>
 * <li>No whitespace between tokens.</li>
 * <li>Object properties are sorted by the UTF-16 code-unit sequence of their raw names
 * (equivalent to {@link String#compareTo(String)}); arrays keep their order.</li>
 * <li>Strings escape {@code "} and {@code \} and the control characters U+0000-U+001F, using the
 * short forms {@code \b \t \n \f \r} where defined and lowercase {@code \\uXXXX} otherwise.
 * Everything else, including U+007F, U+2028/U+2029 and the slash, is emitted literally.</li>
 * <li>Numbers follow the ECMAScript {@code Number::toString} algorithm: the shortest decimal
 * that round-trips to the same IEEE-754 double, formatted with ES6 exponent rules.</li>
 * <li>Literals are {@code true}, {@code false}, {@code null}.</li>
 * </ul>
 *
 * <p>Documented implementation choices (RFC 8785 section 3.2.2.3 lets implementations restrict
 * number handling):
 * <ul>
 * <li>Integral nodes ({@code IntNode}, {@code ShortNode}, {@code LongNode}, {@code BigIntegerNode})
 * are serialized as their exact decimal digits, also outside the IEEE-754 safe range
 * (|v| &gt; 2^53-1) where ECMAScript would round to the nearest double. {@code long} values never
 * reach 1e21, so exponent form never applies to them; a {@code BigIntegerNode} at or above 1e21 is
 * still emitted as plain digits.</li>
 * <li>{@code DoubleNode} and {@code FloatNode} use the ES6 algorithm on their double value.</li>
 * <li>{@code DecimalNode} (only produced when {@code USE_BIG_DECIMAL_FOR_FLOATS} is enabled) is
 * converted with {@link BigDecimal#doubleValue()} first. This is lossy for values with more than
 * 17 significant digits and matches what a double-based parser would have produced.</li>
 * <li>{@code NaN} and infinities, lone UTF-16 surrogates and non-JSON node types (POJO, binary,
 * missing) raise {@link JcsException}.</li>
 * </ul>
 *
 * The output of {@link #canonicalizeToUtf8(JsonNode)} is UTF-8 without a byte-order mark.
 * 
 * @author Yuriy Movchan
 */
public final class JcsCanonicalizer {

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	/** Largest number of significant decimal digits ever needed to round-trip a double. */
	private static final int MAX_DOUBLE_PRECISION = 17;

	private JcsCanonicalizer() {
	}

	/**
	 * Canonicalizes the JSON value as a Java string (UTF-16). Use
	 * {@link #canonicalizeToUtf8(JsonNode)} for the byte form required by cryptographic
	 * operations.
	 *
	 * @throws JcsException if the node contains values that JCS cannot represent
	 */
	public static String canonicalize(JsonNode node) {
		if (node == null) {
			throw new JcsException("Cannot canonicalize a null node");
		}
		StringBuilder out = new StringBuilder(128);
		serialize(node, out);
		return out.toString();
	}

	/**
	 * Canonicalizes the JSON value and encodes the result as UTF-8 (no BOM).
	 *
	 * @throws JcsException if the node contains values that JCS cannot represent
	 */
	public static byte[] canonicalizeToUtf8(JsonNode node) {
		return canonicalize(node).getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Returns a deep copy of {@code node} without the given top-level property. Nested properties
	 * with the same name are kept. The input node is not modified. Used to strip the
	 * {@code signature} property before computing the signature input.
	 */
	public static JsonNode withoutField(ObjectNode node, String fieldName) {
		if (node == null) {
			throw new JcsException("Cannot remove a field from a null object node");
		}
		Objects.requireNonNull(fieldName, "fieldName");
		ObjectNode copy = node.deepCopy();
		copy.remove(fieldName);
		return copy;
	}

	private static void serialize(JsonNode node, StringBuilder out) {
		switch (node.getNodeType()) {
		case OBJECT:
			serializeObject(node, out);
			break;
		case ARRAY:
			serializeArray(node, out);
			break;
		case STRING:
			serializeString(node.textValue(), out);
			break;
		case NUMBER:
			serializeNumber(node, out);
			break;
		case BOOLEAN:
			out.append(node.booleanValue() ? "true" : "false");
			break;
		case NULL:
			out.append("null");
			break;
		default:
			throw new JcsException("Unsupported JSON node type for canonicalization: " + node.getNodeType());
		}
	}

	private static void serializeObject(JsonNode node, StringBuilder out) {
		List<Map.Entry<String, JsonNode>> members = new ArrayList<>(node.size());
		for (Map.Entry<String, JsonNode> member : node.properties()) {
			members.add(member);
		}
		// String.compareTo compares UTF-16 code units in order, then by length: exactly the
		// RFC 8785 section 3.2.3 ordering (not code points, not locale-aware).
		members.sort((a, b) -> a.getKey().compareTo(b.getKey()));

		out.append('{');
		boolean first = true;
		for (Map.Entry<String, JsonNode> member : members) {
			if (!first) {
				out.append(',');
			}
			first = false;
			serializeString(member.getKey(), out);
			out.append(':');
			serialize(member.getValue(), out);
		}
		out.append('}');
	}

	private static void serializeArray(JsonNode node, StringBuilder out) {
		out.append('[');
		boolean first = true;
		for (JsonNode element : node) {
			if (!first) {
				out.append(',');
			}
			first = false;
			serialize(element, out);
		}
		out.append(']');
	}

	private static void serializeString(String value, StringBuilder out) {
		out.append('"');
		int length = value.length();
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			switch (c) {
			case '"':
				out.append("\\\"");
				break;
			case '\\':
				out.append("\\\\");
				break;
			case '\b':
				out.append("\\b");
				break;
			case '\t':
				out.append("\\t");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\f':
				out.append("\\f");
				break;
			case '\r':
				out.append("\\r");
				break;
			default:
				if (c < 0x20) {
					out.append("\\u00").append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
				} else if (Character.isHighSurrogate(c)) {
					if (i + 1 >= length || !Character.isLowSurrogate(value.charAt(i + 1))) {
						throw new JcsException("Lone high surrogate U+" + hex4(c) + " at index " + i);
					}
					out.append(c).append(value.charAt(++i));
				} else if (Character.isLowSurrogate(c)) {
					throw new JcsException("Lone low surrogate U+" + hex4(c) + " at index " + i);
				} else {
					out.append(c);
				}
			}
		}
		out.append('"');
	}

	private static String hex4(char c) {
		return String.format("%04X", (int) c);
	}

	private static void serializeNumber(JsonNode node, StringBuilder out) {
		switch (node.numberType()) {
		case INT:
		case LONG:
			// Exact digits; a long can never reach 1e21, so ES6 would print the same digits
			// for |v| <= 2^53-1 and we deliberately keep exact digits above it.
			out.append(node.longValue());
			break;
		case BIG_INTEGER:
			out.append(node.bigIntegerValue().toString());
			break;
		case FLOAT:
		case DOUBLE:
			out.append(formatDouble(node.doubleValue()));
			break;
		case BIG_DECIMAL:
			out.append(formatDouble(node.decimalValue().doubleValue()));
			break;
		default:
			throw new JcsException("Unsupported number type for canonicalization: " + node.numberType());
		}
	}

	/**
	 * ECMAScript {@code Number::toString} (ECMA-262 section 7.1.12.1 in ES6 numbering) for a
	 * double, as required by RFC 8785 section 3.2.2.3.
	 */
	static String formatDouble(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			throw new JcsException("Non-finite number cannot be canonicalized: " + value);
		}
		if (value == 0.0d) {
			return "0"; // covers -0.0 as well
		}
		boolean negative = value < 0;
		double magnitude = Math.abs(value);

		BigDecimal shortest = shortestRoundTrip(magnitude).stripTrailingZeros();
		// value = s * 10^(n-k), where s has k digits and n positions the decimal point
		String digits = shortest.unscaledValue().toString();
		int k = digits.length();
		int n = k - shortest.scale();

		StringBuilder out = new StringBuilder(32);
		if (negative) {
			out.append('-');
		}
		if (k <= n && n <= 21) {
			out.append(digits);
			appendZeros(out, n - k);
		} else if (0 < n && n <= 21) {
			out.append(digits, 0, n).append('.').append(digits, n, k);
		} else if (-6 < n && n <= 0) {
			out.append("0.");
			appendZeros(out, -n);
			out.append(digits);
		} else {
			int exponent = n - 1;
			out.append(digits.charAt(0));
			if (k > 1) {
				out.append('.').append(digits, 1, k);
			}
			out.append('e').append(exponent < 0 ? '-' : '+').append(Math.abs(exponent));
		}
		return out.toString();
	}

	/**
	 * Finds the decimal with the fewest significant digits that parses back to exactly
	 * {@code magnitude}. When two candidates with that digit count both round-trip, the one
	 * closer to the exact binary value wins; on a tie the candidate with an even significand
	 * wins (ECMA-262 "Note 2").
	 */
	private static BigDecimal shortestRoundTrip(double magnitude) {
		BigDecimal exact = new BigDecimal(magnitude);
		for (int precision = 1; precision <= MAX_DOUBLE_PRECISION; precision++) {
			BigDecimal lower = exact.round(new MathContext(precision, RoundingMode.FLOOR));
			BigDecimal upper = exact.round(new MathContext(precision, RoundingMode.CEILING));
			boolean lowerOk = roundTrips(lower, magnitude);
			boolean upperOk = roundTrips(upper, magnitude);
			if (lowerOk && upperOk) {
				if (lower.compareTo(upper) == 0) {
					return lower;
				}
				int closer = exact.subtract(lower).compareTo(upper.subtract(exact));
				if (closer < 0) {
					return lower;
				}
				if (closer > 0) {
					return upper;
				}
				return lower.unscaledValue().testBit(0) ? upper : lower;
			}
			if (lowerOk) {
				return lower;
			}
			if (upperOk) {
				return upper;
			}
		}
		// 17 significant digits always round-trip for IEEE-754 binary64; unreachable.
		throw new JcsException("No round-trip decimal representation found for " + magnitude);
	}

	private static boolean roundTrips(BigDecimal candidate, double magnitude) {
		// Double.parseDouble is correctly rounded, so equality is the exact round-trip criterion.
		return Double.parseDouble(candidate.toString()) == magnitude;
	}

	private static void appendZeros(StringBuilder out, int count) {
		for (int i = 0; i < count; i++) {
			out.append('0');
		}
	}

}
