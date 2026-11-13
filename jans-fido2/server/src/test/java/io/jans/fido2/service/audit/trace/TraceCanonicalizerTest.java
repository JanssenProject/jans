/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TraceCanonicalizerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String canon(String json) throws Exception {
        return TraceCanonicalizer.canonicalString(MAPPER.readTree(json));
    }

    @Test
    void numbersFollowRfc8785Examples() {
        // Values from RFC 8785 section 3.2.2.3 and Appendix B
        assertEquals("0", TraceCanonicalizer.formatNumber(0.0));
        assertEquals("0", TraceCanonicalizer.formatNumber(-0.0));
        assertEquals("4.5", TraceCanonicalizer.formatNumber(4.5));
        assertEquals("0.002", TraceCanonicalizer.formatNumber(2e-3));
        assertEquals("0.000001", TraceCanonicalizer.formatNumber(0.000001));
        assertEquals("1e-7", TraceCanonicalizer.formatNumber(1e-7));
        assertEquals("1e+30", TraceCanonicalizer.formatNumber(1e30));
        assertEquals("333333333.3333333", TraceCanonicalizer.formatNumber(333333333.33333329));
        assertEquals("9007199254740992", TraceCanonicalizer.formatNumber(9007199254740993.0));
        assertEquals("5e-324", TraceCanonicalizer.formatNumber(Double.MIN_VALUE));
        assertEquals("1.7976931348623157e+308", TraceCanonicalizer.formatNumber(Double.MAX_VALUE));
    }

    @Test
    void numbersInRecordsAreFormatted() throws Exception {
        assertEquals("{\"n\":1e+30}", canon("{\"n\":1E+30}"));
        assertEquals("{\"n\":9007199254740992}", canon("{\"n\":9007199254740993}"));
    }

    @Test
    void sortsMembersAndRemovesWhitespace() throws Exception {
        assertEquals("{\"a\":[true,null,\"x\"],\"b\":1}", canon("{ \"b\" : 1, \"a\" : [ true, null, \"x\" ] }"));
    }

    @Test
    void sortsNestedObjects() throws Exception {
        assertEquals("{\"o\":{\"a\":2,\"z\":1}}", canon("{\"o\":{\"z\":1,\"a\":2}}"));
    }

    @Test
    void escapesOnlyQuotesBackslashesAndControlCharacters() throws Exception {
        assertEquals("\"\\\"\\\\\\n\\r\\t\\b\\f\\u0001\\u001f/é\"",
                TraceCanonicalizer.canonicalString(MAPPER.getNodeFactory().textNode("\"\\\n\r\t\b\f\u0001\u001f/é")));
    }

    @Test
    void sortsByUtf16CodeUnits() throws Exception {
        // U+D83D (a surrogate) sorts before U+FF5E in UTF-16 code units, so the emoji key comes first
        assertEquals("{\"😀\":1,\"～\":2}", canon("{\"～\":2,\"😀\":1}"));
    }

    @Test
    void rejectsNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> TraceCanonicalizer.formatNumber(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> TraceCanonicalizer.formatNumber(Double.POSITIVE_INFINITY));
    }
}
