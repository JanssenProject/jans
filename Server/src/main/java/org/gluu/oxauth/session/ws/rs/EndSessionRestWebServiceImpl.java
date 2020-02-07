/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.session.ws.rs;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.model.security.Identity;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.session.EndSessionErrorResponseType;
import org.gluu.oxauth.model.util.URLPatternList;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.ClientService;
import org.gluu.oxauth.service.GrantService;
import org.gluu.oxauth.service.RedirectionUriService;
import org.gluu.oxauth.service.SessionIdService;
import org.gluu.oxauth.service.external.ExternalApplicationSessionService;
import org.gluu.oxauth.service.external.ExternalEndSessionService;
import org.gluu.oxauth.service.external.context.EndSessionContext;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.Pair;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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
    private ExternalEndSessionService externalEndSessionService;

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
        try {
            log.debug("Attempting to end session, idTokenHint: {}, postLogoutRedirectUri: {}, sessionId: {}, Is Secure = {}",
                    idTokenHint, postLogoutRedirectUri, sessionId, sec.isSecure());

            validateIdTokenHint(idTokenHint, postLogoutRedirectUri);
            validateSessionIdRequestParameter(sessionId, postLogoutRedirectUri);

            final Pair<SessionId, AuthorizationGrant> pair = endSession(idTokenHint, sessionId, httpRequest, httpResponse, postLogoutRedirectUri);

            auditLogging(httpRequest, pair);

            if (pair.getFirst() == null && pair.getSecond() == null) {
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, ""));
            }

            postLogoutRedirectUri = validatePostLogoutRedirectUri(postLogoutRedirectUri, pair);

            boolean isBackchannel = false; // TODO
            if (isBackchannel) {
                return backChannel(postLogoutRedirectUri, state, pair, httpRequest);
            }

            return httpBased(postLogoutRedirectUri, state, pair, httpRequest);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null)
                return e.getResponse();

            throw e;
        }
    }

    private Response backChannel(String postLogoutRedirectUri, String state, Pair<SessionId, AuthorizationGrant> pair, HttpServletRequest httpRequest) {
        final Set<String> backchannelLogoutUris = collectLogoutUris(pair, false);

        final String logoutToken = createLogoutToken();
        final ExecutorService executorService = EndSessionUtils.getExecutorService(backchannelLogoutUris.size());
        for (String backchannelLogoutUri : backchannelLogoutUris) {

            if (backchannelLogoutUri.contains("?")) {
                backchannelLogoutUri = backchannelLogoutUri + "&logout_token=" + logoutToken;
            } else {
                backchannelLogoutUri = backchannelLogoutUri + "?logout_token=" + logoutToken;
            }

            callBackchannelUri(backchannelLogoutUri, executorService);
        }

        if (StringUtils.isBlank(postLogoutRedirectUri)) {
            return Response.ok().build();
        }
        try {
            return Response.status(Response.Status.FOUND).location(new URI(postLogoutRedirectUri)).build();
        } catch (URISyntaxException e) {
            final String message = "Failed to create URI for " + postLogoutRedirectUri + " postlogout_redirect_uri.";
            log.error(message);
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_REQUEST, message)).build();
        }
    }

    private String createLogoutToken() {
        return ""; // todo
    }

    private void callBackchannelUri(final String backchannelLogoutUri, ExecutorService executorService) {
        executorService.execute(() -> {
            // todo
        });
    }

    private Response createErrorResponse(String postLogoutRedirectUri, EndSessionErrorResponseType error, String reason) {
        log.debug(reason);
        try {
            if (allowPostLogoutRedirect(postLogoutRedirectUri)) {
                // Commented out to avoid sending an error:                
                // String separator = postLogoutRedirectUri.contains("?") ? "&" : "?";
                // postLogoutRedirectUri = postLogoutRedirectUri + separator + errorResponseFactory.getErrorAsQueryString(error, "", reason);
                return Response.status(Response.Status.FOUND).location(new URI(postLogoutRedirectUri)).build();
            }
        } catch (URISyntaxException e) {
            log.error("Can't perform redirect", e);
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.errorAsJson(error, reason)).build();
    }

    /**
     * Allow post logout redirect without validation only if:
     * allowPostLogoutRedirectWithoutValidation = true and post_logout_redirect_uri is white listed
     */
    private boolean allowPostLogoutRedirect(String postLogoutRedirectUri) {
        if (StringUtils.isBlank(postLogoutRedirectUri)) {
            return false;
        }

        final Boolean allowPostLogoutRedirectWithoutValidation = appConfiguration.getAllowPostLogoutRedirectWithoutValidation();
        return allowPostLogoutRedirectWithoutValidation != null &&
                allowPostLogoutRedirectWithoutValidation &&
                new URLPatternList(appConfiguration.getClientWhiteList()).isUrlListed(postLogoutRedirectUri);
    }

    private void validateSessionIdRequestParameter(String sessionId, String postLogoutRedirectUri) {
        // session_id is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(sessionId)) {
            SessionId sessionIdObject = sessionIdService.getSessionId(sessionId);
            if (sessionIdObject == null) {
                final String reason = "session_id parameter in request is not valid. Logout is rejected. session_id parameter in request can be skipped or otherwise valid value must be provided.";
                log.error(reason);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }
        }
    }

    private void validateIdTokenHint(String idTokenHint, String postLogoutRedirectUri) {
        // id_token_hint is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(idTokenHint)) {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
            if (authorizationGrant == null) {
                Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
                if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
                    authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
                }

                if (authorizationGrant == null) {
                    final String reason = "id_token_hint is not valid. Logout is rejected. id_token_hint can be skipped or otherwise valid value must be provided.";
                    throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
                }
            }
        }
    }

    private String validatePostLogoutRedirectUri(String postLogoutRedirectUri, Pair<SessionId, AuthorizationGrant> pair) {
        try {
            if (appConfiguration.getAllowPostLogoutRedirectWithoutValidation()) {
                return postLogoutRedirectUri;
            }
            if (pair.getSecond() == null) {
                return redirectionUriService.validatePostLogoutRedirectUri(pair.getFirst(), postLogoutRedirectUri);
            } else {
                return redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);
            }
        } catch (WebApplicationException e) {
            if (pair.getFirst() != null) { // session_id was found and removed
                String reason = "Session was removed successfully but post_logout_redirect_uri validation fails since AS failed to validate it against clients associated with session (which was just removed).";
                log.error(reason, e);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, reason));
            } else {
                throw e;
            }
        }
    }

    private Response httpBased(String postLogoutRedirectUri, String state, Pair<SessionId, AuthorizationGrant> pair, HttpServletRequest httpRequest) {
        final Set<String> frontchannelLogoutUris = collectLogoutUris(pair, true);

        try {
            final EndSessionContext context = new EndSessionContext(httpRequest, frontchannelLogoutUris, postLogoutRedirectUri, pair.getFirst());
            final String htmlFromScript = externalEndSessionService.getFrontchannelHtml(context);
            if (StringUtils.isNotBlank(htmlFromScript)) {
                log.debug("HTML from `getFrontchannelHtml` external script: " + htmlFromScript);
                return okResponse(htmlFromScript);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // default handling
        final String html = EndSessionUtils.createFronthannelHtml(frontchannelLogoutUris, postLogoutRedirectUri, state);
        log.debug("Constructed html logout page: " + html);
        return okResponse(html);
    }

    private Response okResponse(String html) {
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                type(MediaType.TEXT_HTML_TYPE).entity(html).
                build();
    }

    private Pair<SessionId, AuthorizationGrant> endSession(String idTokenHint, String sessionId, HttpServletRequest httpRequest, HttpServletResponse httpResponse, String postLogoutRedirectUri) {
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
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
        }

        boolean isExternalLogoutPresent;
        boolean externalLogoutResult = false;

        isExternalLogoutPresent = externalApplicationSessionService.isEnabled();
        if (isExternalLogoutPresent) {
            String userName = ldapSessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
            externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, ldapSessionId);
            log.info("End session result for '{}': '{}'", userName, "logout", externalLogoutResult);
        }

        boolean isGrantAndExternalLogoutSuccessful = isExternalLogoutPresent && externalLogoutResult;
        if (isExternalLogoutPresent && !isGrantAndExternalLogoutSuccessful) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, EndSessionErrorResponseType.INVALID_GRANT, "External logout is present but executed external logout script returned failed result.");
        }

        grantService.removeAllTokensBySession(ldapSessionId.getDn());

        if (identity != null) {
            identity.logout();
        }

        return new Pair<>(ldapSessionId, authorizationGrant);
    }

    private Set<String> collectLogoutUris(Pair<SessionId, AuthorizationGrant> pair, boolean isFrontchannel) {
        final Set<String> result = Sets.newHashSet();

        SessionId sessionId = pair.getFirst();
        AuthorizationGrant authorizationGrant = pair.getSecond();
        if (sessionId == null) {
            log.error("session_id is not passed to endpoint (as cookie or manually). Therefore unable to match clients for session_id.");
            return result;
        }

        final Set<Client> clientsByDns = sessionId.getPermissionGrantedMap() != null ?
                clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true) :
                Sets.newHashSet();
        if (authorizationGrant != null) {
            clientsByDns.add(authorizationGrant.getClient());
        }

        for (Client client : clientsByDns) {
            List<String> logoutUris = isFrontchannel ? Lists.newArrayList(client.getFrontChannelLogoutUri()) : client.getAttributes().getBackchannelLogoutUri();

            for (String logoutUri : logoutUris) {
                if (Util.isNullOrEmpty(logoutUri)) {
                    continue; // skip client if logout_uri is blank
                }

                boolean appendSid = isFrontchannel ? client.getFrontChannelLogoutSessionRequired() : client.getAttributes().getBackchannelLogoutSessionRequired();

                if (appendSid) {
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

    private void removeConsentSessionId(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String id = sessionIdService.getConsentSessionIdFromCookie(httpRequest);

            if (StringHelper.isNotEmpty(id)) {
                SessionId ldapSessionId = sessionIdService.getSessionId(id);
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