/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.exception;

import io.jans.configapi.rest.model.ApiError;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Mougang T.Gasmyr
 */
@Provider
public class GlobalErrorHandler implements ExceptionMapper<Exception> {

    @Inject
    Logger log;

    public Response toResponse(Exception e) {
        log.error(e.getMessage(), e);
        if (e instanceof WebApplicationException && ((WebApplicationException) e).getResponse() != null) {
            return ((WebApplicationException) e).getResponse();
        } else if (e instanceof ConstraintViolationException
                && ((ConstraintViolationException) e).getMessage() != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        return Response.serverError()
                .entity(new ApiError.ErrorBuilder()
                        .withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                        .withMessage("Internal Server error")
                        .andDescription("Internal error occurs, for more details please check log files.").build())
                .build();
    }
}
