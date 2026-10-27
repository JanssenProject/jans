/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jans.lock.model.trace.config.TraceConfiguration;

class GrpcLimitsTest {

	@Test
	void testMaxInboundMessageSize_FitsLargestBulkBody() {
		assertTrue(GrpcLimits.MAX_INBOUND_MESSAGE_SIZE > TraceConfiguration.HARD_MAX_BULK_REQUEST_BYTES);
	}
}
