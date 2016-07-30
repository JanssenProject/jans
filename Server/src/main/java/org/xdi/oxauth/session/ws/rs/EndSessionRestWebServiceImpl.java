/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.session.ws.rs;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.session.EndSessionParamsValidator;
import org.xdi.oxauth.model.session.EndSessionResponseParam;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.service.external.ExternalApplicationSessionService;
import org.xdi.oxauth.util.RedirectUri;
import org.xdi.oxauth.util.RedirectUtil;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.Pair;
import org.xdi.util.StringHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version December 15, 2015
 */
@Name("endSessionRestWebService")
public class EndSessionRestWebServiceImpl implements EndSessionRestWebService {

    private static final boolean HTTP_BASED = true; // for now always true but maybe we will want to make is configurable?

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
    private SessionStateService sessionStateService;
    @In
    private ClientService clientService;
    @In(required = false)
    private Identity identity;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sessionState,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {

        log.debug("Attempting to end session, idTokenHint: {0}, postLogoutRedirectUri: {1}, sessionState: {2}, Is Secure = {3}",
                idTokenHint, postLogoutRedirectUri, sessionState, sec.isSecure());

        EndSessionParamsValidator.validateParams(idTokenHint, errorResponseFactory);

        final Pair<SessionState, AuthorizationGrant> pair = endSession(idTokenHint, sessionState, httpRequest, httpResponse, sec);

        if (HTTP_BASED) {
            return httpBased(postLogoutRedirectUri, state, pair);
        } else {
            return simpleLogout(postLogoutRedirectUri, state, httpRequest, pair);
        }
    }


    public Response httpBased(String postLogoutRedirectUri, String state, Pair<SessionState, AuthorizationGrant> pair) {

        // Validate redirectUri
        String redirectUri = redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);

        final Set<String> logoutUris = getRpLogoutUris(pair);
        final String html = constructPage(logoutUris, redirectUri, state);
        log.debug("Constructed http logout page: " + html);
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                type(MediaType.TEXT_HTML_TYPE).entity(html).
                build();
    }

    private Response simpleLogout(String postLogoutRedirectUri, String state, HttpServletRequest httpRequest, Pair<SessionState, AuthorizationGrant> pair) {
        if (!Util.isNullOrEmpty(postLogoutRedirectUri)) {

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
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                build();
    }

    private Pair<SessionState, AuthorizationGrant> endSession(String idTokenHint, String sessionState,
                                                              HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {

        EndSessionParamsValidator.validateParams(idTokenHint, errorResponseFactory);

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            log.info("Failed to find out authorization grant for id_token_hint '{0}'", idTokenHint);
            errorResponseFactory.throwUnauthorizedException(EndSessionErrorResponseType.INVALID_GRANT);
        }

        boolean isExternalLogoutPresent;
        boolean externalLogoutResult = false;
        SessionState ldapSessionState = removeSessionState(sessionState, httpRequest, httpResponse);

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
        GrantService.instance().removeAllTokensBySession(ldapSessionState.getDn());

        if (identity != null) {
            identity.logout();
        }

        return new Pair<SessionState, AuthorizationGrant>(ldapSessionState, authorizationGrant);
    }

    private Set<String> getRpLogoutUris(Pair<SessionState, AuthorizationGrant> pair) {
        final Set<String> result = Sets.newHashSet();

        SessionState sessionState = pair.getFirst();
        if (sessionState == null) {
            log.error("session_state is not passed to endpoint (as cookie or manually). Therefore unable to match clients for session_state." +
                    "Http based html will contain no iframes.");
            return result;
        }

        final Set<Client> clientsByDns = sessionState.getPermissionGrantedMap() != null ?
                clientService.getClient(sessionState.getPermissionGrantedMap().getClientIds(true), true) :
                Sets.<Client>newHashSet();
        clientsByDns.add(pair.getSecond().getClient());

        for (Client client : clientsByDns) {
            String[] logoutUris = client.getLogoutUri();

            if (logoutUris == null) {
                continue;
            }

            for (String logoutUri : logoutUris) {
                if (Util.isNullOrEmpty(logoutUri)) {
                    continue; // skip client if logout_uri is blank
                }

                if (client.getLogoutSessionRequired() != null && client.getLogoutSessionRequired()) {
                    if (logoutUri.contains("?")) {
                        logoutUri = logoutUri + "&sid=" + sessionState.getId();
                    } else {
                        logoutUri = logoutUri + "?sid=" + sessionState.getId();
                    }
                }
                result.add(logoutUri);
            }
        }
        return result;
    }

    private SessionState removeSessionState(String sessionState, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        SessionState ldapSessionState = null;

        try {
            String id = sessionState;
            if (StringHelper.isEmpty(id)) {
                id = sessionStateService.getSessionStateFromCookie(httpRequest);
            }

            if (StringHelper.isNotEmpty(id)) {
                ldapSessionState = sessionStateService.getSessionState(id);
                if (ldapSessionState != null) {
                    boolean result = sessionStateService.remove(ldapSessionState);
                    if (!result) {
                        log.error("Failed to remove session_state '{0}' from LDAP", id);
                    }
                } else {
                    log.error("Failed to load session from LDAP by session_state: '{0}'", id);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            sessionStateService.removeSessionStateCookie(httpResponse);
        }
        return ldapSessionState;
    }

    private String constructPage(Set<String> logoutUris, String postLogoutUrl, String state) {
        String iframes = "";
        for (String logoutUri : logoutUris) {
            iframes = iframes + String.format("<iframe height=\"0\" width=\"0\" src=\"%s\"></iframe>", logoutUri);
        }

        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>";

        if (!Util.isNullOrEmpty(postLogoutUrl)) {

            if (!Util.isNullOrEmpty(state)) {
                if (postLogoutUrl.contains("?")) {
                    postLogoutUrl += "&state=" + state;
                } else {
                    postLogoutUrl += "?state=" + state;
                }
            }

            html += "<script>" +
                    "window.onload=function() {" +
                    "window.location='" + postLogoutUrl + "'" +
                    "}" +
                    "</script>";
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