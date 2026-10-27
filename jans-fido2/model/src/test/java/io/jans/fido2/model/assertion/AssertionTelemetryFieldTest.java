/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.jans.fido2.model.telemetry.NativeClientTelemetry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the "optional — absence must not change behavior" requirement from #14607 on the two
 * authentication ("start"/"finish") request DTOs: a request with no {@code telemetry} key
 * deserializes exactly as it did before this field existed, and a request that does carry one
 * populates it without disturbing anything else.
 */
class AssertionTelemetryFieldTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void assertionOptions_withoutTelemetry_deserializesUnaffected() {
		String json = "{\"username\":\"alice\"}";

		AssertionOptions options = assertDoesNotThrow(() -> mapper.readValue(json, AssertionOptions.class));

		assertEquals("alice", options.getUsername());
		assertNull(options.getTelemetry());
	}

	@Test
	void assertionOptions_withTelemetry_populatesIt() throws Exception {
		String json = "{\"username\":\"alice\",\"telemetry\":{\"platform\":\"ios\",\"client_correlation_id\":\"corr-2\"}}";

		AssertionOptions options = mapper.readValue(json, AssertionOptions.class);

		NativeClientTelemetry telemetry = options.getTelemetry();
		assertEquals("ios", telemetry.getPlatform());
		assertEquals("corr-2", telemetry.getClientCorrelationId());
	}

	@Test
	void assertionResult_withoutTelemetry_deserializesUnaffected() {
		String json = "{\"id\":\"cred-1\",\"rawId\":\"cred-1\"}";

		AssertionResult result = assertDoesNotThrow(() -> mapper.readValue(json, AssertionResult.class));

		assertEquals("cred-1", result.getId());
		assertNull(result.getTelemetry());
	}

	@Test
	void assertionResult_withTelemetry_populatesIt() throws Exception {
		String json = "{\"id\":\"cred-1\",\"telemetry\":{\"credential_provider\":\"google-password-manager\","
				+ "\"is_device_secure\":true}}";

		AssertionResult result = mapper.readValue(json, AssertionResult.class);

		NativeClientTelemetry telemetry = result.getTelemetry();
		assertEquals("google-password-manager", telemetry.getCredentialProvider());
		assertEquals(Boolean.TRUE, telemetry.getDeviceSecure());
	}
}
