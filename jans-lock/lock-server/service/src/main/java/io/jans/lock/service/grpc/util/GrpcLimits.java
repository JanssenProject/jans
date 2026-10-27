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

    static final int ENVELOPE_OVERHEAD_BYTES = 64 * 1024;

    private GrpcLimits() {
    }

    /**
     * Largest inbound gRPC message: the configured TRACE bulk body limit plus protobuf envelope
     * overhead, so the TRACE limits, not the transport, reject an oversized bulk request. The server
     * builders read it once at startup, so a changed {@code maxBulkRequestBytes} needs a restart.
     * Messages are read only after the authorization interceptor has accepted the call.
     *
     * @param traceConfiguration TRACE configuration, may be {@code null}
     * @return the maximum inbound message size in bytes
     */
    public static int maxInboundMessageSize(TraceConfiguration traceConfiguration) {
        int maxBulkRequestBytes = traceConfiguration == null ? TraceConfiguration.DEFAULT_MAX_BULK_REQUEST_BYTES
                : traceConfiguration.getMaxBulkRequestBytes();
        return maxBulkRequestBytes + ENVELOPE_OVERHEAD_BYTES;
    }
}
