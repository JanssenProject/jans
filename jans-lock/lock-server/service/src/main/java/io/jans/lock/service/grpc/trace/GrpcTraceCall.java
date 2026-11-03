/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.grpc.util.GrpcStatusMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Runs one TRACE REST operation on behalf of a gRPC call: maps the successful REST entity to the
 * gRPC response message and translates a {@link WebApplicationException} to a gRPC status whose
 * description carries the structured TRACE error body.
 *
 * @author Yuriy Movchan
 */
final class GrpcTraceCall {

    private static final String INTERNAL_ERROR = "Internal error";

    private GrpcTraceCall() {
    }

    static <D, R> void execute(Logger log, String operation, StreamObserver<R> observer, Supplier<Response> call,
            Class<D> entityType, Function<D, R> mapper) {
        log.debug("gRPC {} called", operation);

        try {
            Response response = call.get();
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                observer.onError(toStatus(response).asRuntimeException());
                return;
            }

            R result = mapper.apply(entityType.cast(response.getEntity()));
            observer.onNext(result);
            observer.onCompleted();
        } catch (WebApplicationException e) {
            observer.onError(toStatus(e.getResponse()).asRuntimeException());
        } catch (Exception e) {
            log.error("Error processing gRPC {} request", operation, e);
            observer.onError(Status.INTERNAL.withDescription(INTERNAL_ERROR).asRuntimeException());
        }
    }

    static Status toStatus(Response response) {
        Status status = GrpcStatusMapper.fromHttpStatus(response.getStatusInfo().toEnum());
        Object entity = response.getEntity();
        String body = entity == null ? "" : entity.toString();
        if (Status.Code.ALREADY_EXISTS == status.getCode() && isAmbiguousIdentifier(body)) {
            status = Status.FAILED_PRECONDITION;
        }

        String description = body.isEmpty() ? response.getStatusInfo().getReasonPhrase() : body;
        return status.withDescription(description);
    }

    /**
     * A bare identifier used by several producers is a 409 but not an "already exists" condition.
     */
    private static boolean isAmbiguousIdentifier(String body) {
        return body.contains("\"" + TraceErrorResponseType.AMBIGUOUS_IDENTIFIER.getParameter() + "\"");
    }
}
