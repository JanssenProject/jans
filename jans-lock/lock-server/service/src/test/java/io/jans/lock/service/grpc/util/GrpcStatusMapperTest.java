/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import jakarta.ws.rs.core.Response;

class GrpcStatusMapperTest {

	@Test
	void testFromHttpStatus_MapsKnownStatuses() {
		assertEquals(Status.Code.UNAUTHENTICATED, GrpcStatusMapper.fromHttpStatus(Response.Status.UNAUTHORIZED).getCode());
		assertEquals(Status.Code.PERMISSION_DENIED, GrpcStatusMapper.fromHttpStatus(Response.Status.FORBIDDEN).getCode());
		assertEquals(Status.Code.INVALID_ARGUMENT, GrpcStatusMapper.fromHttpStatus(Response.Status.BAD_REQUEST).getCode());
		assertEquals(Status.Code.NOT_FOUND, GrpcStatusMapper.fromHttpStatus(Response.Status.NOT_FOUND).getCode());
		assertEquals(Status.Code.ALREADY_EXISTS, GrpcStatusMapper.fromHttpStatus(Response.Status.CONFLICT).getCode());
		assertEquals(Status.Code.INTERNAL, GrpcStatusMapper.fromHttpStatus(Response.Status.INTERNAL_SERVER_ERROR).getCode());
		assertEquals(Status.Code.UNAVAILABLE, GrpcStatusMapper.fromHttpStatus(Response.Status.SERVICE_UNAVAILABLE).getCode());
	}

	@Test
	void testFromHttpStatus_NullIsInternalAndOtherIsUnknown() {
		assertEquals(Status.Code.INTERNAL, GrpcStatusMapper.fromHttpStatus(null).getCode());
		assertEquals(Status.Code.UNKNOWN, GrpcStatusMapper.fromHttpStatus(Response.Status.GONE).getCode());
	}
}
