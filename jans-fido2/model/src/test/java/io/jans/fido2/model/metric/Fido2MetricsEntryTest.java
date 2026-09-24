/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.metric;

import org.junit.jupiter.api.Test;

import io.jans.fido2.model.telemetry.NativeClientTelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the {@code nativeClientTelemetry} field added for #14607: absent by default (so an entry
 * persisted before this field existed, or a ceremony that carried no telemetry, reads back as
 * {@code null} rather than some default), and round-trips through the getter/setter the ORM layer
 * relies on for {@code @JsonObject} persistence.
 */
class Fido2MetricsEntryTest {

	@Test
	void nativeClientTelemetry_defaultsToNull() {
		Fido2MetricsEntry entry = new Fido2MetricsEntry();

		assertNull(entry.getNativeClientTelemetry());
	}

	@Test
	void nativeClientTelemetry_getterSetterRoundTrip() {
		Fido2MetricsEntry entry = new Fido2MetricsEntry();
		NativeClientTelemetry telemetry = new NativeClientTelemetry();
		telemetry.setPlatform("ios");
		telemetry.setClientCorrelationId("corr-1");

		entry.setNativeClientTelemetry(telemetry);

		assertEquals(telemetry, entry.getNativeClientTelemetry());
	}
}
