/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link TraceConstants#PRODUCER_PATTERN}: the {@code name/semver} producer id format
 * shared by assertion validation and producer-key/producer-chain registration.
 */
class TraceConstantsTest {

	@ParameterizedTest
	@ValueSource(strings = { //
			"cedarling/1.0.0", //
			"cedarling-fleet_1.eu/10.20.30", //
			"cedarling/1.0.0-alpha", //
			"cedarling/1.0.0-alpha.1", //
			"cedarling/1.0.0-0.3.7", //
			"cedarling/1.0.0-x-y-z.--", //
			"cedarling/1.0.0+001", //
			"cedarling/1.0.0+20130313144700", //
			"cedarling/1.0.0-alpha+001", //
			"cedarling/1.0.0-beta+exp.sha.5114f85" })
	void testProducerPattern_validSemver_matches(String producerId) {
		assertTrue(TraceConstants.PRODUCER_PATTERN.matcher(producerId).matches(), producerId);
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"not-a-producer", //
			"cedarling/", //
			"cedarling/1.0", //
			"cedarling/1.0.0.0", //
			"cedarling/v1.0.0", //
			"cedarling/1.0.0-", //
			"cedarling/1.0.0+", //
			"cedarling/1.0.0-alpha.", //
			"cedarling/1.0.0-alpha..1", //
			"cedarling/1.0.0+001-alpha+2", //
			"cedarling/1.0.0-al pha", //
			"-cedarling/1.0.0", //
			"a/b/1.0.0" })
	void testProducerPattern_invalid_doesNotMatch(String producerId) {
		assertFalse(TraceConstants.PRODUCER_PATTERN.matcher(producerId).matches(), producerId);
	}

	@ParameterizedTest
	@ValueSource(ints = { 128, 129 })
	void testProducerPattern_nameLengthBoundary(int nameLength) {
		StringBuilder name = new StringBuilder(nameLength);
		for (int i = 0; i < nameLength; i++) {
			name.append('a');
		}
		boolean matches = TraceConstants.PRODUCER_PATTERN.matcher(name + "/1.0.0").matches();

		if (nameLength <= 128) {
			assertTrue(matches);
		} else {
			assertFalse(matches);
		}
	}

}
