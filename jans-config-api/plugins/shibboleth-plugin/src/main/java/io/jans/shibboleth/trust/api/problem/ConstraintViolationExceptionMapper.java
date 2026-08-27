package io.jans.shibboleth.trust.api.problem;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String TYPE = "https://jans.io/shibboleth-idp/problems/validation_failed";
    private static final int STATUS = 400;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        List<Violation> violations = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {

            violations.add(new Violation(leafField(violation.getPropertyPath()), "constraint_violation",
                violation.getMessage()));
        }

        String instance = uriInfo != null ? uriInfo.getPath() : null;
        Problem problem = new Problem(TYPE, "Request validation failed", STATUS,
            "One or more fields failed validation", instance, "validation_failed", violations);

        return Response.status(STATUS)
            .type(DomainExceptionMapper.PROBLEM_JSON)
            .entity(problem)
            .build();
    }

    /** The property path is e.g. {@code createTrustRelationship.request.displayName}; keep the last node. */
    private static String leafField(Path propertyPath) {

        String leaf = null;
        for (Path.Node node : propertyPath) {

            leaf = node.getName();
        }
        return leaf;
    }
}
