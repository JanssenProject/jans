/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.session.ws.rs;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.session.EndSessionParamsValidator;
import org.xdi.oxauth.model.session.EndSessionResponseParam;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.service.external.ExternalApplicationSessionService;
import org.xdi.oxauth.util.RedirectUri;
import org.xdi.oxauth.util.RedirectUtil;
import org.xdi.util.Pair;
import org.xdi.util.StringHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version October 1, 2015
 */
@Name("endSessionRestWebService")
public class EndSessionRestWebServiceImpl implements EndSessionRestWebService {

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private RedirectionUriService redirectionUriService;
    @In
    private AuthorizationGrantList authorizationGrantList;
    @In
    private ExternalApplicationSessionService externalApplicationSessionService;
    @In
    private SessionIdService sessionIdService;
    @In
    private ClientService clientService;
    @In(required = false)
    private Identity identity;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sessionId,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {

        log.debug("Attempting to end session, idTokenHint: {0}, postLogoutRedirectUri: {1}, sessionId: {2}, Is Secure = {3}",
                idTokenHint, postLogoutRedirectUri, sessionId, sec.isSecure());

        EndSessionParamsValidator.validateParams(idTokenHint, errorResponseFactory);

        final Pair<SessionId, AuthorizationGrant> pair = endSession(idTokenHint, sessionId, httpRequest, httpResponse, sec);

        if (!Strings.isNullOrEmpty(postLogoutRedirectUri)) {

            // Validate redirectUri
            String redirectUri = redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);

            if (StringUtils.isNotBlank(redirectUri)) {
                RedirectUri redirectUriResponse = new RedirectUri(redirectUri);
                if (StringUtils.isNotBlank(state)) {
                    redirectUriResponse.addResponseParameter(EndSessionResponseParam.STATE, state);
                }

                return RedirectUtil.getRedirectResponseBuilder(redirectUriResponse, httpRequest).build();
            } else {
                errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_REQUEST);
            }
        }
        return Response.ok().build();
    }

    private Pair<SessionId, AuthorizationGrant> endSession(String idTokenHint, String sessionId,
                                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {

        EndSessionParamsValidator.validateParams(idTokenHint, errorResponseFactory);

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            log.info("Failed to find out authorization grant for id_token_hing '{0}'", idTokenHint);
            errorResponseFactory.throwUnauthorizedException(EndSessionErrorResponseType.INVALID_GRANT);
        }

        boolean isExternalLogoutPresent = false;
        boolean externalLogoutResult = false;
        SessionId ldapSessionId = removeSessionId(sessionId, httpRequest, httpResponse);

        isExternalLogoutPresent = externalApplicationSessionService.isEnabled();
        if (isExternalLogoutPresent) {
            externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, authorizationGrant);
            log.info("End session result for '{0}': '{1}'", authorizationGrant.getUser().getUserId(), "logout", externalLogoutResult);
        }

        boolean isGrantAndExternalLogoutSuccessful = isExternalLogoutPresent && externalLogoutResult;
        if (isExternalLogoutPresent && !isGrantAndExternalLogoutSuccessful) {
            errorResponseFactory.throwUnauthorizedException(EndSessionErrorResponseType.INVALID_GRANT);
        }

        authorizationGrant.revokeAllTokens();
        if (identity != null) {
            identity.logout();
        }

        return new Pair<SessionId, AuthorizationGrant>(ldapSessionId, authorizationGrant);
    }

    @Override
    public Response requestEndSessionPage(
            @ApiParam(value = "Previously issued ID Token (id_token) passed to the logout endpoint as a hint about the End-User's current authenticated session with the Client. This is used as an indication of the identity of the End-User that the RP is requesting be logged out by the OP. The OP need not be listed as an audience of the ID Token when it is used as an id_token_hint value.", required = true)
            String idTokenHint,
            String postLogoutRedirectUri,
            @ApiParam(value = "Session ID", required = false)
            String sessionId,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext sec) {

        log.debug("Attempting to end session, idTokenHint: {0}, sessionId: {1}, Is Secure = {2}",
                idTokenHint, sessionId, sec.isSecure());


        Pair<SessionId, AuthorizationGrant> pair = endSession(idTokenHint, sessionId, httpRequest, httpResponse, sec);

        // Validate redirectUri
        String redirectUri = redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);

        final Set<String> logoutUris = getRpLogoutUris(pair.getFirst());
        final String html = constructPage(logoutUris, redirectUri);
        log.debug("Constructed http logout page: " + html);
        return Response.ok().type(MediaType.TEXT_HTML_TYPE).entity(html).build();
    }

    private Set<String> getRpLogoutUris(SessionId sessionId) {
        final Set<String> result = Sets.newHashSet();
        final Set<Client> clientsByDns = clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true);
        for (Client client : clientsByDns) {
            String logoutUri = client.getLogoutUri();

            if (Strings.isNullOrEmpty(logoutUri)) {
                continue; // skip client if logout_uri is blank
            }

            if (client.getLogoutSessionRequired() != null && client.getLogoutSessionRequired()) {
                if (logoutUri.contains("?")) {
                    logoutUri = logoutUri + "&sid=" + sessionId.getId();
                } else {
                    logoutUri = logoutUri + "?sid=" + sessionId.getId();
                }
            }
            result.add(logoutUri);
        }
        return result;
    }

    private SessionId removeSessionId(String sessionId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        SessionId ldapSessionId = null;

        try {
            String id = sessionId;
            if (StringHelper.isEmpty(id)) {
                id = sessionIdService.getSessionIdFromCookie(httpRequest);
            }

            if (StringHelper.isNotEmpty(id)) {
                ldapSessionId = sessionIdService.getSessionId(id);
                if (ldapSessionId != null) {
                    boolean result = sessionIdService.remove(ldapSessionId);
                    if (!result) {
                        log.error("Failed to remove session_id '{0}' from LDAP", id);
                    }
                } else {
                    log.error("Failed to load session from LDAP by session_id: '{0}'", id);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            sessionIdService.removeSessionIdCookie(httpResponse);
        }
        return ldapSessionId;
    }

    private String constructPage(Set<String> logoutUris, String postLogoutUrl) {
        String iframes = "";
        for (String logoutUri : logoutUris) {
            iframes = iframes + String.format("<iframe height=\"0\" width=\"0\" src=\"%s\"></iframe>", logoutUri);
        }

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>";

        if (!Strings.isNullOrEmpty(postLogoutUrl)) {
            html += "<meta http-equiv=\"refresh\" content=\"5; url=" + postLogoutUrl + "\">";
        }

        html += "<title>Gluu Generated logout page</title>" +
                "</head>" +
                "<body>" +
                "Logout requests sent.<br/>" +
                iframes +
                "</body>" +
                "</html>";
        return html;
    }
}