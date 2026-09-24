/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the wire contract for the optional native-client telemetry envelope (#14607): every field
 * round-trips under its documented snake_case key, unknown JSON fields are tolerated (a newer SDK
 * sending a field this server version doesn't know about must not break the request), and
 * enum-shaped fields ({@code platform}/{@code native_api}/{@code flow_context}) accept an
 * unrecognized value rather than rejecting it — they are plain strings, not validated against a
 * closed set, exactly because "optional — absence must not change behavior" extends to "a value this
 * server doesn't recognize yet" too.
 */
class NativeClientTelemetryTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void getterSetterRoundTripCoversEveryField() {
		NativeClientTelemetry telemetry = new NativeClientTelemetry();
		telemetry.setClientCorrelationId("corr-123");
		telemetry.setPlatform("android");
		telemetry.setNativeApi("credential-manager");
		telemetry.setOsVersion("14");
		telemetry.setPlayServicesVersion("24.40");
		telemetry.setDeviceManufacturer("Samsung");
		telemetry.setDeviceModel("Galaxy S23");
		telemetry.setCredentialProvider("google-password-manager");
		telemetry.setDeviceSecure(true);
		telemetry.setFlowContext("native");
		telemetry.setAppVersion("6.2.1");
		telemetry.setDistributionChannel("play-store");
		telemetry.setLastClientErrorCode("GetCredentialCancellationException");

		assertEquals("corr-123", telemetry.getClientCorrelationId());
		assertEquals("android", telemetry.getPlatform());
		assertEquals("credential-manager", telemetry.getNativeApi());
		assertEquals("14", telemetry.getOsVersion());
		assertEquals("24.40", telemetry.getPlayServicesVersion());
		assertEquals("Samsung", telemetry.getDeviceManufacturer());
		assertEquals("Galaxy S23", telemetry.getDeviceModel());
		assertEquals("google-password-manager", telemetry.getCredentialProvider());
		assertEquals(Boolean.TRUE, telemetry.getDeviceSecure());
		assertEquals("native", telemetry.getFlowContext());
		assertEquals("6.2.1", telemetry.getAppVersion());
		assertEquals("play-store", telemetry.getDistributionChannel());
		assertEquals("GetCredentialCancellationException", telemetry.getLastClientErrorCode());
	}

	@Test
	void jsonSerializationUsesTheDocumentedSnakeCaseKeys() throws Exception {
		String json = mapper.writeValueAsString(fullSample());

		assertTrue(json.contains("\"client_correlation_id\""));
		assertTrue(json.contains("\"platform\""));
		assertTrue(json.contains("\"native_api\""));
		assertTrue(json.contains("\"os_version\""));
		assertTrue(json.contains("\"play_services_version\""));
		assertTrue(json.contains("\"device_manufacturer\""));
		assertTrue(json.contains("\"device_model\""));
		assertTrue(json.contains("\"credential_provider\""));
		assertTrue(json.contains("\"is_device_secure\""));
		assertTrue(json.contains("\"flow_context\""));
		assertTrue(json.contains("\"app_version\""));
		assertTrue(json.contains("\"distribution_channel\""));
		assertTrue(json.contains("\"last_client_error_code\""));
	}

	@Test
	void jsonRoundTripPreservesEveryField() throws Exception {
		NativeClientTelemetry original = fullSample();

		String json = mapper.writeValueAsString(original);
		NativeClientTelemetry roundtripped = mapper.readValue(json, NativeClientTelemetry.class);

		assertEquals(original.getClientCorrelationId(), roundtripped.getClientCorrelationId());
		assertEquals(original.getPlatform(), roundtripped.getPlatform());
		assertEquals(original.getNativeApi(), roundtripped.getNativeApi());
		assertEquals(original.getOsVersion(), roundtripped.getOsVersion());
		assertEquals(original.getPlayServicesVersion(), roundtripped.getPlayServicesVersion());
		assertEquals(original.getDeviceManufacturer(), roundtripped.getDeviceManufacturer());
		assertEquals(original.getDeviceModel(), roundtripped.getDeviceModel());
		assertEquals(original.getCredentialProvider(), roundtripped.getCredentialProvider());
		assertEquals(original.getDeviceSecure(), roundtripped.getDeviceSecure());
		assertEquals(original.getFlowContext(), roundtripped.getFlowContext());
		assertEquals(original.getAppVersion(), roundtripped.getAppVersion());
		assertEquals(original.getDistributionChannel(), roundtripped.getDistributionChannel());
		assertEquals(original.getLastClientErrorCode(), roundtripped.getLastClientErrorCode());
	}

	@Test
	void emptyEnvelopeSerializesWithNoKeys() throws Exception {
		String json = mapper.writeValueAsString(new NativeClientTelemetry());

		assertEquals("{}", json, "@JsonInclude(NON_NULL) must omit every absent field, not emit nulls");
	}

	@Test
	void unknownJsonFieldsAreTolerated() {
		String json = "{"
				+ "\"client_correlation_id\":\"corr-123\","
				+ "\"some_field_a_newer_sdk_added\":\"x\","
				+ "\"another_unknown\":42"
				+ "}";

		NativeClientTelemetry telemetry = assertDoesNotThrow(
				() -> mapper.readValue(json, NativeClientTelemetry.class),
				"@JsonIgnoreProperties(ignoreUnknown=true) must allow extra fields");

		assertEquals("corr-123", telemetry.getClientCorrelationId());
	}

	@Test
	void unrecognizedEnumShapedValuesAreAcceptedNotRejected() throws Exception {
		String json = "{"
				+ "\"platform\":\"palmos\","
				+ "\"native_api\":\"some-future-api\","
				+ "\"flow_context\":\"quantum-teleport\""
				+ "}";

		NativeClientTelemetry telemetry = assertDoesNotThrow(
				() -> mapper.readValue(json, NativeClientTelemetry.class),
				"platform/native_api/flow_context are plain strings — an unrecognized value must "
						+ "deserialize, not reject the whole ceremony request over a telemetry field");

		assertEquals("palmos", telemetry.getPlatform());
		assertEquals("some-future-api", telemetry.getNativeApi());
		assertEquals("quantum-teleport", telemetry.getFlowContext());
	}

	@Test
	void isDeviceSecureAbsentDoesNotDefaultToFalse() {
		NativeClientTelemetry telemetry = new NativeClientTelemetry();

		assertFalse(telemetry.getDeviceSecure() != null && telemetry.getDeviceSecure(),
				"is_device_secure must stay null (unknown), not silently become false, when the client omits it");
	}

	private static NativeClientTelemetry fullSample() {
		NativeClientTelemetry telemetry = new NativeClientTelemetry();
		telemetry.setClientCorrelationId("corr-123");
		telemetry.setPlatform("android");
		telemetry.setNativeApi("credential-manager");
		telemetry.setOsVersion("14");
		telemetry.setPlayServicesVersion("24.40");
		telemetry.setDeviceManufacturer("Samsung");
		telemetry.setDeviceModel("Galaxy S23");
		telemetry.setCredentialProvider("google-password-manager");
		telemetry.setDeviceSecure(true);
		telemetry.setFlowContext("native");
		telemetry.setAppVersion("6.2.1");
		telemetry.setDistributionChannel("play-store");
		telemetry.setLastClientErrorCode("GetCredentialCancellationException");
		return telemetry;
	}
}
