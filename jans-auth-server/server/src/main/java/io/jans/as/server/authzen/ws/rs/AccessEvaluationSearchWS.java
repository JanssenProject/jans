package io.jans.as.server.authzen.ws.rs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.authzen.*;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

/**
 * AuthZEN Search API v1.
 * Implements search endpoints per AuthZEN spec:
 * - POST /access/v1/search/subject
 * - POST /access/v1/search/resource
 * - POST /access/v1/search/action
 *
 * @author Yuriy Z
 */
@Path("/access/v1/search")
public class AccessEvaluationSearchWS {

    public static final String X_REQUEST_ID = "X-Request-ID";

    @Inject
    private Logger log;

    @Inject
    private AccessEvaluationSearchService accessEvaluationSearchService;

    @Inject
    private AccessEvaluationService accessEvaluationService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /**
     * Search subjects authorized for a given action on a resource.
     * POST /access/v1/search/subject
     */
    @POST
    @Path("/subject")
    @Produces({MediaType.APPLICATION_JSON})
    public Response searchSubject(String requestParams, @Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {

        log.trace("/search/subject - request params: {}", requestParams);

        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

            String requestId = httpRequest.getHeader(X_REQUEST_ID);
            String authorization = httpRequest.getHeader("Authorization");

            accessEvaluationService.validateAuthorization(authorization);

            SearchSubjectRequest request = readSearchSubjectRequest(requestParams);

            ExecutionContext executionContext = ExecutionContext.of(httpRequest, httpResponse).setRequestId(requestId);
            SearchResponse<Subject> response = accessEvaluationSearchService.searchSubject(request, executionContext);

            final String responseAsString = ServerUtil.asJson(response);

            log.trace("/search/subject - response entity: {}", responseAsString);
            return Response.status(Response.Status.OK)
                    .entity(responseAsString)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(X_REQUEST_ID, requestId)
                    .build();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    /**
     * Search resources a subject is authorized to access for a given action.
     * POST /access/v1/search/resource
     */
    @POST
    @Path("/resource")
    @Produces({MediaType.APPLICATION_JSON})
    public Response searchResource(String requestParams, @Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {

        log.trace("/search/resource - request params: {}", requestParams);

        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

            String requestId = httpRequest.getHeader(X_REQUEST_ID);
            String authorization = httpRequest.getHeader("Authorization");

            accessEvaluationService.validateAuthorization(authorization);

            SearchResourceRequest request = readSearchResourceRequest(requestParams);

            ExecutionContext executionContext = ExecutionContext.of(httpRequest, httpResponse).setRequestId(requestId);
            SearchResponse<Resource> response = accessEvaluationSearchService.searchResource(request, executionContext);

            final String responseAsString = ServerUtil.asJson(response);

            log.trace("/search/resource - response entity: {}", responseAsString);
            return Response.status(Response.Status.OK)
                    .entity(responseAsString)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(X_REQUEST_ID, requestId)
                    .build();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    /**
     * Search actions a subject is authorized to perform on a resource.
     * POST /access/v1/search/action
     */
    @POST
    @Path("/action")
    @Produces({MediaType.APPLICATION_JSON})
    public Response searchAction(String requestParams, @Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {

        log.trace("/search/action - request params: {}", requestParams);

        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

            String requestId = httpRequest.getHeader(X_REQUEST_ID);
            String authorization = httpRequest.getHeader("Authorization");

            accessEvaluationService.validateAuthorization(authorization);

            SearchActionRequest request = readSearchActionRequest(requestParams);

            ExecutionContext executionContext = ExecutionContext.of(httpRequest, httpResponse).setRequestId(requestId);
            SearchResponse<Action> response = accessEvaluationSearchService.searchAction(request, executionContext);

            final String responseAsString = ServerUtil.asJson(response);

            log.trace("/search/action - response entity: {}", responseAsString);
            return Response.status(Response.Status.OK)
                    .entity(responseAsString)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .header(X_REQUEST_ID, requestId)
                    .build();
        } catch (WebApplicationException e) {
            if (log.isTraceEnabled()) {
                log.trace(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    protected SearchSubjectRequest readSearchSubjectRequest(String requestParams) {
        try {
            return ServerUtil.createJsonMapper().readValue(requestParams, SearchSubjectRequest.class);
        } catch (JsonProcessingException e) {
            log.error(String.format("Failed to parse search subject request json: %s", requestParams), e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Failed to parse search subject request")
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    protected SearchResourceRequest readSearchResourceRequest(String requestParams) {
        try {
            return ServerUtil.createJsonMapper().readValue(requestParams, SearchResourceRequest.class);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to parse search resource request json: %s", requestParams);
            log.error(msg, e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }

    protected SearchActionRequest readSearchActionRequest(String requestParams) {
        try {
            return ServerUtil.createJsonMapper().readValue(requestParams, SearchActionRequest.class);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to parse search action request json: %s", requestParams);
            log.error(msg, e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }
}
