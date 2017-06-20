package org.xdi.oxauth.uma.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.uma.authorization.UmaWebException;
import org.xdi.oxauth.uma.service.ExternalUmaClaimsGatheringService;
import org.xdi.oxauth.uma.service.UmaValidationService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
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
    private UmaValidationService umaValidationService;

    @Inject
    private ExternalUmaClaimsGatheringService external;

    @Inject
    private SessionStateService sessionStateService;

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
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {
        try {
            log.trace("gatherClaims client_id: {}, ticket: {}, claims_redirect_uri: {}, state: {}, queryString: {}"
                    , clientId, ticket, claimRedirectUri, state, httpRequest.getQueryString());

            Client client = umaValidationService.validateClientAndClaimsRedirectUri(clientId, claimRedirectUri, state);
            List<UmaPermission> permissions = umaValidationService.validateTicketWithRedirect(ticket, claimRedirectUri, state);
            String[] scriptNames = umaValidationService.validatesGatheringScriptNames(getScriptNames(permissions), claimRedirectUri, state);

            CustomScriptConfiguration script = external.determineScript(scriptNames);
            if (script == null) {
                log.error("Failed to determine claims-gathering script for names: " + Arrays.toString(scriptNames));
                throw new UmaWebException(claimRedirectUri, errorResponseFactory, INVALID_CLAIMS_GATHERING_SCRIPT_NAME, state);
            }

            SessionState session = getSession(httpRequest, httpResponse);
            configureSession(session);

            session.getSessionAttributes().get("step");

            //external.getPageForStep()
//            external.getNextStep()
            // todo

            String redirectUri = claimRedirectUri;
            return Response.status(Response.Status.FOUND)
                    .location(URI.create(redirectUri))
                    .build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        log.error("Failed to handle to UMA Claims Gathering Endpoint.");
        throw new UmaWebException(Response.Status.INTERNAL_SERVER_ERROR, errorResponseFactory, UmaErrorResponseType.SERVER_ERROR);
    }

    private void configureSession(SessionState session) {
        // todo
    }

    private SessionState getSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String cookieSessionId = sessionStateService.getSessionStateFromCookie();
        log.trace("Cookie - uma_session_state: " + cookieSessionId);

        if (StringUtils.isNotBlank(cookieSessionId)) {
            SessionState ldapSessionState = sessionStateService.getSessionState(cookieSessionId);
            if (ldapSessionState != null) {
                log.trace("Loaded uma_session_state from cookie, session: " + ldapSessionState);
                return ldapSessionState;
            } else {
                log.error("Failed to load uma_session_state from cookie: " + cookieSessionId);
            }
        } else {
            log.error("uma_session_state cookie is not set.");
        }

        log.trace("Generating new uma_session_state ...");
        SessionState session = sessionStateService.generateAuthenticatedSessionState("no");

        sessionStateService.createSessionStateCookie(session.getId(), httpResponse, true);
        log.trace("uma_session_state cookie created.");
        return session;
    }

    private String getScriptNames(List<UmaPermission> permissions) {
        return permissions.get(0).getAttributes().get(UmaConstants.GATHERING_ID);
    }
}
