/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.fido2.model.error.Fido2ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;

/**
 * Maps malformed request bodies (invalid JSON, bad base64url, unknown enum values, etc.) to the
 * FIDO2 conformance failure envelope instead of a container-generated error page. The conformance
 * suite sends many such negative vectors and expects a {@code {status:"failed", errorMessage:...}}
 * response rather than a stack trace.
 */
@ApplicationScoped
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Inject
    private Logger log;

    @Override
    public Response toResponse(JsonProcessingException ex) {
        log.error("Handled malformed FIDO2 request body", ex);

        return Response.status(Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Fido2ErrorResponse.failed("Invalid request: " + ex.getOriginalMessage()).toJson())
                .build();
    }
}
