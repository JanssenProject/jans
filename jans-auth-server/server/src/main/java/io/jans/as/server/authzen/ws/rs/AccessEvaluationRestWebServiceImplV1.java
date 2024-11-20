package io.jans.as.server.authzen.ws.rs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
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
 * @author Yuriy Z
 */
@Path("/access/v1")
public class AccessEvaluationRestWebServiceImplV1 {

    public static final String X_REQUEST_ID = "X-Request-ID";

    @Inject
    private Logger log;

    @Inject
    private AccessEvaluationService accessEvaluationService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @POST
    @Path("/evaluation")
    @Produces({MediaType.APPLICATION_JSON})
    public Response evaluation(String requestParams, @Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {

        log.trace("/evaluation - request params: {}", requestParams);

        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

            String requestId = httpRequest.getHeader(X_REQUEST_ID);
            String authorization = httpRequest.getHeader("Authorization");

            accessEvaluationService.validateAuthorization(authorization);

            AccessEvaluationRequest request = readRequest(requestParams);

            ExecutionContext executionContext = ExecutionContext.of(httpRequest, httpResponse).setRequestId(requestId);
            AccessEvaluationResponse response = accessEvaluationService.evaluation(request, executionContext);

            final String responseAsString = ServerUtil.asJson(response);

            log.trace("/evaluation - response entity: {}", responseAsString);
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

    protected AccessEvaluationRequest readRequest(String requestParams) {
        try {
            return ServerUtil.createJsonMapper().readValue(requestParams, AccessEvaluationRequest.class);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to parse request json: %s", requestParams);
            log.error(msg, e);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(msg)
                    .type(MediaType.APPLICATION_JSON_TYPE).build());
        }
    }
}
