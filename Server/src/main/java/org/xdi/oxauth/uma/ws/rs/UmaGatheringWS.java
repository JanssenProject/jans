package org.xdi.oxauth.uma.ws.rs;

import org.gluu.jsf2.service.FacesService;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.SessionState;
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
import java.util.Arrays;
import java.util.List;

import static org.xdi.oxauth.model.uma.UmaErrorResponseType.INVALID_CLAIMS_GATHERING_SCRIPT_NAME;

/**
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
    private FacesService facesService;
    @Inject
    private UmaPermissionService permissionService;
    @Inject
    private UmaPctService pctService;

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response gatherClaims(
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

                log.trace("Redirecting to page: '{}'", page);
                facesService.redirect(page);
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
}
