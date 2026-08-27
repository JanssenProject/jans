package io.jans.shibboleth.trust.api.problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(ThrowableExceptionMapper.class);
    private static final String TYPE_BASE = "https://jans.io/shibboleth-idp/problems/";

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable throwable) {

        String instance = uriInfo != null ? uriInfo.getPath() : null;

        if (throwable instanceof WebApplicationException) {

            Response original = ((WebApplicationException) throwable).getResponse();
            int status = original.getStatus();
            Problem problem = new Problem(TYPE_BASE + "request_failed", reasonFor(status), status,
                throwable.getMessage(), instance, "request_failed", null);
            return Response.status(status)
                .type(DomainExceptionMapper.PROBLEM_JSON)
                .entity(problem)
                .build();
        }

        LOG.error("Unhandled exception while serving trust configuration request", throwable);
        Problem problem = new Problem(TYPE_BASE + "internal_server_error", "Internal server error", 500,
            "An unexpected error occurred", instance, "internal_server_error", null);
        return Response.status(500)
            .type(DomainExceptionMapper.PROBLEM_JSON)
            .entity(problem)
            .build();
    }

    private static String reasonFor(int status) {

        Response.Status resolved = Response.Status.fromStatusCode(status);
        return resolved != null ? resolved.getReasonPhrase() : "Request failed";
    }
}
