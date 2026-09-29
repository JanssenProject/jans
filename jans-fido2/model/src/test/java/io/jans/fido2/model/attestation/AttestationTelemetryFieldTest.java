/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.jans.fido2.model.telemetry.NativeClientTelemetry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the "optional — absence must not change behavior" requirement from #14607 on the two
 * registration ("start"/"finish") request DTOs: a request with no {@code telemetry} key deserializes
 * exactly as it did before this field existed, and a request that does carry one populates it without
 * disturbing anything else.
 */
class AttestationTelemetryFieldTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void attestationOptions_withoutTelemetry_deserializesUnaffected() {
		String json = "{\"username\":\"alice\",\"displayName\":\"Alice\"}";

		AttestationOptions options = assertDoesNotThrow(() -> mapper.readValue(json, AttestationOptions.class));

		assertEquals("alice", options.getUsername());
		assertNull(options.getTelemetry());
	}

	@Test
	void attestationOptions_withTelemetry_populatesIt() throws Exception {
		String json = "{\"username\":\"alice\",\"telemetry\":{\"platform\":\"android\",\"client_correlation_id\":\"corr-1\"}}";

		AttestationOptions options = mapper.readValue(json, AttestationOptions.class);

		NativeClientTelemetry telemetry = options.getTelemetry();
		assertEquals("android", telemetry.getPlatform());
		assertEquals("corr-1", telemetry.getClientCorrelationId());
	}

	@Test
	void attestationResult_withoutTelemetry_deserializesUnaffected() {
		String json = "{\"id\":\"cred-1\",\"rawId\":\"cred-1\",\"type\":\"public-key\"}";

		AttestationResult result = assertDoesNotThrow(() -> mapper.readValue(json, AttestationResult.class));

		assertEquals("cred-1", result.getId());
		assertNull(result.getTelemetry());
	}

	@Test
	void attestationResult_withTelemetry_populatesIt() throws Exception {
		String json = "{\"id\":\"cred-1\",\"telemetry\":{\"native_api\":\"credential-manager\","
				+ "\"last_client_error_code\":\"GetCredentialCancellationException\"}}";

		AttestationResult result = mapper.readValue(json, AttestationResult.class);

		NativeClientTelemetry telemetry = result.getTelemetry();
		assertEquals("credential-manager", telemetry.getNativeApi());
		assertEquals("GetCredentialCancellationException", telemetry.getLastClientErrorCode());
	}
}
