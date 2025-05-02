/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.token.ws.rs;

import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.authzdetails.AuthzDetails;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.TokenType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.binding.TokenBindingMessage;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.auth.DpopService;
import io.jans.as.server.authorize.ws.rs.AuthzDetailsService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.*;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.session.SessionClient;
import io.jans.as.server.model.token.JwrService;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.*;
import io.jans.as.server.service.ciba.CibaRequestService;
import io.jans.as.server.service.external.ExternalResourceOwnerPasswordCredentialsService;
import io.jans.as.server.service.external.ExternalUpdateTokenService;
import io.jans.as.server.service.external.context.ExternalResourceOwnerPasswordCredentialsContext;
import io.jans.as.server.service.external.context.ExternalUpdateTokenContext;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.uma.service.UmaTokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.token.TokenEntity;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static io.jans.as.model.config.Constants.*;
import static io.jans.as.server.util.ServerUtil.prepareForLogs;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Provides interface for token REST web services
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version October 5, 2021
 */
@Path("/")
public class TokenRestWebServiceImpl implements TokenRestWebService {

    @Inject
    private Logger log;

    @Inject
    private Identity identity;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private UserService userService;

    @Inject
    private GrantService grantService;

    @Inject
    private AuthenticationFilterService authenticationFilterService;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private UmaTokenService umaTokenService;

    @Inject
    private ExternalResourceOwnerPasswordCredentialsService externalResourceOwnerPasswordCredentialsService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CibaRequestService cibaRequestService;

    @Inject
    private DeviceAuthorizationService deviceAuthorizationService;

    @Inject
    private ExternalUpdateTokenService externalUpdateTokenService;

    @Inject
    private TokenRestWebServiceValidator tokenRestWebServiceValidator;

    @Inject
    private TokenExchangeService tokenExchangeService;

    @Inject
    private TokenCreatorService tokenCreatorService;

    @Inject
    private StatService statService;

    @Inject
    private DpopService dPoPService;

    @Inject
    private AuthzDetailsService authzDetailsService;

    @Inject
    private TxTokenService txTokenService;

    private final ConcurrentMap<String, String> refreshTokenLock = Maps.newConcurrentMap();

    @Override
    public Response requestAccessToken(String grantType, String code,
                                       String redirectUri, String username, String password, String scope,
                                       String authorizationDetails,
                                       String assertion, String refreshToken,
                                       String clientId, String clientSecret, String codeVerifier,
                                       String ticket, String claimToken, String claimTokenFormat, String pctCode,
                                       String rptCode, String authReqId, String deviceCode,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug(
                "Attempting to request access token: grantType = {}, code = {}, redirectUri = {}, username = {}, refreshToken = {}, " +
                        "clientId = {}, ExtraParams = {}, isSecure = {}, codeVerifier = {}, ticket = {}, authorizationDetails = {}",
                grantType, code, redirectUri, username, refreshToken, clientId, prepareForLogs(request.getParameterMap()),
                sec.isSecure(), codeVerifier, ticket, authorizationDetails);

        boolean isUma = StringUtils.isNotBlank(ticket);
        if (isUma) {
            return umaTokenService.requestRpt(grantType, ticket, claimToken, claimTokenFormat, pctCode, rptCode, scope, request, response);
        }

        OAuth2AuditLog auditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.TOKEN_REQUEST);
        auditLog.setClientId(clientId);
        auditLog.setUsername(username);
        auditLog.setScope(scope);

        String tokenBindingHeader = request.getHeader("Sec-Token-Binding");

        scope = ServerUtil.urlDecode(scope); // it may be encoded in uma case

        try {
            tokenRestWebServiceValidator.validateParams(grantType, code, refreshToken, auditLog);

            GrantType gt = GrantType.fromString(grantType);
            log.debug("Grant type: '{}'", gt);

            Client client = tokenRestWebServiceValidator.validateClient(getClient(), auditLog);
            tokenRestWebServiceValidator.validateGrantType(gt, client, auditLog);
            String dpopStr = dPoPService.getDPoPJwkThumbprint(request, client, auditLog);

            final Function<JsonWebResponse, Void> idTokenTokingBindingPreprocessing = TokenBindingMessage.createIdTokenTokingBindingPreprocessing(
                    tokenBindingHeader, client.getIdTokenTokenBindingCnf()); // for all except authorization code grant
            final SessionId sessionIdObj = sessionIdService.getSessionId(request);
            final Function<JsonWebResponse, Void> idTokenPreProcessing = JwrService.wrapWithSidFunction(idTokenTokingBindingPreprocessing, sessionIdObj != null ? sessionIdObj.getOutsideSid() : null);

            final ExecutionContext executionContext = new ExecutionContext(request, response);
            executionContext.setCertAsPem(request.getHeader(X_CLIENTCERT));
            executionContext.setDpop(dpopStr);
            executionContext.setClient(client);
            executionContext.setAppConfiguration(appConfiguration);
            executionContext.setAttributeService(attributeService);
            executionContext.setAuditLog(auditLog);
            executionContext.setScopes(StringUtils.isNotBlank(scope) ? new HashSet<>(Arrays.asList(scope.split(" "))) : new HashSet<>());

            final AuthzDetails authzDetails = authzDetailsService.validateAuthorizationDetails(authorizationDetails, executionContext);
            executionContext.setAuthzDetails(authzDetails);

            if (gt == GrantType.AUTHORIZATION_CODE) {
                return processAuthorizationCode(code, scope, codeVerifier, sessionIdObj, redirectUri, executionContext);
            } else if (gt == GrantType.REFRESH_TOKEN) {
                return processRefreshTokenGrant(scope, refreshToken, idTokenPreProcessing, executionContext);
            } else if (gt == GrantType.CLIENT_CREDENTIALS) {
                return processClientGredentials(scope, request, auditLog, client, idTokenPreProcessing, executionContext);
            } else if (gt == GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS) {
                return processROPC(username, password, scope, gt, idTokenPreProcessing, executionContext);
            } else if (gt == GrantType.CIBA) {
                return processCIBA(scope, authReqId, idTokenPreProcessing, executionContext);
            } else if (gt == GrantType.DEVICE_CODE) {
                return processDeviceCodeGrantType(executionContext, deviceCode, scope);
            } else if (gt == GrantType.TOKEN_EXCHANGE) {

                if (txTokenService.isTxTokenFlow(request)) {
                    return txTokenService.processTxToken(executionContext);
                }

                final JSONObject responseJson = tokenExchangeService.processTokenExchange(scope, idTokenPreProcessing, executionContext);
                return response(Response.ok().entity(responseJson.toString()), auditLog);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return response(Response.status(500), auditLog);
        }

        throw new WebApplicationException(tokenRestWebServiceValidator.error(400, TokenErrorResponseType.UNSUPPORTED_GRANT_TYPE, "Unsupported Grant Type.").build());
    }

    private Response processROPC(String username, String password, String scope, GrantType gt, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) throws SearchException {
        boolean authenticated = false;
        User user = null;
        if (authenticationFilterService.isEnabled()) {
            String userDn = authenticationFilterService.processAuthenticationFilters(executionContext.getHttpRequest().getParameterMap());
            if (StringHelper.isNotEmpty(userDn)) {
                user = userService.getUserByDn(userDn);
                authenticated = true;
            }
        }


        if (!authenticated) {
            user = authenticateUser(username, password, executionContext, user);
        }

        tokenRestWebServiceValidator.validateUser(user, executionContext.getAuditLog());

        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant = authorizationGrantList.createResourceOwnerPasswordCredentialsGrant(user, executionContext.getClient());
        executionContext.setGrant(resourceOwnerPasswordCredentialsGrant);

        SessionId sessionId = identity.getSessionId();
        if (sessionId != null) {
            resourceOwnerPasswordCredentialsGrant.setAcrValues(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME);
            resourceOwnerPasswordCredentialsGrant.setSessionDn(sessionId.getDn());
            resourceOwnerPasswordCredentialsGrant.save(); // call save after object modification!!!

            sessionId.getSessionAttributes().put(Constants.AUTHORIZED_GRANT, gt.getValue());
            boolean updateResult = sessionIdService.updateSessionId(sessionId, false, true, true);
            if (!updateResult) {
                log.debug("Failed to update session entry: '{}'", sessionId.getId());
            }
        }

        RefreshToken reToken = tokenCreatorService.createRefreshToken(executionContext, scope);

        scope = resourceOwnerPasswordCredentialsGrant.checkScopesPolicy(scope);
        AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), resourceOwnerPasswordCredentialsGrant);

        AccessToken accessToken = resourceOwnerPasswordCredentialsGrant.createAccessToken(executionContext); // create token after scopes are checked

        IdToken idToken = null;
        if (isTrue(appConfiguration.getOpenidScopeBackwardCompatibility()) && resourceOwnerPasswordCredentialsGrant.getScopes().contains("openid")) {
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), resourceOwnerPasswordCredentialsGrant, executionContext.getClient(), appConfiguration, attributeService);
            context.setExecutionContext(executionContext);

            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(idTokenPreProcessing);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = resourceOwnerPasswordCredentialsGrant.createIdToken(
                    null, null, null, null, null, executionContext);
        }

        executionContext.getAuditLog().updateOAuth2AuditLog(resourceOwnerPasswordCredentialsGrant, true);

        return response(Response.ok().entity(getJSonResponse(accessToken,
                accessToken.getTokenType(),
                accessToken.getExpiresIn(),
                reToken,
                scope,
                idToken, checkedAuthzDetails)), executionContext.getAuditLog());
    }

    private Response processClientGredentials(String scope, HttpServletRequest request, OAuth2AuditLog auditLog, Client client, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) {
        ClientCredentialsGrant clientCredentialsGrant = authorizationGrantList.createClientCredentialsGrant(new User(), client);

        scope = clientCredentialsGrant.checkScopesPolicy(scope);
        AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), clientCredentialsGrant);

        executionContext.setGrant(clientCredentialsGrant);
        AccessToken accessToken = clientCredentialsGrant.createAccessToken(executionContext); // create token after scopes are checked

        IdToken idToken = null;
        if (isTrue(appConfiguration.getOpenidScopeBackwardCompatibility()) && clientCredentialsGrant.getScopes().contains(OPENID)) {
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(request, clientCredentialsGrant, client, appConfiguration, attributeService);

            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(idTokenPreProcessing);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = clientCredentialsGrant.createIdToken(
                    null, null, null, null, null, executionContext);
        }

        auditLog.updateOAuth2AuditLog(clientCredentialsGrant, true);

        return response(Response.ok().entity(getJSonResponse(accessToken,
                accessToken.getTokenType(),
                accessToken.getExpiresIn(),
                null,
                scope,
                idToken, checkedAuthzDetails)), auditLog);
    }

    private Response processRefreshTokenGrant(String scope, String refreshToken, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) {
        final Client client = executionContext.getClient();
        final OAuth2AuditLog auditLog = executionContext.getAuditLog();

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), refreshToken);
        tokenRestWebServiceValidator.validateGrant(authorizationGrant, client, refreshToken, auditLog);

        final RefreshToken refreshTokenObject = authorizationGrant.getRefreshToken(refreshToken);
        tokenRestWebServiceValidator.validateRefreshToken(refreshTokenObject, auditLog);

        executionContext.setGrant(authorizationGrant);

        // The authorization server MAY issue a new refresh token, in which case
        // the client MUST discard the old refresh token and replace it with the new refresh token.
        RefreshToken reToken = null;
        if (isFalse(appConfiguration.getSkipRefreshTokenDuringRefreshing())) {
            if (isTrue(appConfiguration.getRefreshTokenExtendLifetimeOnRotation())) {
                reToken = tokenCreatorService.createRefreshToken(executionContext, scope); // extend lifetime
            } else {
                log.trace("Create refresh token with fixed (not extended) lifetime taken from previous refresh token.");

                reToken = authorizationGrant.createRefreshToken(executionContext, refreshTokenObject.getExpirationDate()); // do not extend lifetime
            }
        }

        authorizationGrant.checkScopesPolicy(scope);
        scope = authorizationGrant.getScopesAsString();
        AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), authorizationGrant);

        AccessToken accToken = authorizationGrant.createAccessToken(executionContext); // create token after scopes are checked

        IdToken idToken = null;
        if (isTrue(appConfiguration.getOpenidScopeBackwardCompatibility()) && authorizationGrant.getScopes().contains(OPENID)) {
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), authorizationGrant, client, appConfiguration, attributeService);
            context.setExecutionContext(executionContext);

            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(idTokenPreProcessing);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = authorizationGrant.createIdToken(
                    null, null, accToken, null, null, executionContext);
        }

        TokenEntity lockedRefreshToken = lockAndRemoveRefreshToken(refreshToken);
        if (lockedRefreshToken == null) {
            log.trace("Failed to lock refresh token {}", refreshToken);
            return response(error(400, TokenErrorResponseType.INVALID_GRANT, "Failed to lock refresh token."), auditLog);
        }

        tokenExchangeService.rotateDeviceSecretOnRefreshToken(executionContext.getHttpRequest(), authorizationGrant, scope);

        statService.reportActiveUser(authorizationGrant.getUserId());
        auditLog.updateOAuth2AuditLog(authorizationGrant, true);

        final String entity = getJSonResponse(
                accToken,
                accToken.getTokenType(),
                accToken.getExpiresIn(),
                reToken,
                scope,
                idToken, checkedAuthzDetails);
        return response(Response.ok().entity(entity), auditLog);
    }

    private TokenEntity lockAndRemoveRefreshToken(String refreshTokenCode) {
        try {
            synchronized (refreshTokenLock) {
                if (refreshTokenLock.containsKey(refreshTokenCode)) {
                    log.trace("Refresh token is already used by another request. Refresh token  code: {}", refreshTokenCode);
                    return null;
                }

                refreshTokenLock.put(refreshTokenCode, refreshTokenCode);

                final TokenEntity token = grantService.getGrantByCode(refreshTokenCode);
                grantService.remove(token);
                return token;
            }
        } catch (Exception e) {
            // ignore
            log.trace(e.getMessage(), e);
        } finally {
            refreshTokenLock.remove(refreshTokenCode);
        }
        return null;
    }

    private Response processAuthorizationCode(String code, String scope, String codeVerifier, SessionId sessionIdObj, String redirectUri, ExecutionContext executionContext) {
        Client client = executionContext.getClient();

        log.debug("Attempting to find authorizationCodeGrant by clientId: '{}', code: '{}'", client.getClientId(), code);
        final AuthorizationCodeGrant authorizationCodeGrant = authorizationGrantList.getAuthorizationCodeGrant(code);
        executionContext.setGrant(authorizationCodeGrant);
        log.trace("AuthorizationCodeGrant : '{}'", authorizationCodeGrant);

        // if authorization code is not found then code was already used or wrong client provided = remove all grants with this auth code
        tokenRestWebServiceValidator.validateGrant(authorizationCodeGrant, client, code, executionContext.getAuditLog(), grant -> grantService.removeAllByAuthorizationCode(code));

        // validate redirectUri only for Authorization Code Flow. For First-Party App redirect uri is blank. It is perfectly valid case.
        // redirect uri must be validated after grant is validated
        if (!authorizationCodeGrant.isAuthorizationChallenge()) {
            tokenRestWebServiceValidator.validateRedirectUri(redirectUri, executionContext.getAuditLog());
        }
        tokenRestWebServiceValidator.validatePKCE(authorizationCodeGrant, codeVerifier, executionContext.getAuditLog(), client);
        dPoPService.validateDpopThumprint(authorizationCodeGrant.getDpopJkt(), executionContext.getDpop());

        authorizationCodeGrant.setIsCachedWithNoPersistence(false);
        authorizationCodeGrant.save();

        RefreshToken reToken = tokenCreatorService.createRefreshToken(executionContext, scope);

        scope = authorizationCodeGrant.checkScopesPolicy(scope);
        AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), authorizationCodeGrant);

        AccessToken accToken = authorizationCodeGrant.createAccessToken(executionContext); // create token after scopes are checked
        final String deviceSecret = tokenExchangeService.createNewDeviceSecret(authorizationCodeGrant.getSessionDn(), client, authorizationCodeGrant.getScopesAsString());

        IdToken idToken = null;
        if (authorizationCodeGrant.getScopes().contains(OPENID)) {
            String nonce = authorizationCodeGrant.getNonce();
            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                    appConfiguration.getLegacyIdTokenClaims());
            final String idTokenTokenBindingCnf = client.getIdTokenTokenBindingCnf();
            Function<JsonWebResponse, Void> authorizationCodePreProcessing = jsonWebResponse -> {
                if (StringUtils.isNotBlank(idTokenTokenBindingCnf) && StringUtils.isNotBlank(authorizationCodeGrant.getTokenBindingHash())) {
                    TokenBindingMessage.setCnfClaim(jsonWebResponse, authorizationCodeGrant.getTokenBindingHash(), idTokenTokenBindingCnf);
                }
                return null;
            };

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), authorizationCodeGrant, client, appConfiguration, attributeService);

            executionContext.setDeviceSecret(deviceSecret);
            executionContext.setIncludeIdTokenClaims(includeIdTokenClaims);
            executionContext.setPreProcessing(JwrService.wrapWithSidFunction(authorizationCodePreProcessing, sessionIdObj != null ? sessionIdObj.getOutsideSid() : null));
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            idToken = authorizationCodeGrant.createIdToken(
                    nonce, authorizationCodeGrant.getAuthorizationCode(), accToken, null, null, executionContext);
        }

        executionContext.getAuditLog().updateOAuth2AuditLog(authorizationCodeGrant, true);

        grantService.removeAuthorizationCode(authorizationCodeGrant.getAuthorizationCode().getCode());

        JSONObject jsonObj = new JSONObject();
        try {
            fillJsonObject(jsonObj, accToken, accToken.getTokenType(), accToken.getExpiresIn(), reToken, scope, idToken, checkedAuthzDetails);
            if (StringUtils.isNotBlank(deviceSecret)) {
                jsonObj.put("device_token", deviceSecret);
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return response(Response.ok().entity(jsonObj.toString()), executionContext.getAuditLog());
    }

    @Nullable
    private Client getClient() {
        SessionClient sessionClient = identity.getSessionClient();
        Client client = null;
        if (sessionClient != null) {
            client = sessionClient.getClient();
            log.debug("Get sessionClient: '{}'", sessionClient);
        }
        return client;
    }

    private User authenticateUser(String username, String password, ExecutionContext executionContext, User user) {

        if (externalResourceOwnerPasswordCredentialsService.isEnabled()) {
            final ExternalResourceOwnerPasswordCredentialsContext context = new ExternalResourceOwnerPasswordCredentialsContext(executionContext);
            context.setUser(user);
            if (externalResourceOwnerPasswordCredentialsService.executeExternalAuthenticate(context)) {
                log.trace("RO PC - User is authenticated successfully by external script.");
                user = context.getUser();
            }
        } else {
            try {
                boolean authenticated = authenticationService.authenticate(username, password);
                if (authenticated) {
                    user = authenticationService.getAuthenticatedUser();
                }
            } catch (AuthenticationException ex) {
                log.trace("Failed to authenticate user ", new RuntimeException("User name or password is invalid"));
            }
        }
        return user;
    }

    /**
     * Processes token request for device code grant type.
     *
     * @param executionContext Execution context
     * @param deviceCode     Device code generated in device authn request.
     * @param scope          Scope registered in device authn request.
     */
    private Response processDeviceCodeGrantType(ExecutionContext executionContext, final String deviceCode, String scope) {
        log.debug("Attempting to find authorizationGrant by deviceCode: '{}'", deviceCode);
        final Client client = executionContext.getClient();
        final DeviceCodeGrant deviceCodeGrant = authorizationGrantList.getDeviceCodeGrant(deviceCode);
        executionContext.setGrant(deviceCodeGrant);

        log.trace("DeviceCodeGrant : '{}'", deviceCodeGrant);

        if (deviceCodeGrant != null) {
            if (!deviceCodeGrant.getClientId().equals(client.getClientId())) {
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.INVALID_GRANT, REASON_CLIENT_NOT_AUTHORIZED), executionContext.getAuditLog()));
            }

            RefreshToken refToken = tokenCreatorService.createRefreshToken(executionContext, scope);

            AccessToken accessToken = deviceCodeGrant.createAccessToken(executionContext);

            ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), deviceCodeGrant, client, appConfiguration, attributeService);
            context.setExecutionContext(executionContext);


            executionContext.setIncludeIdTokenClaims(Boolean.TRUE.equals(appConfiguration.getLegacyIdTokenClaims()));
            executionContext.setPreProcessing(null);
            executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

            IdToken idToken = deviceCodeGrant.createIdToken(
                    null, null, accessToken, refToken, null, executionContext);

            deviceCodeGrant.checkScopesPolicy(scope);
            AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), deviceCodeGrant);

            log.info("Device authorization in token endpoint processed and return to the client, device_code: {}", deviceCodeGrant.getDeviceCode());

            executionContext.getAuditLog().updateOAuth2AuditLog(deviceCodeGrant, true);

            grantService.removeByCode(deviceCodeGrant.getDeviceCode());

            return Response.ok().entity(getJSonResponse(accessToken, accessToken.getTokenType(),
                    accessToken.getExpiresIn(), refToken, scope, idToken, checkedAuthzDetails)).build();
        } else {
            final DeviceAuthorizationCacheControl cacheData = deviceAuthorizationService.getDeviceAuthzByDeviceCode(deviceCode);
            log.trace("DeviceAuthorizationCacheControl data : '{}'", cacheData);
            tokenRestWebServiceValidator.validateDeviceAuthorization(client, deviceCode, cacheData, executionContext.getAuditLog());

            long currentTime = new Date().getTime();
            Long lastAccess = cacheData.getLastAccessControl();
            if (lastAccess == null) {
                lastAccess = currentTime;
            }
            cacheData.setLastAccessControl(currentTime);
            deviceAuthorizationService.saveInCache(cacheData, true, true);

            if (cacheData.getStatus() == DeviceAuthorizationStatus.PENDING) {
                int intervalSeconds = appConfiguration.getBackchannelAuthenticationResponseInterval();
                long timeFromLastAccess = currentTime - lastAccess;

                if (timeFromLastAccess > intervalSeconds * 1000) {
                    log.debug("Access hasn't been granted yet for deviceCode: '{}'", deviceCode);
                    throw new WebApplicationException(response(error(400, TokenErrorResponseType.AUTHORIZATION_PENDING, "User hasn't answered yet"), executionContext.getAuditLog()));
                } else {
                    log.debug("Slow down protection deviceCode: '{}'", deviceCode);
                    throw new WebApplicationException(response(error(400, TokenErrorResponseType.SLOW_DOWN, "Client is asking too fast the token."), executionContext.getAuditLog()));
                }
            }
            if (cacheData.getStatus() == DeviceAuthorizationStatus.DENIED) {
                log.debug("The end-user denied the authorization request for deviceCode: '{}'", deviceCode);
                throw new WebApplicationException(response(error(400, TokenErrorResponseType.ACCESS_DENIED, "The end-user denied the authorization request."), executionContext.getAuditLog()));
            }
            log.debug("The authentication request has expired for deviceCode: '{}'", deviceCode);
            throw new WebApplicationException(response(error(400, TokenErrorResponseType.EXPIRED_TOKEN, "The authentication request has expired"), executionContext.getAuditLog()));
        }
    }

    private Response response(ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    private ResponseBuilder error(int status, TokenErrorResponseType type, String reason) {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(errorResponseFactory.errorAsJson(type, reason));
    }

    /**
     * Builds a JSon String with the structure for token issues.
     */
    public String getJSonResponse(AccessToken accessToken, TokenType tokenType,
                                  Integer expiresIn, RefreshToken refreshToken, String scope,
                                  IdToken idToken, AuthzDetails checkedAuthzDetails) {
        JSONObject jsonObj = new JSONObject();
        try {
            fillJsonObject(jsonObj, accessToken, tokenType, expiresIn, refreshToken, scope, idToken, checkedAuthzDetails);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }

    public static void fillJsonObject(JSONObject jsonObj, AccessToken accessToken, TokenType tokenType,
                                     Integer expiresIn, RefreshToken refreshToken, String scope,
                                     IdToken idToken, AuthzDetails checkedAuthzDetails) {
        jsonObj.put("access_token", accessToken.getCode()); // Required
        jsonObj.put("token_type", tokenType.toString()); // Required
        if (expiresIn != null) { // Optional
            jsonObj.put("expires_in", expiresIn);
        }
        if (refreshToken != null) { // Optional
            jsonObj.put("refresh_token", refreshToken.getCode());
        }
        if (scope != null) { // Optional
            jsonObj.put("scope", scope);
        }
        if (idToken != null) {
            jsonObj.put("id_token", idToken.getCode());
        }
        if (checkedAuthzDetails != null && checkedAuthzDetails.getDetails() != null && !checkedAuthzDetails.getDetails().isEmpty()) {
            jsonObj.put("authorization_details", checkedAuthzDetails.asJsonArray());
        }
    }

    private Response processCIBA(String scope, String authReqId, Function<JsonWebResponse, Void> idTokenPreProcessing, ExecutionContext executionContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.CIBA);

        log.debug("Attempting to find authorizationGrant by authReqId: '{}'", authReqId);
        final CIBAGrant cibaGrant = authorizationGrantList.getCIBAGrant(authReqId);
        executionContext.setGrant(cibaGrant);

        log.trace("AuthorizationGrant : '{}'", cibaGrant);

        Client client = executionContext.getClient();

        if (cibaGrant != null) {
            tokenRestWebServiceValidator.validateGrant(cibaGrant, client, authReqId, executionContext.getAuditLog());

            if (cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PING ||
                    cibaGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.POLL) {
                if (!cibaGrant.isTokensDelivered()) {
                    RefreshToken refToken = tokenCreatorService.createRefreshToken(executionContext, scope);
                    AccessToken accessToken = cibaGrant.createAccessToken(executionContext);

                    ExternalUpdateTokenContext context = new ExternalUpdateTokenContext(executionContext.getHttpRequest(), cibaGrant, client, appConfiguration, attributeService);
                    context.setExecutionContext(executionContext);

                    executionContext.setIncludeIdTokenClaims(Boolean.TRUE.equals(appConfiguration.getLegacyIdTokenClaims()));
                    executionContext.setPreProcessing(idTokenPreProcessing);
                    executionContext.setPostProcessor(externalUpdateTokenService.buildModifyIdTokenProcessor(context));

                    IdToken idToken = cibaGrant.createIdToken(
                            null, null, accessToken, refToken, null, executionContext);

                    cibaGrant.setTokensDelivered(true);
                    cibaGrant.save();

                    RefreshToken reToken = null;
                    if (tokenCreatorService.isRefreshTokenAllowed(client, scope, cibaGrant)) {
                        reToken = refToken;
                    }

                    scope = cibaGrant.checkScopesPolicy(scope);
                    AuthzDetails checkedAuthzDetails = authzDetailsService.checkAuthzDetailsAndSave(executionContext.getAuthzDetails(), cibaGrant);

                    executionContext.getAuditLog().updateOAuth2AuditLog(cibaGrant, true);

                    return response(Response.ok().entity(getJSonResponse(accessToken,
                            accessToken.getTokenType(),
                            accessToken.getExpiresIn(),
                            reToken,
                            scope,
                            idToken,
                            checkedAuthzDetails)), executionContext.getAuditLog());
                } else {
                    return response(error(400, TokenErrorResponseType.INVALID_GRANT, "AuthReqId is no longer available."), executionContext.getAuditLog());
                }
            } else {
                log.debug("Client is not using Poll flow authReqId: '{}'", authReqId);
                return response(error(400, TokenErrorResponseType.UNAUTHORIZED_CLIENT, "The client is not authorized as it is configured in Push Mode"), executionContext.getAuditLog());
            }
        } else {
            return processCIBAIfGrantIsNull(authReqId, executionContext);
        }
    }

    private Response processCIBAIfGrantIsNull(String authReqId, ExecutionContext executionContext) {
        final CibaRequestCacheControl cibaRequest = cibaRequestService.getCibaRequest(authReqId);
        log.trace("Ciba request : '{}'", cibaRequest);
        if (cibaRequest != null) {
            if (!cibaRequest.getClient().getClientId().equals(executionContext.getClient().getClientId())) {
                return response(error(400, TokenErrorResponseType.INVALID_GRANT, REASON_CLIENT_NOT_AUTHORIZED), executionContext.getAuditLog());
            }
            long currentTime = new Date().getTime();
            Long lastAccess = cibaRequest.getLastAccessControl();
            if (lastAccess == null) {
                lastAccess = currentTime;
            }
            cibaRequest.setLastAccessControl(currentTime);
            cibaRequestService.update(cibaRequest);

            if (cibaRequest.getStatus() == CibaRequestStatus.PENDING) {
                int intervalSeconds = appConfiguration.getBackchannelAuthenticationResponseInterval();
                long timeFromLastAccess = currentTime - lastAccess;

                if (timeFromLastAccess > intervalSeconds * 1000) {
                    log.debug("Access hasn't been granted yet for authReqId: '{}'", authReqId);
                    return response(error(400, TokenErrorResponseType.AUTHORIZATION_PENDING, "User hasn't answered yet"), executionContext.getAuditLog());
                } else {
                    log.debug("Slow down protection authReqId: '{}'", authReqId);
                    return response(error(400, TokenErrorResponseType.SLOW_DOWN, "Client is asking too fast the token."), executionContext.getAuditLog());
                }
            } else if (cibaRequest.getStatus() == CibaRequestStatus.DENIED) {
                log.debug("The end-user denied the authorization request for authReqId: '{}'", authReqId);
                return response(error(400, TokenErrorResponseType.ACCESS_DENIED, "The end-user denied the authorization request."), executionContext.getAuditLog());
            } else if (cibaRequest.getStatus() == CibaRequestStatus.EXPIRED) {
                log.debug("The authentication request has expired for authReqId: '{}'", authReqId);
                return response(error(400, TokenErrorResponseType.EXPIRED_TOKEN, "The authentication request has expired"), executionContext.getAuditLog());
            }
        } else {
            log.debug("AuthorizationGrant is empty by authReqId: '{}'", authReqId);
            return response(error(400, TokenErrorResponseType.EXPIRED_TOKEN, "Unable to find grant object for given auth_req_id."), executionContext.getAuditLog());
        }

        return response(error(400, TokenErrorResponseType.EXPIRED_TOKEN, "Unable to find grant object for given auth_req_id."), executionContext.getAuditLog());
    }
}
