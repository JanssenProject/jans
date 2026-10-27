/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.config.TraceConfiguration;

class GrpcLimitsTest {

	@Test
	void testMaxInboundMessageSize_FollowsConfiguredBulkLimit() {
		TraceConfiguration configuration = new TraceConfiguration();
		configuration.setMaxBulkRequestBytes(8 * 1024 * 1024);

		assertEquals(8 * 1024 * 1024 + GrpcLimits.ENVELOPE_OVERHEAD_BYTES, GrpcLimits.maxInboundMessageSize(configuration));
	}

	@Test
	void testMaxInboundMessageSize_NeverExceedsHardBulkCeiling() {
		TraceConfiguration configuration = new TraceConfiguration();
		configuration.setMaxBulkRequestBytes(Integer.MAX_VALUE - GrpcLimits.ENVELOPE_OVERHEAD_BYTES);

		assertEquals(TraceConfiguration.HARD_MAX_BULK_REQUEST_BYTES + GrpcLimits.ENVELOPE_OVERHEAD_BYTES,
				GrpcLimits.maxInboundMessageSize(configuration));
	}

	@Test
	void testMaxInboundMessageSize_DefaultWithoutConfiguration() {
		assertEquals(TraceConfiguration.DEFAULT_MAX_BULK_REQUEST_BYTES + GrpcLimits.ENVELOPE_OVERHEAD_BYTES,
				GrpcLimits.maxInboundMessageSize(null));
	}
}
