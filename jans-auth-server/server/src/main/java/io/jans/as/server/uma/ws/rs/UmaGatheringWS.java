/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.external.ExternalUmaClaimsGatheringService;
import io.jans.as.server.uma.authorization.UmaGatherContext;
import io.jans.as.server.uma.authorization.UmaWebException;
import io.jans.as.server.uma.service.UmaPctService;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaSessionService;
import io.jans.as.server.uma.service.UmaValidationService;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_CLAIMS_GATHERING_SCRIPT_NAME;
import static io.jans.as.model.uma.UmaErrorResponseType.INVALID_SESSION;
import static io.jans.as.model.util.Util.escapeLog;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FOUND;

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

    private static String getScriptNames(List<UmaPermission> permissions) {
        return permissions.get(0).getAttributes().get(UmaConstants.GATHERING_ID);
    }

    public Response gatherClaims(String clientId, String ticket, String claimRedirectUri, String state,
                                 Boolean authenticationRedirect, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("gatherClaims client_id: {}, ticket: {}, claims_redirect_uri: {}, state: {}, authenticationRedirect: {}, queryString: {}",
                        escapeLog(clientId), escapeLog(ticket), escapeLog(claimRedirectUri), escapeLog(state), escapeLog(authenticationRedirect), httpRequest.getQueryString());
            }

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

            SessionId session = sessionService.getSession(httpRequest, httpResponse);

            if (authenticationRedirect != null && authenticationRedirect) { // restore parameters from session
                log.debug("Authentication redirect, restoring parameters from session ...");
                if (session == null) {
                    log.error("Session is null however authentication=true. Wrong workflow! Please correct custom Glaims-Gathering Script.");
                    throw errorResponseFactory.createWebApplicationException(BAD_REQUEST, INVALID_SESSION, "Session is null however authentication=true. Wrong workflow! Please correct custom Glaims-Gathering Script.");
                }
                clientId = sessionService.getClientId(session);
                ticket = sessionService.getTicket(session);
                claimRedirectUri = sessionService.getClaimsRedirectUri(session);
                state = sessionService.getState(session);

                if (log.isDebugEnabled()) {
                    log.debug("Restored parameters from session, clientId: {}, ticket: {}, claims_redirect_uri: {}, state: {}",
                            escapeLog(clientId), escapeLog(ticket), escapeLog(claimRedirectUri), escapeLog(state));
                }
            }

            validationService.validateClientAndClaimsRedirectUri(clientId, claimRedirectUri, state);
            List<UmaPermission> permissions = validationService.validateTicketWithRedirect(ticket, claimRedirectUri, state);
            String[] scriptNames = validationService.validatesGatheringScriptNames(getScriptNames(permissions), claimRedirectUri, state);

            CustomScriptConfiguration script = external.determineScript(scriptNames);
            if (script == null) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to determine claims-gathering script for names: {}", Arrays.toString(scriptNames));
                }
                throw new UmaWebException(claimRedirectUri, errorResponseFactory, INVALID_CLAIMS_GATHERING_SCRIPT_NAME, state);
            }

            sessionService.configure(session, script.getName(), permissions, clientId, claimRedirectUri, state);

            UmaGatherContext context = new UmaGatherContext(script.getConfigurationAttributes(), httpRequest, session, sessionService, permissionService,
                    pctService, new HashMap<>(), appConfiguration);

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
                log.error("Step '{}' is more or equal to stepCount: '{}'", step, stepsCount);
            }
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }
        }

        log.error("Failed to handle call to UMA Claims Gathering Endpoint.");
        throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Failed to handle call to UMA Claims Gathering Endpoint.");
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
        return gatherClaims(clientId, ticket, claimRedirectUri, state, authenticationRedirect, httpRequest, httpResponse);
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
        return gatherClaims(clientId, ticket, claimRedirectUri, state, authenticationRedirect, httpRequest, httpResponse);
    }
}
