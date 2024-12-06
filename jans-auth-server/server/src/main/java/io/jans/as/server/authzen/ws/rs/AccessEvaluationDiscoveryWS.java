package io.jans.as.server.authzen.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.gluu.GluuErrorResponseType;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.util.ServerUtil;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Yuriy Z
 */
@Path("/authzen-configuration")
public class AccessEvaluationDiscoveryWS {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AccessEvaluationDiscoveryService accessEvaluationDiscoveryService;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDiscovery(@Context HttpServletRequest httpRequest, @Context HttpServletResponse httpResponse) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.ACCESS_EVALUATION);

        try {
            final ExecutionContext context = new ExecutionContext(httpRequest, httpResponse);
            final JSONObject response = accessEvaluationDiscoveryService.discovery(context);
            final String entity = ServerUtil.toPrettyJson(response).replace("\\/", "/");
            log.trace("AuthZen Discovery: {}", entity);

            return Response.ok(entity).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, GluuErrorResponseType.SERVER_ERROR, "Internal error.");
        }
    }
}
