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
        String description = entity == null || entity.toString().isEmpty() ? response.getStatusInfo().getReasonPhrase()
                : entity.toString();
        return status.withDescription(description);
    }
}
