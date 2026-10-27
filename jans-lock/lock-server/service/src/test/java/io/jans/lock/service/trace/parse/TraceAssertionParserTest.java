/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;

/**
 * Tests for {@link TraceAssertionParser}: bounded reading, strict UTF-8 decoding, and the JSON
 * limits from design §14.
 */
class TraceAssertionParserTest {

	@Mock
	private AppConfiguration appConfiguration;

	@InjectMocks
	private TraceAssertionParser parser;

	private TraceConfiguration traceConfiguration;

	@BeforeEach
	void setUp() throws ReflectiveOperationException {
		MockitoAnnotations.openMocks(this);
		java.lang.reflect.Field cryptoField = TraceAssertionParser.class.getDeclaredField("cryptoProvider");
		cryptoField.setAccessible(true);
		cryptoField.set(parser, io.jans.lock.service.trace.testkit.TraceTestCrypto.javaProvider());
		traceConfiguration = new TraceConfiguration();
		org.mockito.Mockito.when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
	}

	private InputStream utf8(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}

	private String buildNested(int depth) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("{\"a\":");
		}
		sb.append("1");
		for (int i = 0; i < depth; i++) {
			sb.append("}");
		}
		return sb.toString();
	}

	private String buildArrayOf(int count) {
		StringBuilder sb = new StringBuilder("{\"arr\":[");
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(i);
		}
		sb.append("]}");
		return sb.toString();
	}

	private String buildObjectWithMembers(int count) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append("\"k").append(i).append("\":").append(i);
		}
		sb.append("}");
		return sb.toString();
	}

	@Test
	void testParse_designExample_parsesSuccessfully() throws IOException {
		String fixture;
		try (InputStream is = getClass().getResourceAsStream("/trace/assertions/valid-authorization-decision.json")) {
			fixture = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		ParsedAssertion result = parser.parse(utf8(fixture));

		assertEquals(fixture, result.getRawText());
		ObjectNode root = result.getRoot();
		assertEquals("cedarling-fleet-1", root.get("producer").textValue());
		assertTrue(root.get("trace").isObject());
		assertTrue(root.get("parent_record_ids").isArray());
	}

	@Test
	void testParse_bodyExactlyMaxRequestBytes_accepted() {
		traceConfiguration.setMaxRequestBytes(10);
		String body = "{\"a\":1234}"; // exactly 10 bytes
		assertEquals(10, body.getBytes(StandardCharsets.UTF_8).length);

		ParsedAssertion result = parser.parse(utf8(body));

		assertEquals(1234, result.getRoot().get("a").intValue());
	}

	@Test
	void testParse_bodyOneByteOverMaxRequestBytes_throwsBodyTooLarge() {
		traceConfiguration.setMaxRequestBytes(10);
		String body = "{\"a\":12345}"; // exactly 11 bytes: maxRequestBytes + 1
		assertEquals(11, body.getBytes(StandardCharsets.UTF_8).length);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("body_too_large", ex.getReason());
	}

	@Test
	void testParse_nestingDepthAtLimit_accepted() {
		String body = buildNested(traceConfiguration.getMaxJsonDepth());

		ParsedAssertion result = parser.parse(utf8(body));

		assertTrue(result.getRoot().isObject());
	}

	@Test
	void testParse_nestingDepthOverLimit_rejected() {
		String body = buildNested(traceConfiguration.getMaxJsonDepth() + 1);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("json_limit_exceeded", ex.getReason());
	}

	@Test
	void testParse_arrayLengthAtLimit_accepted() {
		String body = buildArrayOf(traceConfiguration.getMaxArrayLength());

		ParsedAssertion result = parser.parse(utf8(body));

		assertEquals(traceConfiguration.getMaxArrayLength(), ((ArrayNode) result.getRoot().get("arr")).size());
	}

	@Test
	void testParse_arrayLengthOverLimit_rejected() {
		String body = buildArrayOf(traceConfiguration.getMaxArrayLength() + 1);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("max_array_length_exceeded", ex.getReason());
	}

	@Test
	void testParse_objectMemberCountAtLimit_accepted() {
		String body = buildObjectWithMembers(traceConfiguration.getMaxObjectMembers());

		ParsedAssertion result = parser.parse(utf8(body));

		assertEquals(traceConfiguration.getMaxObjectMembers(), result.getRoot().size());
	}

	@Test
	void testParse_objectMemberCountOverLimit_rejected() {
		String body = buildObjectWithMembers(traceConfiguration.getMaxObjectMembers() + 1);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("max_object_members_exceeded", ex.getReason());
	}

	@Test
	void testParse_stringLengthOverLimit_rejected() {
		traceConfiguration.setMaxStringLength(5);
		String body = "{\"a\":\"abcdef\"}";

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("json_limit_exceeded", ex.getReason());
	}

	@Test
	void testParse_duplicateKey_rejected() {
		String body = "{\"a\":1,\"a\":2}";

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("malformed_json", ex.getReason());
	}

	@Test
	void testParse_trailingContent_rejected() {
		String body = "{}{}";

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("trailing_content", ex.getReason());
	}

	@Test
	void testParse_rootNotObject_rejected() {
		String body = "[1,2,3]";

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("root_not_object", ex.getReason());
	}

	@Test
	void testParse_malformedJsonSyntax_rejected() {
		String body = "{\"a\":}";

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("malformed_json", ex.getReason());
	}

	@Test
	void testParse_malformedUtf8_rejected() {
		byte[] bytes = { '{', (byte) 0xFF, '}' };

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> parser.parse(new ByteArrayInputStream(bytes)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("malformed_utf8", ex.getReason());
	}

	@Test
	void testParse_utf8Bom_rejected() {
		byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
		byte[] combined = new byte[bom.length + body.length];
		System.arraycopy(bom, 0, combined, 0, bom.length);
		System.arraycopy(body, 0, combined, bom.length, body.length);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> parser.parse(new ByteArrayInputStream(combined)));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("utf8_bom", ex.getReason());
	}

	@Test
	void testParse_unreadableBody_rejected() {
		InputStream throwing = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("boom");
			}
		};

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(throwing));

		assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
		assertEquals("unreadable_body", ex.getReason());
	}

	// -- assertion_raw fidelity: the decoded text must re-encode to the received bytes ---------------

	@Test
	void testParse_variedUnicodeNumbersWhitespaceAndOrder_rawTextReEncodesToInputBytes() {
		String body = "  {\"z\":\"\uD83D\uDE00\",\n\t\"a\":\"\\ud83d\\ude00\" , \"nfc\":\"\u00e9\",\"nfd\":\"e\u0301\","
				+ "\"esc_nfd\":\"e\\u0301\",\"nul\":\"\\u0000\",\"us\":\"\\u001f\",\"ls\":\"\u2028\u2029\","
				+ "\"n\":[1.0,1.50,1e2,1E+2,-0,-0.0,9007199254740991,9007199254740993,123456789012345678901234567890]}\r\n  ";
		byte[] input = body.getBytes(StandardCharsets.UTF_8);

		ParsedAssertion result = parser.parse(new ByteArrayInputStream(input));

		assertArrayEquals(input, result.getRawText().getBytes(StandardCharsets.UTF_8));
		assertEquals(body, result.getRawText());
		assertEquals(-1, result.getRawText().indexOf('\u0000'));
		assertTrue(result.getRawText().contains("\\u0000"));
		assertEquals("z", result.getRoot().fieldNames().next());
	}

	@Test
	void testParse_escapedAndLiteralFormsOfSameCharacter_equalValuesDifferentRawText() {
		String literal = "{\"s\":\"\uD83D\uDE00 \u00e9\"}";
		String escaped = "{\"s\":\"\\ud83d\\ude00 \\u00e9\"}";

		ParsedAssertion fromLiteral = parser.parse(utf8(literal));
		ParsedAssertion fromEscaped = parser.parse(utf8(escaped));

		assertEquals(fromLiteral.getRoot().get("s").textValue(), fromEscaped.getRoot().get("s").textValue());
		assertEquals(fromLiteral.getRoot(), fromEscaped.getRoot());
		assertEquals(literal, fromLiteral.getRawText());
		assertEquals(escaped, fromEscaped.getRawText());
	}

	@Test
	void testParse_combiningSequenceAndPrecomposedForm_notNormalized() {
		ParsedAssertion result = parser.parse(utf8("{\"nfc\":\"\u00e9\",\"nfd\":\"e\u0301\"}"));

		assertEquals("\u00e9", result.getRoot().get("nfc").textValue());
		assertEquals("e\u0301", result.getRoot().get("nfd").textValue());
	}

	@Test
	void testParse_integersAtAndBeyondSafeRange_acceptedWithExactDigits() {
		ParsedAssertion result = parser.parse(
				utf8("{\"safe\":9007199254740991,\"over\":9007199254740993,\"huge\":123456789012345678901234567890}"));

		assertEquals(9007199254740991L, result.getRoot().get("safe").longValue());
		assertEquals(9007199254740993L, result.getRoot().get("over").longValue());
		assertEquals(new java.math.BigInteger("123456789012345678901234567890"),
				result.getRoot().get("huge").bigIntegerValue());
	}

	@Test
	void testParse_loneSurrogateEscape_rejectedNotCanonicalizable() {
		for (String body : new String[] { "{\"s\":\"\\ud800\"}", "{\"s\":\"\\udc00\"}",
				"{\"s\":\"\\ud83d x\"}", "{\"s\":\"\\ude00\\ud83d\"}" }) {
			TraceValidationException ex = assertThrows(TraceValidationException.class, () -> parser.parse(utf8(body)),
					body);

			assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
			assertEquals("not_canonicalizable", ex.getReason(), body);
		}
	}

	@Test
	void testParse_surrogateAndOverlongBytes_rejectedMalformedUtf8() {
		byte[][] bodies = {
				{ '{', '"', 's', '"', ':', '"', (byte) 0xED, (byte) 0xA0, (byte) 0x80, '"', '}' },
				{ '{', '"', 's', '"', ':', '"', (byte) 0xC0, (byte) 0x80, '"', '}' },
				{ '{', '"', 's', '"', ':', '"', (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80, '"', '}' } };

		for (byte[] bytes : bodies) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> parser.parse(new ByteArrayInputStream(bytes)));

			assertEquals("malformed_utf8", ex.getReason());
		}
	}

	@Test
	void testParse_literalControlCharacterInString_rejectedMalformedJson() {
		for (char control : new char[] { '\u0000', '\u0009', '\n', '\u001f' }) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> parser.parse(utf8("{\"s\":\"a" + control + "b\"}")));

			assertEquals("malformed_json", ex.getReason());
		}
	}

	@Test
	void testParse_numberOverflowingDouble_rejected() {
		for (String number : new String[] { "1e400", "-1e400" }) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> parser.parse(utf8("{\"n\":" + number + "}")), number);

			assertEquals(TraceValidationException.ERROR_INVALID_REQUEST, ex.getErrorId());
			assertEquals("not_canonicalizable", ex.getReason(), number);
		}
	}

	@Test
	void testParse_nonJsonNumberLiterals_rejectedMalformedJson() {
		for (String number : new String[] { "NaN", "Infinity", "-Infinity", "+1", "01", ".5", "1.", "0x10" }) {
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> parser.parse(utf8("{\"n\":" + number + "}")), number);

			assertEquals("malformed_json", ex.getReason(), number);
		}
	}

	@Test
	void testParse_duplicateKeyWrittenWithEscape_rejected() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> parser.parse(utf8("{\"a\":1,\"\\u0061\":2}")));

		assertEquals("malformed_json", ex.getReason());
	}

}
