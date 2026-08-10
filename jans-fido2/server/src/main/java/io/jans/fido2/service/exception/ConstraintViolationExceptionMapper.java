/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.exception;

import java.util.stream.Collectors;

import io.jans.fido2.model.error.Fido2ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;

/**
 * Maps bean-validation failures (e.g. a missing/null request body annotated with {@code @NotNull})
 * to the FIDO2 conformance failure envelope. Without this, an empty body yields a default container
 * 400 whose shape the conformance suite rejects.
 */
@ApplicationScoped
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    private Logger log;

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations() == null ? null
                : ex.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
        if (message == null || message.trim().isEmpty()) {
            message = "Request validation failed";
        }
        log.error("Handled FIDO2 request validation failure: {}", message);

        return Response.status(Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Fido2ErrorResponse.failed(message).toJson())
                .build();
    }
}
