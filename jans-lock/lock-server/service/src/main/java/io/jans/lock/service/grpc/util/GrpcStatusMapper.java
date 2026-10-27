/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.util;

import io.grpc.Status;
import jakarta.ws.rs.core.Response;

/**
 * Maps HTTP response statuses produced by the REST layer to gRPC statuses.
 *
 * @author Yuriy Movchan
 */
public final class GrpcStatusMapper {

    private GrpcStatusMapper() {
    }

    /**
     * Map HTTP status codes to gRPC status codes.
     *
     * @param httpStatus HTTP response status
     * @return corresponding gRPC Status
     */
    public static Status fromHttpStatus(Response.Status httpStatus) {
        if (httpStatus == null) {
            return Status.INTERNAL;
        }

        switch (httpStatus) {
            case UNAUTHORIZED:
                return Status.UNAUTHENTICATED;
            case FORBIDDEN:
                return Status.PERMISSION_DENIED;
            case BAD_REQUEST:
                return Status.INVALID_ARGUMENT;
            case NOT_FOUND:
                return Status.NOT_FOUND;
            case CONFLICT:
                return Status.ALREADY_EXISTS;
            case INTERNAL_SERVER_ERROR:
                return Status.INTERNAL;
            case SERVICE_UNAVAILABLE:
                return Status.UNAVAILABLE;
            default:
                return Status.UNKNOWN;
        }
    }
}
