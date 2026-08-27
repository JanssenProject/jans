package io.jans.shibboleth.trust.api.problem;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    static final String PROBLEM_JSON = "application/problem+json";

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(DomainException exception) {

        String instance = uriInfo != null ? uriInfo.getPath() : null;
        Problem problem = ProblemMapper.toProblem(exception.getError(), instance);

        return Response.status(problem.getStatus())
            .type(PROBLEM_JSON)
            .entity(problem)
            .build();
    }
}
