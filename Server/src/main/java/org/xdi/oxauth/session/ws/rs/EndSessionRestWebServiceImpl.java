/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.session.ws.rs;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.security.Identity;
import org.xdi.oxauth.audit.ApplicationAuditLogger;
import org.xdi.oxauth.model.audit.Action;
import org.xdi.oxauth.model.audit.OAuth2AuditLog;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.util.URLPatternList;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.GrantService;
import org.xdi.oxauth.service.RedirectionUriService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.service.external.ExternalApplicationSessionService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.Pair;
import org.xdi.util.StringHelper;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version December 8, 2018
 */
@Path("/")
public class EndSessionRestWebServiceImpl implements EndSessionRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private RedirectionUriService redirectionUriService;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private ExternalApplicationSessionService externalApplicationSessionService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private ClientService clientService;

    @Inject
    private GrantService grantService;

    @Inject
    private Identity identity;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sessionId,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {
        log.debug("Attempting to end session, idTokenHint: {}, postLogoutRedirectUri: {}, sessionId: {}, Is Secure = {}",
                idTokenHint, postLogoutRedirectUri, sessionId, sec.isSecure());

        validateIdTokenHint(idTokenHint);
        validateSessionIdRequestParameter(sessionId);

        final Pair<SessionId, AuthorizationGrant> pair = endSession(idTokenHint, sessionId, httpRequest, httpResponse);

        auditLogging(httpRequest, pair);

        //Perform redirect to RP if id_token is expired (see https://github.com/GluuFederation/oxAuth/issues/575)
        // yuriyz : we have to re-visit this since it doesn't look safe to redirect error to uri which is not validated.
        if (pair.getFirst() == null && pair.getSecond() == null && StringUtils.isNotBlank(postLogoutRedirectUri) && allowPostLogoutRedirect(postLogoutRedirectUri)) {
            try {
                String error = errorResponseFactory.getErrorAsJson(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION);
                return Response.temporaryRedirect(new URI(postLogoutRedirectUri)).entity(error).build();
            } catch (URISyntaxException e) {
                log.error("Can't perform redirect", e);
            }
        }

        return httpBased(postLogoutRedirectUri, state, pair);
    }

    /**
     * Allow post logout redirect without validation only if:
     * allowPostLogoutRedirectWithoutValidation = true and post_logout_redirect_uri is white listed
     */
    private boolean allowPostLogoutRedirect(String postLogoutRedirectUri) {
        final Boolean allowPostLogoutRedirectWithoutValidation = appConfiguration.getAllowPostLogoutRedirectWithoutValidation();
        return allowPostLogoutRedirectWithoutValidation != null &&
                allowPostLogoutRedirectWithoutValidation &&
                new URLPatternList(appConfiguration.getClientWhiteList()).isUrlListed(postLogoutRedirectUri);
    }

    private void validateSessionIdRequestParameter(String sessionId) {
        // session_id is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(sessionId)) {
            SessionId sessionIdObject = sessionIdService.getSessionId(sessionId);
            if (sessionIdObject == null) {
                final String reason = "session_id parameter in request is not valid. Logout is rejected. session_id parameter in request can be skipped or otherwise valid value must be provided.";
                log.error(reason);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                        errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason)).build());
            }
        }
    }

    private void validateIdTokenHint(String idTokenHint) {
        // id_token_hint is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(idTokenHint)) {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
            if (authorizationGrant == null) {
                final String reason = "id_token_hint is not valid. Logout is rejected. id_token_hint can be skipped or otherwise valid value must be provided.";
                log.error(reason);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                        errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason)).build());
            }
        }
    }

    private String validatePostLogoutRedirectUri(String postLogoutRedirectUri, Pair<SessionId, AuthorizationGrant> pair) {
        try {
            if (pair.getSecond() == null) {
                return redirectionUriService.validatePostLogoutRedirectUri(pair.getFirst(), postLogoutRedirectUri);
            } else {
                return redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);
            }
        } catch (WebApplicationException e) {
            if (pair.getFirst() != null) { // session_id was found and removed
                String reason = "Session was removed successfully but redirect to post_logout_redirect_uri fails since AS failed to validate it against clients associated with session (which was just removed).";
                log.error(reason, e);
                throw new WebApplicationException(Response.status(Response.Status.OK).entity(
                        errorResponseFactory.errorAsJson(EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, reason)).build());
            } else {
                throw e;
            }
        }
    }

    public Response httpBased(String postLogoutRedirectUri, String state, Pair<SessionId, AuthorizationGrant> pair) {
        final String redirectUri = validatePostLogoutRedirectUri(postLogoutRedirectUri, pair);
        final Set<String> frontchannelLogoutUris = getRpFrontchannelLogoutUris(pair);
        final String html = constructPage(frontchannelLogoutUris, redirectUri, state);
        log.debug("Constructed http logout page: " + html);
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                type(MediaType.TEXT_HTML_TYPE).entity(html).
                build();
    }

    private Pair<SessionId, AuthorizationGrant> endSession(String idTokenHint, String sessionId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
            if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
                authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
            }
        }

        // Clean up authorization session
        removeConsentSessionId(httpRequest, httpResponse);

        SessionId ldapSessionId = removeSessionId(sessionId, httpRequest, httpResponse);
        if (ldapSessionId == null) {
            final String reason = "Failed to identify session by session_id query parameter or by session_id cookie.";
            log.debug(reason);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(
                    errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason)).build());
        }

        boolean isExternalLogoutPresent;
        boolean externalLogoutResult = false;

        isExternalLogoutPresent = externalApplicationSessionService.isEnabled();
        if (isExternalLogoutPresent && (ldapSessionId != null)) {
            String userName = ldapSessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
            externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, ldapSessionId);
            log.info("End session result for '{}': '{}'", userName, "logout", externalLogoutResult);
        }

        boolean isGrantAndExternalLogoutSuccessful = isExternalLogoutPresent && externalLogoutResult;
        if (isExternalLogoutPresent && !isGrantAndExternalLogoutSuccessful) {
            errorResponseFactory.throwUnauthorizedException(EndSessionErrorResponseType.INVALID_GRANT);
        }

        if (ldapSessionId != null) {
            grantService.removeAllTokensBySession(ldapSessionId.getDn());
        }

        if (identity != null) {
            identity.logout();
        }

        return new Pair<SessionId, AuthorizationGrant>(ldapSessionId, authorizationGrant);
    }

    private Set<String> getRpFrontchannelLogoutUris(Pair<SessionId, AuthorizationGrant> pair) {
        final Set<String> result = Sets.newHashSet();

        SessionId sessionId = pair.getFirst();
        AuthorizationGrant authorizationGrant = pair.getSecond();
        if (sessionId == null) {
            log.error("session_id is not passed to endpoint (as cookie or manually). Therefore unable to match clients for session_id." +
                    "Http based html will contain no iframes.");
            return result;
        }

        final Set<Client> clientsByDns = sessionId.getPermissionGrantedMap() != null ?
                clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true) :
                Sets.<Client>newHashSet();
        if (authorizationGrant != null) {
            clientsByDns.add(authorizationGrant.getClient());
        }

        for (Client client : clientsByDns) {
            String[] logoutUris = client.getFrontChannelLogoutUri();

            if (logoutUris == null) {
                continue;
            }

            for (String logoutUri : logoutUris) {
                if (Util.isNullOrEmpty(logoutUri)) {
                    continue; // skip client if logout_uri is blank
                }

                if (client.getFrontChannelLogoutSessionRequired() != null && client.getFrontChannelLogoutSessionRequired()) {
                    if (logoutUri.contains("?")) {
                        logoutUri = logoutUri + "&sid=" + sessionId.getId();
                    } else {
                        logoutUri = logoutUri + "?sid=" + sessionId.getId();
                    }
                }
                result.add(logoutUri);
            }
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
                        log.error("Failed to remove session_id '{}'", id);
                    }
                } else {
                    log.error("Failed to load session by session_id: '{}'", id);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            sessionIdService.removeSessionIdCookie(httpResponse);
            sessionIdService.removeOPBrowserStateCookie(httpResponse);
        }
        return ldapSessionId;
    }

    private SessionId removeConsentSessionId(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SessionId ldapSessionId = null;

        try {
            String id = sessionIdService.getConsentSessionIdFromCookie(httpRequest);

            if (StringHelper.isNotEmpty(id)) {
                ldapSessionId = sessionIdService.getSessionId(id);
                if (ldapSessionId != null) {
                    boolean result = sessionIdService.remove(ldapSessionId);
                    if (!result) {
                        log.error("Failed to remove consent_session_id '{}'", id);
                    }
                } else {
                    log.error("Failed to load session by consent_session_id: '{}'", id);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            sessionIdService.removeConsentSessionIdCookie(httpResponse);
        }
        return ldapSessionId;
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

    private void auditLogging(HttpServletRequest request, Pair<SessionId, AuthorizationGrant> pair) {
        SessionId sessionId = pair.getFirst();
        AuthorizationGrant authorizationGrant = pair.getSecond();

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.SESSION_DESTROYED);
        oAuth2AuditLog.setSuccess(true);

        if (authorizationGrant != null) {
            oAuth2AuditLog.setClientId(authorizationGrant.getClientId());
            oAuth2AuditLog.setScope(StringUtils.join(authorizationGrant.getScopes(), " "));
            oAuth2AuditLog.setUsername(authorizationGrant.getUserId());
        } else if (sessionId != null) {
            oAuth2AuditLog.setClientId(sessionId.getPermissionGrantedMap().getClientIds(true).toString());
            oAuth2AuditLog.setScope(sessionId.getSessionAttributes().get(AuthorizeRequestParam.SCOPE));
            oAuth2AuditLog.setUsername(sessionId.getUserDn());
        }

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
    }
}