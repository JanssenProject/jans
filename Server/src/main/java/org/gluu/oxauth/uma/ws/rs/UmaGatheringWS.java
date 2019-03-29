/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FOUND;
import static org.gluu.oxauth.model.uma.UmaErrorResponseType.INVALID_CLAIMS_GATHERING_SCRIPT_NAME;
import static org.gluu.oxauth.model.uma.UmaErrorResponseType.INVALID_SESSION;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.persistence.UmaPermission;
import org.gluu.oxauth.service.UserService;
import org.gluu.oxauth.service.external.ExternalUmaClaimsGatheringService;
import org.gluu.oxauth.uma.authorization.UmaGatherContext;
import org.gluu.oxauth.uma.authorization.UmaWebException;
import org.gluu.oxauth.uma.service.UmaPctService;
import org.gluu.oxauth.uma.service.UmaPermissionService;
import org.gluu.oxauth.uma.service.UmaSessionService;
import org.gluu.oxauth.uma.service.UmaValidationService;
import org.slf4j.Logger;

/**
 * Claims-Gathering Endpoint.
 *
 * @author yuriyz
 * @version August 9, 2017
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
    @Inject
    private UserService userService;

    public Response gatherClaims(String clientId, String ticket, String claimRedirectUri, String state, Boolean reset,
                                 Boolean authenticationRedirect, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            log.trace("gatherClaims client_id: {}, ticket: {}, claims_redirect_uri: {}, state: {}, authenticationRedirect: {}, queryString: {}",
                    clientId, ticket, claimRedirectUri, state, authenticationRedirect, httpRequest.getQueryString());

            SessionId session = sessionService.getSession(httpRequest, httpResponse);

            if (authenticationRedirect != null && authenticationRedirect) { // restore parameters from session
                log.debug("Authentication redirect, restoring parameters from session ...");
                if (session == null) {
                    log.error("Session is null however authentication=true. Wrong workflow! Please correct custom Glaims-Gathering Script.");
                    throw new UmaWebException(BAD_REQUEST, errorResponseFactory, INVALID_SESSION);
                }
                clientId = sessionService.getClientId(session);
                ticket = sessionService.getTicket(session);
                claimRedirectUri = sessionService.getClaimsRedirectUri(session);
                state = sessionService.getState(session);
                log.debug("Restored parameters from session, clientId: {}, ticket: {}, claims_redirect_uri: {}, state: {}",
                        clientId, ticket, claimRedirectUri, state);
            }

            validationService.validateClientAndClaimsRedirectUri(clientId, claimRedirectUri, state);
            List<UmaPermission> permissions = validationService.validateTicketWithRedirect(ticket, claimRedirectUri, state);
            String[] scriptNames = validationService.validatesGatheringScriptNames(getScriptNames(permissions), claimRedirectUri, state);

            CustomScriptConfiguration script = external.determineScript(scriptNames);
            if (script == null) {
                log.error("Failed to determine claims-gathering script for names: " + Arrays.toString(scriptNames));
                throw new UmaWebException(claimRedirectUri, errorResponseFactory, INVALID_CLAIMS_GATHERING_SCRIPT_NAME, state);
            }

            sessionService.configure(session, script.getName(), reset, permissions, clientId, claimRedirectUri, state);

            UmaGatherContext context = new UmaGatherContext(script.getConfigurationAttributes(), httpRequest, session, sessionService, permissionService,
                    pctService, new HashMap<String, String>(), userService, null, appConfiguration);

            int step = sessionService.getStep(session);
            int stepsCount = external.getStepsCount(script, context);

            if (step < stepsCount) {
                String page = external.getPageForStep(script, step, context);

                context.persist();

                String baseEndpoint = StringUtils.removeEnd(appConfiguration.getBaseEndpoint(), "/");
                baseEndpoint = StringUtils.removeEnd(baseEndpoint, "restv1");
                baseEndpoint = StringUtils.removeEnd(baseEndpoint, "/");

                String fullUri = baseEndpoint + page;
                fullUri = StringUtils.removeEnd(fullUri, ".xhtml") + ".htm";
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
            @QueryParam("authentication")
                    Boolean authenticationRedirect,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {
        return gatherClaims(clientId, ticket, claimRedirectUri, state, reset, authenticationRedirect, httpRequest, httpResponse);
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
            @FormParam("authentication")
                    Boolean authenticationRedirect,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse) {
        return gatherClaims(clientId, ticket, claimRedirectUri, state, reset, authenticationRedirect, httpRequest, httpResponse);
    }
}
