/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.session.ws.rs;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorHandlingMethod;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.gluu.GluuErrorResponseType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.model.session.EndSessionRequestParam;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.util.URLPatternList;
import io.jans.as.model.util.Util;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.service.*;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.external.ExternalEndSessionService;
import io.jans.as.server.service.external.context.EndSessionContext;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.model.security.Identity;
import io.jans.util.Pair;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.BooleanUtils.isTrue;

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

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Override
    public Response requestEndSession(String idTokenHint, String postLogoutRedirectUri, String state, String sid,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext sec) {
        try {
            log.debug("Attempting to end session, idTokenHint: {}, postLogoutRedirectUri: {}, sid: {}, Is Secure = {}",
                    idTokenHint, postLogoutRedirectUri, sid, sec.isSecure());

            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.END_SESSION);

            final SessionId sidSession = validateSidRequestParameter(sid, postLogoutRedirectUri);
            Jwt validatedIdToken = validateIdTokenHint(idTokenHint, sidSession, postLogoutRedirectUri);

            final Pair<SessionId, AuthorizationGrant> pair = getPair(idTokenHint, validatedIdToken, sid, httpRequest);
            if (pair.getFirst() == null) {
                final String reason = "Failed to identify session by session_id query parameter or by session_id cookie.";
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }

            postLogoutRedirectUri = validatePostLogoutRedirectUri(postLogoutRedirectUri, pair);
            validateSid(postLogoutRedirectUri, validatedIdToken, pair.getFirst());

            endSession(pair, httpRequest, httpResponse);
            auditLogging(httpRequest, pair);

            Set<Client> clients = getSsoClients(pair);
            Set<String> frontchannelUris = Sets.newHashSet();
            Map<String, Client> backchannelUris = Maps.newHashMap();

            collectFrontAndBackChannelsUris(pair, clients, frontchannelUris, backchannelUris);

            backChannel(backchannelUris, pair.getSecond(), pair.getFirst());
            postLogoutRedirectUri = addStateInPostLogoutRedirectUri(postLogoutRedirectUri, state);

            if (frontchannelUris.isEmpty() && StringUtils.isNotBlank(postLogoutRedirectUri)) { // no front-channel
                return noFrontChannelRedirectUrisResponse(postLogoutRedirectUri);
            }

            return httpBased(frontchannelUris, postLogoutRedirectUri, state, pair, httpRequest);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw e;
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error(e.getMessage(), e);
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(GluuErrorResponseType.SERVER_ERROR))
                    .build());
        }
    }

    private void collectFrontAndBackChannelsUris(Pair<SessionId, AuthorizationGrant> pair, Set<Client> clients, Set<String> frontchannelUris, Map<String, Client> backchannelUris) {
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
            if (StringUtils.isNotBlank(client.getFrontChannelLogoutUri())) {
                String logoutUri = client.getFrontChannelLogoutUri();
                if (isTrue(client.getFrontChannelLogoutSessionRequired())) {
                    logoutUri = EndSessionUtils.appendSid(logoutUri, pair.getFirst().getOutsideSid(), appConfiguration.getIssuer());
                }
                frontchannelUris.add(logoutUri);
            }
        }
    }

    private Response noFrontChannelRedirectUrisResponse(String postLogoutRedirectUri) {
        log.trace("No frontchannel_redirect_uri's found in clients involved in SSO.");

        try {
            log.trace("Redirect to postlogout_redirect_uri: {}", postLogoutRedirectUri);
            return Response.status(Response.Status.FOUND).location(new URI(postLogoutRedirectUri)).build();
        } catch (URISyntaxException e) {
            final String message = "Failed to create URI for " + postLogoutRedirectUri + " postlogout_redirect_uri.";
            log.error(message);
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponseFactory.errorAsJson(EndSessionErrorResponseType.INVALID_REQUEST, message)).build();
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

    private void backChannel(Map<String, Client> backchannelUris, AuthorizationGrant grant, SessionId session) {
        if (backchannelUris.isEmpty()) {
            return;
        }

        log.trace("backchannel_redirect_uri's: {}", backchannelUris);

        User user = grant != null ? grant.getUser() : null;
        if (user == null) {
            user = sessionIdService.getUser(session);
        }

        final ExecutorService executorService = EndSessionUtils.getExecutorService();
        for (final Map.Entry<String, Client> entry : backchannelUris.entrySet()) {
            final JsonWebResponse logoutToken = logoutTokenFactory.createLogoutToken(entry.getValue(), session.getOutsideSid(), user);
            if (logoutToken == null) {
                log.error("Failed to create logout_token for client: {}", entry.getValue().getClientId());
                return;
            }
            executorService.execute(() -> EndSessionUtils.callRpWithBackchannelUri(entry.getKey(), logoutToken.toString()));
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
            log.trace("Finished backchannel calls.");
        } catch (InterruptedException e) {
            log.error("Thread is interrupted.");
            Thread.currentThread().interrupt();
            throw new WebApplicationException(Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(GluuErrorResponseType.SERVER_ERROR))
                    .build());
        }

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

    private SessionId validateSidRequestParameter(String sid, String postLogoutRedirectUri) {
        // sid is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(sid)) {
            SessionId sessionIdObject = sessionIdService.getSessionBySid(sid);
            if (sessionIdObject == null) {
                final String reason = "sid parameter in request is not valid. Logout is rejected. sid parameter in request can be skipped or otherwise valid value must be provided.";
                log.error(reason);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }
            return sessionIdObject;
        }
        return null;
    }

    protected Jwt validateIdTokenHint(String idTokenHint, SessionId sidSession, String postLogoutRedirectUri) {
        final boolean isIdTokenHintRequired = isTrue(appConfiguration.getForceIdTokenHintPrecense());

        if (isIdTokenHintRequired && StringUtils.isBlank(idTokenHint)) { // must be present for logout tests #1279
            final String reason = "id_token_hint is not set";
            log.trace(reason);
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_REQUEST, reason));
        }

        if (StringUtils.isBlank(idTokenHint) && !isIdTokenHintRequired) {
            return null;
        }

        // id_token_hint is not required but if it is present then we must validate it #831
        if (StringUtils.isNotBlank(idTokenHint) || isIdTokenHintRequired) {
            final boolean isRejectEndSessionIfIdTokenExpired = appConfiguration.getRejectEndSessionIfIdTokenExpired();
            final AuthorizationGrant tokenHintGrant = getTokenHintGrant(idTokenHint);

            if (tokenHintGrant == null && isRejectEndSessionIfIdTokenExpired) {
                final String reason = "id_token_hint is not valid. Logout is rejected. id_token_hint can be skipped or otherwise valid value must be provided.";
                log.trace(reason);
                throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, reason));
            }
            return validateIdTokenJwt(tokenHintGrant, idTokenHint, sidSession, postLogoutRedirectUri);
        }
        return null;
    }

    private Jwt validateIdTokenJwt(AuthorizationGrant tokenHintGrant, String idTokenHint, SessionId sidSession, String postLogoutRedirectUri) {
        try {
            final Jwt jwt = Jwt.parse(idTokenHint);
            if (tokenHintGrant != null) { // id_token is in db
                log.debug("Found id_token in db.");
                return jwt;
            }
            validateIdTokenSignature(sidSession, jwt, postLogoutRedirectUri);
            log.debug("id_token is validated successfully.");
            return jwt;
        } catch (InvalidJwtException e) {
            log.error("Unable to parse id_token_hint as JWT.", e);
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, "Unable to parse id_token_hint as JWT."));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unable to validate id_token_hint as JWT.", e);
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, "Unable to validate id_token_hint as JWT."));
        }
    }

    private void validateIdTokenSignature(SessionId sidSession, Jwt jwt, String postLogoutRedirectUri) throws InvalidJwtException, CryptoProviderException {
        // verify jwt signature if we can't find it in db
        if (!cryptoProvider.verifySignature(jwt.getSigningInput(), jwt.getEncodedSignature(), jwt.getHeader().getKeyId(),
                null, null, jwt.getHeader().getSignatureAlgorithm())) {
            log.error("id_token signature verification failed.");
            throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, "id_token signature verification failed."));
        }

        if (isTrue(appConfiguration.getAllowEndSessionWithUnmatchedSid())) {
            return;
        }
        final String sidClaim = jwt.getClaims().getClaimAsString("sid");
        if (sidSession != null && StringUtils.equals(sidSession.getOutsideSid(), sidClaim)) {
            return;
        }
        log.error("sid claim from id_token does not match to any valid session on AS.");
        throw new WebApplicationException(createErrorResponse(postLogoutRedirectUri, EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION, "sid claim from id_token does not match to any valid session on AS."));
    }

    protected AuthorizationGrant getTokenHintGrant(String idTokenHint) {
        if (StringUtils.isBlank(idTokenHint)) {
            return null;
        }

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(TokenHashUtil.hash(idTokenHint));
        if (authorizationGrant != null) {
            return authorizationGrant;
        }

        authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
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
            if (isTrue(appConfiguration.getAllowPostLogoutRedirectWithoutValidation())) {
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
                log.debug("HTML from `getFrontchannelHtml` external script: {}", htmlFromScript);
                return okResponse(htmlFromScript);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // default handling
        final String html = EndSessionUtils.createFronthannelHtml(frontchannelUris, postLogoutRedirectUri, state);
        log.debug("Constructed html logout page: {}", html);
        return okResponse(html);
    }

    private Response okResponse(String html) {
        return Response.ok().
                cacheControl(ServerUtil.cacheControl(true, true)).
                header("Pragma", "no-cache").
                type(MediaType.TEXT_HTML_TYPE).entity(html).
                build();
    }

    private Pair<SessionId, AuthorizationGrant> getPair(String idTokenHint, Jwt validatedIdToken, String sid, HttpServletRequest httpRequest) {
        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            Boolean endSessionWithAccessToken = appConfiguration.getEndSessionWithAccessToken();
            if ((endSessionWithAccessToken != null) && endSessionWithAccessToken) {
                authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(idTokenHint);
            }
        }

        SessionId sessionId = null;

        try {
            String cookieSessionId = cookieService.getSessionIdFromCookie(httpRequest);
            if (StringHelper.isNotEmpty(cookieSessionId)) {
                sessionId = sessionIdService.getSessionId(cookieSessionId);
            }
            if (sessionId == null && StringUtils.isNotBlank(sid)) {
                sessionId = sessionIdService.getSessionBySid(sid);
            }
            if (sessionId == null && validatedIdToken != null) {
                final String sidClaim = validatedIdToken.getClaims().getClaimAsString("sid");
                if (StringUtils.isNotBlank(sidClaim)) {
                    sessionId = sessionIdService.getSessionBySid(sidClaim);
                }
            }

            if (sessionId == null) {
                log.trace("Unable to find session for ending.");
            } else {
                log.trace("Found session for ending successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to find current session id.", e);
        }
        return new Pair<>(sessionId, authorizationGrant);
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
            log.info("End session result for '{}': '{}'", userName, externalLogoutResult);
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