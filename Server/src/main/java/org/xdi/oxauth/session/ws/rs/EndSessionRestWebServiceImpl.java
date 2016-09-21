/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.session.ws.rs;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.service.external.ExternalApplicationSessionService;
import org.xdi.oxauth.util.ServerUtil;
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

        final Optional<SessionState> endedSession = endSession(sessionState, httpRequest, httpResponse);
        if (!endedSession.isPresent()) {
            log.error("Unable to identify session_state (session state does not exist on OP or otherwise no parameters for correct identification," +
                    " e.g. cookie or session_state query parameter.). Reject request and send back invalid_request error.");
            errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_REQUEST);
        }

        return httpBased(postLogoutRedirectUri, state, endedSession);
    }


    public Response httpBased(String postLogoutRedirectUri, String state, Optional<SessionState> sessionState) {

        String validatedPostLogoutRedirectUri = redirectionUriService.validatePostLogoutRedirectUri(sessionState, postLogoutRedirectUri);

        final Set<String> logoutUris = getRpLogoutUris(sessionState);
        final String html = constructPage(logoutUris, validatedPostLogoutRedirectUri, state);
        log.debug("Constructed http logout page: " + html);
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                type(MediaType.TEXT_HTML_TYPE).entity(html).
                build();
    }

    private Optional<SessionState> endSession(String sessionState, HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {

        boolean isExternalLogoutPresent;
        boolean externalLogoutResult = false;
        Optional<SessionState> ldapSessionState = removeSessionState(sessionState, httpRequest, httpResponse);

        isExternalLogoutPresent = externalApplicationSessionService.isEnabled();
        if (isExternalLogoutPresent) {
            externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, ldapSessionState.orNull());
            log.info("End session result for '{0}': '{1}'", sessionState, "logout", externalLogoutResult);
        }

        boolean isGrantAndExternalLogoutSuccessful = isExternalLogoutPresent && externalLogoutResult;
        if (isExternalLogoutPresent && !isGrantAndExternalLogoutSuccessful) {
            errorResponseFactory.throwUnauthorizedException(EndSessionErrorResponseType.INVALID_GRANT);
        }

        if (ldapSessionState.isPresent()) {
            GrantService.instance().removeAllTokensBySession(ldapSessionState.get().getDn());
        }

        if (identity != null) {
            identity.logout();
        }

        return ldapSessionState;
    }

    private Set<String> getRpLogoutUris(Optional<SessionState> sessionState) {
        final Set<String> result = Sets.newHashSet();

        if (!sessionState.isPresent()) {
            log.error("session_state is not passed to endpoint (as cookie or manually). Therefore unable to match clients for session_state." +
                    "Http based html will contain no iframes.");
            return result;
        }

        final Set<Client> clientsByDns = sessionState.get().getPermissionGrantedMap() != null ?
                clientService.getClient(sessionState.get().getPermissionGrantedMap().getClientIds(true), true) :
                Sets.<Client>newHashSet();

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
                        logoutUri = logoutUri + "&sid=" + sessionState.get().getId();
                    } else {
                        logoutUri = logoutUri + "?sid=" + sessionState.get().getId();
                    }
                }
                result.add(logoutUri);
            }
        }
        return result;
    }

    private Optional<SessionState> removeSessionState(String sessionState, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

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
        return Optional.fromNullable(ldapSessionState);
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