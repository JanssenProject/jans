/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * RFC 8785 conformance tests for {@link JcsCanonicalizer}.
 */
class JcsCanonicalizerTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String RESOURCE_DIR = "/trace/jcs/";

	/** Resource pairs {@code <name>.input.json} / {@code <name>.output.json}. */
	static Stream<Arguments> rfcVectors() {
		return Stream.of("rfc-3.2.2-primitives", "rfc-3.2.3-sorting", "arrays", "french", "structures", "unicode",
				"weird").map(Arguments::of);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rfcVectors")
	void testCanonicalize_rfcVectors_matchExpected(String vector) throws IOException {
		JsonNode input = MAPPER.readTree(readResource(vector + ".input.json"));
		String expected = stripTrailingNewline(readResource(vector + ".output.json"));

		assertEquals(expected, JcsCanonicalizer.canonicalize(input));
		assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), JcsCanonicalizer.canonicalizeToUtf8(input));
	}

	@Test
	void testCanonicalizeToUtf8_rfcWireFormatSample_matchesBytes() throws IOException {
		// RFC 8785 section 3.2.4: expected UTF-8 bytes of the section 3.2.2 sample
		String hex = "7b226c69746572616c73223a5b6e756c6c2c747275652c66616c73655d2c226e756d626572"
				+ "73223a5b3333333333333333332e333333333333332c31652b33302c342e352c302e3030322c31652d3237"
				+ "5d2c22737472696e67223a22e282ac245c75303030665c6e4127425c225c5c5c5c5c222f227d";
		JsonNode input = MAPPER.readTree(readResource("rfc-3.2.2-primitives.input.json"));

		assertArrayEquals(hexToBytes(hex), JcsCanonicalizer.canonicalizeToUtf8(input));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			// RFC 8785 Appendix B, Table 1 (IEEE 754 bit pattern -> JSON representation)
			"0000000000000000, 0",
			"8000000000000000, 0",
			"0000000000000001, 5e-324",
			"8000000000000001, -5e-324",
			"7fefffffffffffff, 1.7976931348623157e+308",
			"ffefffffffffffff, -1.7976931348623157e+308",
			"4340000000000000, 9007199254740992",
			"c340000000000000, -9007199254740992",
			"4430000000000000, 295147905179352830000",
			"44b52d02c7e14af5, 9.999999999999997e+22",
			"44b52d02c7e14af6, 1e+23",
			"44b52d02c7e14af7, 1.0000000000000001e+23",
			"444b1ae4d6e2ef4e, 999999999999999700000",
			"444b1ae4d6e2ef4f, 999999999999999900000",
			"444b1ae4d6e2ef50, 1e+21",
			"3eb0c6f7a0b5ed8c, 9.999999999999997e-7",
			"3eb0c6f7a0b5ed8d, 0.000001",
			"41b3de4355555553, 333333333.3333332",
			"41b3de4355555554, 333333333.33333325",
			"41b3de4355555555, 333333333.3333333",
			"41b3de4355555556, 333333333.3333334",
			"41b3de4355555557, 333333333.33333343",
			"becbf647612f3696, -0.0000033333333333333333",
			"43143ff3c1cb0959, 1424953923781206.2" })
	void testCanonicalize_numbers_rfcAppendixB(String ieee754Hex, String expected) {
		double value = Double.longBitsToDouble(Long.parseUnsignedLong(ieee754Hex, 16));

		assertEquals(expected, JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(value)));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({
			// JSON text as Jackson parses it (IntNode / LongNode / BigIntegerNode / DoubleNode)
			"0, 0",
			"-0, 0",
			"-0.0, 0",
			"1, 1",
			"-1, -1",
			"1.5, 1.5",
			"1e21, 1e+21",
			"1e+21, 1e+21",
			"1E21, 1e+21",
			"1e20, 100000000000000000000",
			"123456789012345680000, 123456789012345680000",
			"123456789012345680000.0, 123456789012345680000",
			"1e-7, 1e-7",
			"1.5e-10, 1.5e-10",
			"0.000001, 0.000001",
			"0.0000001, 1e-7",
			"5e-324, 5e-324",
			"1.7976931348623157e308, 1.7976931348623157e+308",
			"9007199254740993, 9007199254740993",
			"-9007199254740993, -9007199254740993",
			"9223372036854775807, 9223372036854775807",
			"9007199254740993.0, 9007199254740992",
			"0.30000000000000004, 0.30000000000000004",
			"0.1, 0.1",
			"100.0, 100",
			"2e-3, 0.002",
			"4.50, 4.5" })
	void testCanonicalize_numbers_es6Formatting(String json, String expected) throws IOException {
		assertEquals(expected, JcsCanonicalizer.canonicalize(MAPPER.readTree(json)));
	}

	@Test
	void testCanonicalize_numbers_computedDoubleUsesShortestRoundTrip() {
		double sum = 0.1 + 0.2;

		assertEquals("0.30000000000000004", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(sum)));
		assertEquals("0.1", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(0.1d)));
		assertEquals("-1.5", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(-1.5d)));
	}

	@Test
	void testCanonicalize_numbers_bigIntegerBeyondSafeRangeKeepsExactDigits() {
		BigInteger huge = new BigInteger("123456789012345678901234567890");

		assertEquals("123456789012345678901234567890",
				JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(huge)));
	}

	@Test
	void testCanonicalize_numbers_bigDecimalNodeUsesDoubleValue() {
		assertEquals("4.5", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(new BigDecimal("4.50"))));
		assertEquals("1e+30", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(new BigDecimal("1E30"))));
		assertEquals("42", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(new BigDecimal("42"))));
		// lossy by design: more than 17 significant digits collapse to the nearest double
		assertEquals("0.30000000000000004", JcsCanonicalizer
				.canonicalize(JsonNodeFactory.instance.numberNode(new BigDecimal("0.300000000000000044408920985006"))));
	}

	@Test
	void testCanonicalize_numbers_floatNodeUsesExactDoubleValue() {
		assertEquals("0.10000000149011612",
				JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(0.1f)));
		assertEquals("1.5", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(1.5f)));
	}

	@Test
	void testCanonicalize_strings_escaping() {
		StringBuilder controls = new StringBuilder();
		for (char c = 0; c < 0x20; c++) {
			controls.append(c);
		}
		String expectedControls = "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\f\\r"
				+ "\\u000e\\u000f\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019"
				+ "\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f";

		assertEquals('"' + expectedControls + '"',
				JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode(controls.toString())));

		// quote and backslash are escaped; everything else is literal
		assertEquals("\"a\\\"b\\\\c\"", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("a\"b\\c")));
		assertEquals("\"/\"", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("/")));
		assertEquals("\"\"", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("")));
		assertEquals("\"€\"", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("€")));
		assertEquals("\"😀\"",
				JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("😀")));
		assertEquals("\"  \"",
				JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("  ")));
		assertEquals("\"\"", JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("")));

		// UTF-8 output, no BOM
		byte[] euro = JcsCanonicalizer.canonicalizeToUtf8(JsonNodeFactory.instance.textNode("€"));
		assertArrayEquals(new byte[] { '"', (byte) 0xe2, (byte) 0x82, (byte) 0xac, '"' }, euro);
	}

	@Test
	void testCanonicalize_strings_loneSurrogate_throws() {
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("a\ud800b")));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("\udc00")));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.textNode("\ud800")));
	}

	@Test
	void testCanonicalize_keyOrder_utf16CodeUnits() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("€", "euro");
		node.put("😀", "emoji");
		node.put("＠", "fullwidth at");
		node.put("a", "lower a");
		node.put("A", "upper a");
		node.put("1", "one");

		// UTF-16 order puts the surrogate pair (D83D DE00) before U+FF20; code-point order would not
		assertEquals("{\"1\":\"one\",\"A\":\"upper a\",\"a\":\"lower a\",\"€\":\"euro\","
				+ "\"😀\":\"emoji\",\"＠\":\"fullwidth at\"}", JcsCanonicalizer.canonicalize(node));
	}

	@Test
	void testCanonicalize_keyOrder_shorterPrefixFirstAndNested() throws IOException {
		JsonNode node = MAPPER.readTree("{\"ab\":1,\"aa\":{\"b\":[{\"z\":1,\"y\":2}],\"a\":2},\"a\":3,\"\":4}");

		assertEquals("{\"\":4,\"a\":3,\"aa\":{\"a\":2,\"b\":[{\"y\":2,\"z\":1}]},\"ab\":1}",
				JcsCanonicalizer.canonicalize(node));
	}

	@Test
	void testCanonicalize_literalsAndEmptyContainers_noWhitespace() throws IOException {
		JsonNode node = MAPPER.readTree(" { \"b\" : [ true , false , null , { } , [ ] ] , \"a\" : null } ");

		assertEquals("{\"a\":null,\"b\":[true,false,null,{},[]]}", JcsCanonicalizer.canonicalize(node));
	}

	@Test
	void testCanonicalize_structurallyEqualNodes_identicalBytes() throws IOException {
		JsonNode first = MAPPER.readTree("{\"z\":[1,2.50,{\"k\":\"v\"}],\"a\":\"x\"}");
		JsonNode second = MAPPER.readTree("{ \"a\" : \"x\" , \"z\" : [ 1 , 2.5 , { \"k\" : \"v\" } ] }");

		assertEquals(first, second);
		assertArrayEquals(JcsCanonicalizer.canonicalizeToUtf8(first), JcsCanonicalizer.canonicalizeToUtf8(second));
	}

	@Test
	void testCanonicalize_nonFinite_throws() throws IOException {
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(Double.NaN)));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(Double.POSITIVE_INFINITY)));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(Double.NEGATIVE_INFINITY)));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.numberNode(Float.NaN)));

		ObjectMapper lenient = new ObjectMapper().enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());
		JsonNode nested = lenient.readTree("{\"a\":[1,{\"b\":NaN}]}");
		assertThrows(JcsException.class, () -> JcsCanonicalizer.canonicalize(nested));
	}

	@Test
	void testCanonicalize_unsupportedNodeTypes_throw() {
		assertThrows(JcsException.class, () -> JcsCanonicalizer.canonicalize(null));
		assertThrows(JcsException.class, () -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.missingNode()));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.binaryNode(new byte[] { 1 })));
		assertThrows(JcsException.class,
				() -> JcsCanonicalizer.canonicalize(JsonNodeFactory.instance.pojoNode(new Object())));
	}

	@Test
	void testWithoutField_removesOnlyTopLevelField_deepCopy() throws IOException {
		ObjectNode original = (ObjectNode) MAPPER.readTree(
				"{\"signature\":\"sig\",\"trace\":{\"signature\":\"inner\",\"x\":[1]},\"list\":[{\"signature\":1}]}");
		String originalCanonical = JcsCanonicalizer.canonicalize(original);

		JsonNode stripped = JcsCanonicalizer.withoutField(original, "signature");

		assertEquals("{\"list\":[{\"signature\":1}],\"trace\":{\"signature\":\"inner\",\"x\":[1]}}",
				JcsCanonicalizer.canonicalize(stripped));
		assertFalse(stripped.has("signature"));
		assertTrue(original.has("signature"));
		assertEquals(originalCanonical, JcsCanonicalizer.canonicalize(original));

		// deep copy: mutating nested nodes of the copy must not affect the original
		assertNotSame(original.get("trace"), stripped.get("trace"));
		((ObjectNode) stripped.get("trace")).put("x", "mutated");
		((ArrayNode) stripped.get("list")).removeAll();
		assertEquals(originalCanonical, JcsCanonicalizer.canonicalize(original));
	}

	@Test
	void testWithoutField_missingField_returnsEqualCopy() throws IOException {
		ObjectNode original = (ObjectNode) MAPPER.readTree("{\"a\":1}");

		JsonNode copy = JcsCanonicalizer.withoutField(original, "signature");

		assertEquals(original, copy);
		assertNotSame(original, copy);
	}

	@Test
	void testWithoutField_nullNode_throws() {
		assertThrows(JcsException.class, () -> JcsCanonicalizer.withoutField(null, "signature"));
	}

	private static String readResource(String name) throws IOException {
		try (InputStream in = JcsCanonicalizerTest.class.getResourceAsStream(RESOURCE_DIR + name)) {
			if (in == null) {
				throw new IOException("Missing test resource " + RESOURCE_DIR + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String stripTrailingNewline(String text) {
		int end = text.length();
		while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
			end--;
		}
		return text.substring(0, end);
	}

	private static byte[] hexToBytes(String hex) {
		byte[] out = new byte[hex.length() / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return out;
	}

}
