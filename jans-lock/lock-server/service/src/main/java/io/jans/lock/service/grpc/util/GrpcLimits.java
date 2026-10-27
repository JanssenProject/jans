/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.util;

import io.jans.lock.model.trace.config.TraceConfiguration;

/**
 * Transport limits shared by every gRPC server flavour (plain, TLS, servlet bridge).
 *
 * @author Yuriy Movchan
 */
public final class GrpcLimits {

    private static final int ENVELOPE_OVERHEAD_BYTES = 64 * 1024;

    /**
     * Largest inbound gRPC message: the TRACE bulk body ceiling plus protobuf envelope overhead, so
     * the configured TRACE limits, not the transport, reject an oversized bulk request. Messages are
     * read only after the authorization interceptor has accepted the call.
     */
    public static final int MAX_INBOUND_MESSAGE_SIZE = TraceConfiguration.HARD_MAX_BULK_REQUEST_BYTES
            + ENVELOPE_OVERHEAD_BYTES;

    private GrpcLimits() {
    }
}
