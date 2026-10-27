/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link TraceConstants#PRODUCER_PATTERN}: the opaque stable producer id grammar shared
 * by assertion validation and producer-key/producer-chain registration.
 */
class TraceConstantsTest {

	@ParameterizedTest
	@ValueSource(strings = { //
			"cedarling-fleet-1", //
			"gateway-fleet-1", //
			"effect-observer-1", //
			"jans-auth-server", //
			"cedarling-fleet-1/1.0.0", //
			"spiffe:abc", //
			"a", //
			"0", //
			"Cedarling_Fleet.eu:1/x-y" })
	void testProducerPattern_validStableId_matches(String producerId) {
		assertTrue(TraceConstants.PRODUCER_PATTERN.matcher(producerId).matches(), producerId);
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"cedarling/1.0.0", //
			"cedarling-fleet_1.eu/10.20.30", //
			"cedarling/1.0.0-alpha.1", //
			"cedarling/1.0.0-x-y-z.--", //
			"cedarling/1.0", //
			"cedarling/" })
	void testProducerPattern_legacyNameSemver_stillMatches(String producerId) {
		assertTrue(TraceConstants.PRODUCER_PATTERN.matcher(producerId).matches(), producerId);
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"", //
			".cedarling", //
			"-cedarling", //
			"_cedarling", //
			":cedarling", //
			"/cedarling", //
			" cedarling", //
			"cedarling ", //
			"cedar ling", //
			"cedarling\n", //
			"\ncedarling", //
			"cedar\tling", //
			"cedar\u0000ling", //
			"cedar\u007fling", //
			"cedar\u0001ling", //
			"cedarlïng", //
			"cedarlingé", //
			"сedarling", //
			"cedar@ling", //
			"cedar+ling", //
			"cedarling/1.0.0+001", //
			"cedarling/1.0.0-alpha+001" })
	void testProducerPattern_invalid_doesNotMatch(String producerId) {
		assertFalse(TraceConstants.PRODUCER_PATTERN.matcher(producerId).matches(), producerId);
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 255 })
	void testProducerPattern_lengthWithinBound_matches(int length) {
		assertTrue(TraceConstants.PRODUCER_PATTERN.matcher(repeat('a', length)).matches());
	}

	@ParameterizedTest
	@ValueSource(ints = { 256, 257, 1000 })
	void testProducerPattern_lengthOverBound_doesNotMatch(int length) {
		assertFalse(TraceConstants.PRODUCER_PATTERN.matcher(repeat('a', length)).matches());
	}

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}

}
