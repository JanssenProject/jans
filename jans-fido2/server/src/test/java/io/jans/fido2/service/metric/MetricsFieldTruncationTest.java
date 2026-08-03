/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.service.metric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for MetricsFieldTruncation
 *
 * @author Janssen Project
 * @version 1.0
 */
class MetricsFieldTruncationTest {

    private static final String FIELD = "userAgent";

    /**
     * Every call site wraps a nullable getter, so a null must pass straight through.
     * StringHelper.truncate would throw NPE here, which is why this class exists
     * rather than reusing it.
     */
    @Test
    void testNullValueIsReturnedUntouched() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        String result = truncation.apply(FIELD, null, 64);

        // Then
        assertNull(result, "null must survive unchanged so nullable getters stay safe to wrap");
        assertFalse(truncation.hasTruncations(), "a null is not a truncation");
    }

    @Test
    void testEmptyValueIsReturnedUntouched() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        String result = truncation.apply(FIELD, "", 64);

        // Then
        assertEquals("", result);
        assertFalse(truncation.hasTruncations());
    }

    /**
     * The boundary. A value of exactly the column width fits, so it must not be
     * touched; one character more must be cut. Getting this off by one either
     * silently shortens every value or lets the INSERT fail.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 63, 64})
    void testValuesUpToTheLimitAreReturnedUnchanged(int length) {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();
        String value = "a".repeat(length);

        // When
        String result = truncation.apply(FIELD, value, 64);

        // Then
        assertSame(value, result, "a value that fits must be returned as-is, not copied");
        assertFalse(truncation.hasTruncations(), "nothing was removed, so nothing to report");
    }

    @Test
    void testValueOneCharacterOverTheLimitIsTruncated() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        String result = truncation.apply(FIELD, "a".repeat(65), 64);

        // Then
        assertEquals(64, result.length(), "result must fit the column exactly");
        assertTrue(truncation.hasTruncations());
    }

    /**
     * The realistic post-fix case: a real browser user agent against the widened
     * 512-character column. This must not be shortened, otherwise the schema change
     * bought nothing.
     */
    @Test
    void testTypicalUserAgentFitsTheWidenedColumn() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

        // When
        String result = truncation.apply(FIELD, userAgent, 512);

        // Then
        assertSame(userAgent, result, "a real user agent must reach the database intact");
        assertFalse(truncation.hasTruncations());
    }

    @Test
    void testPathologicalValueIsCutToTheLimit() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        String result = truncation.apply(FIELD, "x".repeat(5000), 512);

        // Then
        assertEquals(512, result.length());
        assertTrue(truncation.hasTruncations());
    }

    /**
     * A naive substring can split a UTF-16 surrogate pair and leave a lone
     * surrogate, which MySQL rejects with error 1366 and which encodes to invalid
     * UTF-8 for the PostgreSQL driver -- one persistence failure traded for another.
     * The cut must back off a code unit instead.
     */
    @Test
    void testCutLandingOnASurrogatePairDoesNotSplitIt() {
        // Given: 63 ASCII characters then an emoji, so the 64-character cut falls
        // between the emoji's high and low surrogate
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();
        String value = "a".repeat(63) + "😀" + "tail";

        // When
        String result = truncation.apply(FIELD, value, 64);

        // Then
        assertEquals(63, result.length(), "the cut must back off rather than split the pair");
        assertFalse(Character.isHighSurrogate(result.charAt(result.length() - 1)),
                "result must not end on an unpaired high surrogate");
        assertTrue(truncation.hasTruncations());
    }

    @Test
    void testSurrogatePairIsKeptWhenItFitsWholly() {
        // Given: the emoji ends exactly on the limit
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();
        String value = "a".repeat(62) + "😀" + "tail";

        // When
        String result = truncation.apply(FIELD, value, 64);

        // Then
        assertEquals(64, result.length());
        assertTrue(Character.isLowSurrogate(result.charAt(63)),
                "a pair that fits must be kept whole");
    }

    /**
     * Limits are expressed in characters. PostgreSQL "character varying(n)" and
     * MySQL VARCHAR(n) both count characters rather than bytes, so a multibyte
     * string needs no byte-level adjustment.
     */
    @Test
    void testMultibyteCharactersAreCountedAsCharacters() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        String result = truncation.apply(FIELD, "é".repeat(100), 64);

        // Then
        assertEquals(64, result.length(), "the limit counts characters, not bytes");
    }

    @Test
    void testEveryTruncatedFieldIsReported() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        truncation.apply("userAgent", "a".repeat(600), 512);
        truncation.apply("sessionId", "b".repeat(200), 128);

        // Then
        String description = truncation.describe();
        assertTrue(description.contains("userAgent"), "description must name every truncated field");
        assertTrue(description.contains("sessionId"), "description must name every truncated field");
        assertTrue(description.contains("600"), "description must carry the original length");
        assertTrue(description.contains("512"), "description must carry the limit that was applied");
    }

    /**
     * User agents, usernames, session ids and IP addresses are PII. The warning must
     * carry field names and lengths only, or it leaks them into logs that are not
     * protected the way the metrics API is.
     */
    @Test
    void testDescriptionNeverContainsTheValue() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();
        String sensitive = "sensitive-session-token-" + "z".repeat(200);

        // When
        truncation.apply("sessionId", sensitive, 128);

        // Then
        assertFalse(truncation.describe().contains("sensitive-session-token"),
                "the value is PII and must never reach the log");
    }

    @Test
    void testNothingIsReportedWhenNothingWasTruncated() {
        // Given
        MetricsFieldTruncation truncation = new MetricsFieldTruncation();

        // When
        truncation.apply("userAgent", "short", 512);
        truncation.apply("sessionId", null, 128);

        // Then
        assertFalse(truncation.hasTruncations());
        assertEquals("", truncation.describe());
    }
}
