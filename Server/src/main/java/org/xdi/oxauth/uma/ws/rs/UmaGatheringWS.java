package org.xdi.oxauth.uma.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.uma.authorization.UmaGatherContext;
import org.xdi.oxauth.uma.authorization.UmaWebException;
import org.xdi.oxauth.uma.service.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.Response.Status.FOUND;
import static org.xdi.oxauth.model.uma.UmaErrorResponseType.INVALID_CLAIMS_GATHERING_SCRIPT_NAME;

/**
 * Claims-Gathering Endpoint.
 *
 * @author yuriyz on 06/04/2017.
 */
@Path("/uma/gather_claims")
public class UmaGatheringWS {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private UmaValidationService validationService;
    @Inject
    private ExternalUmaClaimsGatheringService external;
    @Inject
    private UmaSessionService sessionService;
    @Inject
    private UmaPermissionService permissionService;
    @Inject
    private UmaPctService pctService;
    @Inject
    private AppConfiguration appConfiguration;

    public Response gatherClaims(String clientId, String ticket, String claimRedirectUri, String state, Boolean reset,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            log.trace("gatherClaims client_id: {}, ticket: {}, claims_redirect_uri: {}, state: {}, queryString: {}",
                    clientId, ticket, claimRedirectUri, state, httpRequest.getQueryString());

            validationService.validateClientAndClaimsRedirectUri(clientId, claimRedirectUri, state);
            List<UmaPermission> permissions = validationService.validateTicketWithRedirect(ticket, claimRedirectUri, state);
            String[] scriptNames = validationService.validatesGatheringScriptNames(getScriptNames(permissions), claimRedirectUri, state);

            CustomScriptConfiguration script = external.determineScript(scriptNames);
            if (script == null) {
                log.error("Failed to determine claims-gathering script for names: " + Arrays.toString(scriptNames));
                throw new UmaWebException(claimRedirectUri, errorResponseFactory, INVALID_CLAIMS_GATHERING_SCRIPT_NAME, state);
            }

            SessionState session = sessionService.getSession(httpRequest, httpResponse);
            sessionService.configure(session, script.getName(), reset, permissions, clientId, claimRedirectUri, state);

            UmaGatherContext context = new UmaGatherContext(httpRequest, session, sessionService, permissionService, pctService);

            int step = sessionService.getStep(session);
            int stepsCount = external.getStepsCount(script, context);

            if (step < stepsCount) {
                String page = external.getPageForStep(script, step, context);

                context.persist();

                String fullUri = StringUtils.removeEnd(appConfiguration.getIssuer(), "/") + page;
                fullUri = StringUtils.removeEnd(fullUri, ".xhtml");
                log.trace("Redirecting to page: '{}', fullUri: {}", page, fullUri);
                return Response.status(FOUND).location(new URI(fullUri)).build();
            } else {
                log.error("Step '{}' is more or equal to stepCount: '{}'", stepsCount);
            }
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        log.error("Failed to handle call to UMA Claims Gathering Endpoint.");
        throw new UmaWebException(Response.Status.INTERNAL_SERVER_ERROR, errorResponseFactory, UmaErrorResponseType.SERVER_ERROR);
    }

    private static String getScriptNames(List<UmaPermission> permissions) {
        return permissions.get(0).getAttributes().get(UmaConstants.GATHERING_ID);
    }

    @GET
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response getGatherClaims(
            @QueryParam("client_id")
            String clientId,
            @QueryParam("ticket")
            String ticket,
            @QueryParam("claims_redirect_uri")
            String claimRedirectUri,
            @QueryParam("state")
            String state,
            @QueryParam("reset")
            Boolean reset,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {
        return gatherClaims(clientId, ticket, claimRedirectUri, state, reset, httpRequest, httpResponse);
    }

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response postGatherClaims(
            @FormParam("client_id")
            String clientId,
            @FormParam("ticket")
            String ticket,
            @FormParam("claims_redirect_uri")
            String claimRedirectUri,
            @FormParam("state")
            String state,
            @FormParam("reset")
            Boolean reset,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {
        return gatherClaims(clientId, ticket, claimRedirectUri, state, reset, httpRequest, httpResponse);
    }
}
