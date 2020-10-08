/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.session.ws.rs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import io.jans.model.security.Identity;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorHandlingMethod;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.gluu.GluuErrorResponseType;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.session.EndSessionErrorResponseType;
import org.gluu.oxauth.model.session.EndSessionRequestParam;
import org.gluu.oxauth.model.token.JsonWebResponse;
import org.gluu.oxauth.model.util.URLPatternList;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxauth.service.*;
import org.gluu.oxauth.service.external.ExternalApplicationSessionService;
import org.gluu.oxauth.service.external.ExternalEndSessionService;
import org.gluu.oxauth.service.external.context.EndSessionContext;
import org.gluu.oxauth.util.ServerUtil;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
    private CookieService cookieService;

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

    @Inject
    private LogoutTokenFactory logoutTokenFactory;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sessionId,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {
        try {
            log.debug("Attempting to end session, idTokenHint: {}, postLogoutRedirectUri: {}, sessionId: {}, Is Secure = {}",
                    idTokenHint, postLogoutRedirectUri, sessionId, sec.isSecure());

            Jwt idToken = validateIdTokenHint(idTokenHint, postLogoutRedirectUri);
            validateSessionIdRequestParameter(sessionId, postLogoutRedirectUri);

            final Pair<SessionId, AuthorizationGrant> pair = getPair(idTokenHint, sessionId, httpRequest);
            if (pair.getFirst() == null) {
                final String reason = "Failed to identify session by session_id query parameter or by session_id cookie.";
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }

            postLogoutRedirectUri = validatePostLogoutRedirectUri(postLogoutRedirectUri, pair);
            validateSid(postLogoutRedirectUri, idToken, pair.getFirst());

            endSession(pair, httpRequest, httpResponse);
            auditLogging(httpRequest, pair);

            Set<Client> clients = getSsoClients(pair);
            Set<String> frontchannelUris = Sets.newHashSet();
            Map<String, Client> backchannelUris = Maps.newHashMap();

            for (Client client : clients) {
                boolean hasBackchannel = false;
                for (String logoutUri : client.getAttributes().getBackchannelLogoutUri()) {
                    if (Util.isNullOrEmpty(logoutUri)) {
                        continue; // skip if logout_uri is blank
                    }
                    backchannelUris.put(logoutUri, client);
                    hasBackchannel = true;
                }

                if (hasBackchannel) { // client has backchannel_logout_uri
                    continue;
                }
                if(StringUtils.isNotBlank(client.getFrontChannelLogoutUri())) {
                    String logoutUri = client.getFrontChannelLogoutUri();
                    if (client.getFrontChannelLogoutSessionRequired()) {
                        logoutUri = EndSessionUtils.appendSid(logoutUri, pair.getFirst().getOutsideSid(), appConfiguration.getIssuer());
                    }
                    frontchannelUris.add(logoutUri);
                }
            }

            backChannel(backchannelUris, pair.getSecond(), pair.getFirst().getOutsideSid());
            postLogoutRedirectUri = addStateInPostLogoutRedirectUri(postLogoutRedirectUri, state);

            if (frontchannelUris.isEmpty() && StringUtils.isNotBlank(postLogoutRedirectUri)) { // no front-channel
                log.trace("No frontchannel_redirect_uri's found in clients involved in SSO.");

                try {
                    log.trace("Redirect to postlogout_redirect_uri: " + postLogoutRedirectUri);
                    return Response.status(Response.Status.FOUND).location(new URI(postLogoutRedirectUri)).build();
                } catch (URISyntaxException e) {
                    final String message = "Failed to create URI for " + postLogoutRedirectUri + " postlogout_redirect_uri.";
                    log.error(message);
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_REQUEST, message)).build();
                }
            }


            return httpBased(frontchannelUris, postLogoutRedirectUri, state, pair, httpRequest);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(GluuErrorResponseType.SERVER_ERROR))
                    .build());
        }
    }

    /**
     * Adds state param in the post_logout_redirect_uri whether it exists.
     */
    private String addStateInPostLogoutRedirectUri(String postLogoutRedirectUri, String state) {
        if (StringUtils.isBlank(postLogoutRedirectUri) || StringUtils.isBlank(state)) {
            return postLogoutRedirectUri;
        }

        return UriBuilder.fromUri(postLogoutRedirectUri)
                .queryParam(EndSessionRequestParam.STATE, state)
                .build()
                .toString();
    }

    private void validateSid(String postLogoutRedirectUri, Jwt idToken, SessionId session) {
        if (idToken == null) {
            return;
        }
        final String sid = idToken.getClaims().getClaimAsString("sid");
        if (StringUtils.isNotBlank(sid) && !sid.equals(session.getOutsideSid())) {
            log.error("sid in id_token_hint does not match sid of the session. id_token_hint sid: {}, session sid: {}", sid, session.getOutsideSid());
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_REQUEST, "sid in id_token_hint does not match sid of the session"));
        }
    }

    private void backChannel(Map<String, Client> backchannelUris, AuthorizationGrant grant, String outsideSid) throws InterruptedException {
        if (backchannelUris.isEmpty()) {
            return;
        }

        log.trace("backchannel_redirect_uri's: " + backchannelUris);

        final ExecutorService executorService = EndSessionUtils.getExecutorService();
        for (final Map.Entry<String, Client> entry : backchannelUris.entrySet()) {
            final JsonWebResponse logoutToken = logoutTokenFactory.createLogoutToken(entry.getValue(), outsideSid, grant.getUser());
            if (logoutToken == null) {
                log.error("Failed to create logout_token for client: " + entry.getValue().getClientId());
                return;
            }
            executorService.execute(() -> EndSessionUtils.callRpWithBackchannelUri(entry.getKey(), logoutToken.toString()));
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        log.trace("Finished backchannel calls.");
    }

    private Response createErrorResponse(String postLogoutRedirectUri, EndSessionErrorResponseType error, String reason) {
        log.debug(reason);
        try {
            if (allowPostLogoutRedirect(postLogoutRedirectUri)) {
                if (ErrorHandlingMethod.REMOTE == appConfiguration.getErrorHandlingMethod()) {
                    String separator = postLogoutRedirectUri.contains("?") ? "&" : "?";
                    postLogoutRedirectUri = postLogoutRedirectUri + separator + errorResponseFactory.getErrorAsQueryString(error, "", reason);
                }
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

    private Jwt validateIdTokenHint(String idTokenHint, String postLogoutRedirectUri) {
        if (appConfiguration.getForceIdTokenHintPrecense() && StringUtils.isBlank(idTokenHint)) { // must be present for logout tests #1279
            final String reason = "id_token_hint is not set";
            log.trace(reason);
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_REQUEST, reason));
        }

        final AuthorizationGrant tokenHintGrant = getTokenHintGrant(idTokenHint);
        if (appConfiguration.getForceIdTokenHintPrecense() && tokenHintGrant == null) { // must be present for logout tests #1279
            final String reason = "id_token_hint is not set";
            log.trace(reason);
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_REQUEST, reason));
        }

        // id_token_hint is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(idTokenHint)) {
            if (tokenHintGrant == null) {
                final String reason = "id_token_hint is not valid. Logout is rejected. id_token_hint can be skipped or otherwise valid value must be provided.";
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }
            try {
                return Jwt.parse(idTokenHint);
            } catch (InvalidJwtException e) {
                log.error("Unable to parse id_token_hint as JWT.", e);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, "Unable to parse id_token_hint as JWT."));
            }
        }
        return null;
    }

    private AuthorizationGrant getTokenHintGrant(String idTokenHint) {
        if (StringUtils.isBlank(idTokenHint)) {
            return null;
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant != null) {
            return authorizationGrant;
        }

        Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
        if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
            return authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
        }
        return null;
    }


    private String validatePostLogoutRedirectUri(String postLogoutRedirectUri, Pair<SessionId, AuthorizationGrant> pair) {
        try {
            if (StringUtils.isBlank(postLogoutRedirectUri)) {
                return "";
            }
            if (appConfiguration.getAllowPostLogoutRedirectWithoutValidation()) {
                log.trace("Skipped post_logout_redirect_uri validation (because allowPostLogoutRedirectWithoutValidation=true)");
                return postLogoutRedirectUri;
            }

            final String result;
            if (pair.getSecond() == null) {
                result = redirectionUriService.validatePostLogoutRedirectUri(pair.getFirst(), postLogoutRedirectUri);
            } else {
                result = redirectionUriService.validatePostLogoutRedirectUri(pair.getSecond().getClient().getClientId(), postLogoutRedirectUri);
            }

            if (StringUtils.isBlank(result)) {
                log.trace("Failed to validate post_logout_redirect_uri.");
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, ""));
            }

            if (StringUtils.isNotBlank(result)) {
                return result;
            }
            log.trace("Unable to validate post_logout_redirect_uri.");
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, ""));
        } catch (WebApplicationException e) {
            if (pair.getFirst() != null) {
                log.error(e.getMessage(), e);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, ""));
            } else {
                throw e;
            }
        }
    }

    private Response httpBased(Set<String> frontchannelUris, String postLogoutRedirectUri, String state, Pair<SessionId, AuthorizationGrant> pair, HttpServletRequest httpRequest) {
        try {
            final EndSessionContext context = new EndSessionContext(httpRequest, frontchannelUris, postLogoutRedirectUri, pair.getFirst());
            final String htmlFromScript = externalEndSessionService.getFrontchannelHtml(context);
            if (StringUtils.isNotBlank(htmlFromScript)) {
                log.debug("HTML from `getFrontchannelHtml` external script: " + htmlFromScript);
                return okResponse(htmlFromScript);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // default handling
        final String html = EndSessionUtils.createFronthannelHtml(frontchannelUris, postLogoutRedirectUri, state);
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

    private Pair<SessionId, AuthorizationGrant> getPair(String idTokenHint, String sessionId, HttpServletRequest httpRequest) {
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
            if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
                authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
            }
        }

        SessionId ldapSessionId = null;

        try {
            String id = sessionId;
            if (StringHelper.isEmpty(id)) {
                id = cookieService.getSessionIdFromCookie(httpRequest);
            }
            if (StringHelper.isNotEmpty(id)) {
                ldapSessionId = sessionIdService.getSessionId(id);
            }
        } catch (Exception e) {
            log.error("Failed to current session id.", e);
        }
        return new Pair<>(ldapSessionId, authorizationGrant);
    }

    private void endSession(Pair<SessionId, AuthorizationGrant> pair, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Clean up authorization session
        removeConsentSessionId(httpRequest, httpResponse);

        removeSessionId(pair, httpResponse);

        boolean isExternalLogoutPresent;
        boolean externalLogoutResult = false;

        isExternalLogoutPresent = externalApplicationSessionService.isEnabled();
        if (isExternalLogoutPresent) {
            String userName = pair.getFirst().getSessionAttributes().get(Constants.AUTHENTICATED_USER);
            externalLogoutResult = externalApplicationSessionService.executeExternalEndSessionMethods(httpRequest, pair.getFirst());
            log.info("End session result for '{}': '{}'", userName, "logout", externalLogoutResult);
        }

        boolean isGrantAndExternalLogoutSuccessful = isExternalLogoutPresent && externalLogoutResult;
        if (isExternalLogoutPresent && !isGrantAndExternalLogoutSuccessful) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.UNAUTHORIZED, EndSessionErrorResponseType.INVALID_GRANT, "External logout is present but executed external logout script returned failed result.");
        }

        grantService.logout(pair.getFirst().getDn());

        if (identity != null) {
            identity.logout();
        }
    }

    private Set<Client> getSsoClients(Pair<SessionId, AuthorizationGrant> pair) {
        SessionId sessionId = pair.getFirst();
        AuthorizationGrant authorizationGrant = pair.getSecond();
        if (sessionId == null) {
            log.error("session_id is not passed to endpoint (as cookie or manually). Therefore unable to match clients for session_id.");
            return Sets.newHashSet();
        }

        final Set<Client> clients = sessionId.getPermissionGrantedMap() != null ?
                clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true) :
                Sets.newHashSet();
        if (authorizationGrant != null) {
            clients.add(authorizationGrant.getClient());
        }
        return clients;
    }

    private void removeSessionId(Pair<SessionId, AuthorizationGrant> pair, HttpServletResponse httpResponse) {
        try {
            boolean result = sessionIdService.remove(pair.getFirst());
            if (!result) {
                log.error("Failed to remove session_id '{}'", pair.getFirst().getId());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            cookieService.removeSessionIdCookie(httpResponse);
            cookieService.removeOPBrowserStateCookie(httpResponse);
        }
    }

    private void removeConsentSessionId(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String id = cookieService.getConsentSessionIdFromCookie(httpRequest);

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
            cookieService.removeConsentSessionIdCookie(httpResponse);
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