/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.token.ws.rs;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.authorize.CodeVerifier;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.binding.TokenBindingMessage;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.session.SessionClient;
import org.gluu.oxauth.model.token.JsonWebResponse;
import org.gluu.oxauth.model.token.TokenErrorResponseType;
import org.gluu.oxauth.model.token.TokenParamsValidator;
import org.gluu.oxauth.security.Identity;
import org.gluu.oxauth.service.*;
import org.gluu.oxauth.service.external.ExternalResourceOwnerPasswordCredentialsService;
import org.gluu.oxauth.service.external.context.ExternalResourceOwnerPasswordCredentialsContext;
import org.gluu.oxauth.uma.service.UmaTokenService;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.service.UserService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;

/**
 * Provides interface for token REST web services
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 14, 2019
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

    @Override
    public Response requestAccessToken(String grantType, String code,
                                       String redirectUri, String username, String password, String scope,
                                       String assertion, String refreshToken,
                                       String clientId, String clientSecret, String codeVerifier,
                                       String ticket, String claimToken, String claimTokenFormat, String pctCode, String rptCode,
                                       HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.debug(
                "Attempting to request access token: grantType = {}, code = {}, redirectUri = {}, username = {}, refreshToken = {}, " +
                        "clientId = {}, ExtraParams = {}, isSecure = {}, codeVerifier = {}, ticket = {}",
                grantType, code, redirectUri, username, refreshToken, clientId, request.getParameterMap(),
                sec.isSecure(), codeVerifier, ticket);

        boolean isUma = StringUtils.isNotBlank(ticket);
        if (isUma) {
            return umaTokenService.requestRpt(grantType, ticket, claimToken, claimTokenFormat, pctCode, rptCode, scope, request);
        }

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(request), Action.TOKEN_REQUEST);
        oAuth2AuditLog.setClientId(clientId);
        oAuth2AuditLog.setUsername(username);
        oAuth2AuditLog.setScope(scope);

        String tokenBindingHeader = request.getHeader("Sec-Token-Binding");

        scope = ServerUtil.urlDecode(scope); // it may be encoded in uma case
        ResponseBuilder builder = Response.ok();

        try {
            log.debug("Starting to validate request parameters");
            if (!TokenParamsValidator.validateParams(grantType, code, redirectUri, username, password,
                    scope, assertion, refreshToken)) {
                log.trace("Failed to validate request parameters");
                builder = error(400, TokenErrorResponseType.INVALID_REQUEST);
            } else {
                log.trace("Request parameters are right");
                GrantType gt = GrantType.fromString(grantType);
                log.debug("Grant type: '{}'", gt);

                SessionClient sessionClient = identity.getSessionClient();
                Client client = null;
                if (sessionClient != null) {
                    client = sessionClient.getClient();
                    log.debug("Get sessionClient: '{}'", sessionClient);
                }

                if (client != null) {
                    log.debug("Get client from session: '{}'", client.getClientId());
                    if (client.isDisabled()) {
                        return response(error(Response.Status.FORBIDDEN.getStatusCode(), TokenErrorResponseType.DISABLED_CLIENT), oAuth2AuditLog);
                    }
                } else {
                    return response(error(401, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog);
                }

                final Function<JsonWebResponse, Void> idTokenTokingBindingPreprocessing = TokenBindingMessage.createIdTokenTokingBindingPreprocessing(
                        tokenBindingHeader, client.getIdTokenTokenBindingCnf()); // for all except authorization code grant

                if (gt == GrantType.AUTHORIZATION_CODE) {
                    if (!TokenParamsValidator.validateGrantType(gt, client.getGrantTypes(), appConfiguration.getGrantTypesSupported())) {
                        return response(error(400, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog);
                    }

                    log.debug("Attempting to find authorizationCodeGrant by clinetId: '{}', code: '{}'", client.getClientId(), code);
                    final AuthorizationCodeGrant authorizationCodeGrant = authorizationGrantList.getAuthorizationCodeGrant(client.getClientId(), code);
                    log.trace("AuthorizationCodeGrant : '{}'", authorizationCodeGrant);

                    if (authorizationCodeGrant != null) {
                        validatePKCE(authorizationCodeGrant, codeVerifier, oAuth2AuditLog);

                        authorizationCodeGrant.setIsCachedWithNoPersistence(false);
                        authorizationCodeGrant.save();

                        RefreshToken reToken = null;
                        if (client.getGrantTypes() != null
                                && client.getGrantTypes().length > 0
                                && Arrays.asList(client.getGrantTypes()).contains(GrantType.REFRESH_TOKEN)) {
                            reToken = authorizationCodeGrant.createRefreshToken();
                        }

                        if (scope != null && !scope.isEmpty()) {
                            scope = authorizationCodeGrant.checkScopesPolicy(scope);
                        }

                        AccessToken accToken = authorizationCodeGrant.createAccessToken(request.getHeader("X-ClientCert")); // create token after scopes are checked
                        log.debug("Issuing access token: {}", accToken.getCode());

                        IdToken idToken = null;
                        if (authorizationCodeGrant.getScopes().contains("openid")) {
                            String nonce = authorizationCodeGrant.getNonce();
                            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                                    appConfiguration.getLegacyIdTokenClaims());
                            final String idTokenTokenBindingCnf = client.getIdTokenTokenBindingCnf();
                            Function<JsonWebResponse, Void> authorizationCodePreProcessing = new Function<JsonWebResponse, Void>() {
                                @Override
                                public Void apply(JsonWebResponse jsonWebResponse) {
                                    if (StringUtils.isNotBlank(idTokenTokenBindingCnf) && StringUtils.isNotBlank(authorizationCodeGrant.getTokenBindingHash())) {
                                        TokenBindingMessage.setCnfClaim(jsonWebResponse, authorizationCodeGrant.getTokenBindingHash(), idTokenTokenBindingCnf);
                                    }
                                    return null;
                                }
                            };
                            idToken = authorizationCodeGrant.createIdToken(
                                    nonce, null, accToken, null, authorizationCodeGrant, includeIdTokenClaims, authorizationCodePreProcessing);
                        }

                        builder.entity(getJSonResponse(accToken,
                                accToken.getTokenType(),
                                accToken.getExpiresIn(),
                                reToken,
                                scope,
                                idToken));

                        oAuth2AuditLog.updateOAuth2AuditLog(authorizationCodeGrant, true);

                        grantService.removeByCode(authorizationCodeGrant.getAuthorizationCode().getCode(), authorizationCodeGrant.getClientId());
                    } else {
                        log.debug("AuthorizationCodeGrant is empty by clinetId: '{}', code: '{}'", client.getClientId(), code);
                        // if authorization code is not found then code was already used = remove all grants with this auth code
                        grantService.removeAllByAuthorizationCode(code);
                        builder = error(400, TokenErrorResponseType.INVALID_GRANT);
                    }
                } else if (gt == GrantType.REFRESH_TOKEN) {
                    if (!TokenParamsValidator.validateGrantType(gt, client.getGrantTypes(), appConfiguration.getGrantTypesSupported())) {
                        return response(error(400, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog);
                    }

                    AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), refreshToken);

                    if (authorizationGrant != null) {


                        /*
                        The authorization server MAY issue a new refresh token, in which case
                        the client MUST discard the old refresh token and replace it with the
                        new refresh token.
                        */
                        RefreshToken reToken = authorizationGrant.createRefreshToken();
                        grantService.removeByCode(refreshToken, client.getClientId());

                        if (scope != null && !scope.isEmpty()) {
                            scope = authorizationGrant.checkScopesPolicy(scope);
                        }

                        AccessToken accToken = authorizationGrant.createAccessToken(request.getHeader("X-ClientCert")); // create token after scopes are checked

                        builder.entity(getJSonResponse(accToken,
                                accToken.getTokenType(),
                                accToken.getExpiresIn(),
                                reToken,
                                scope,
                                null));
                        oAuth2AuditLog.updateOAuth2AuditLog(authorizationGrant, true);
                    } else {
                        builder = error(401, TokenErrorResponseType.INVALID_GRANT);
                    }
                } else if (gt == GrantType.CLIENT_CREDENTIALS) {
                    if (!TokenParamsValidator.validateGrantType(gt, client.getGrantTypes(), appConfiguration.getGrantTypesSupported())) {
                        return response(error(400, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog);
                    }

                    ClientCredentialsGrant clientCredentialsGrant = authorizationGrantList.createClientCredentialsGrant(new User(), client); // TODO: fix the user arg

                    if (scope != null && !scope.isEmpty()) {
                        scope = clientCredentialsGrant.checkScopesPolicy(scope);
                    }

                    AccessToken accessToken = clientCredentialsGrant.createAccessToken(request.getHeader("X-ClientCert")); // create token after scopes are checked

                    IdToken idToken = null;
                    if (appConfiguration.getOpenidScopeBackwardCompatibility() && clientCredentialsGrant.getScopes().contains("openid")) {
                        boolean includeIdTokenClaims = Boolean.TRUE.equals(
                                appConfiguration.getLegacyIdTokenClaims());
                        idToken = clientCredentialsGrant.createIdToken(
                                null, null, null, null, clientCredentialsGrant, includeIdTokenClaims, idTokenTokingBindingPreprocessing);
                    }

                    oAuth2AuditLog.updateOAuth2AuditLog(clientCredentialsGrant, true);
                    builder.entity(getJSonResponse(accessToken,
                            accessToken.getTokenType(),
                            accessToken.getExpiresIn(),
                            null,
                            scope,
                            idToken));
                } else if (gt == GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS) {
                    if (!TokenParamsValidator.validateGrantType(gt, client.getGrantTypes(), appConfiguration.getGrantTypesSupported())) {
                        return response(error(400, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog);
                    }

                    User user = null;
                    if (authenticationFilterService.isEnabled()) {
                        String userDn = authenticationFilterService.processAuthenticationFilters(request.getParameterMap());
                        if (StringHelper.isNotEmpty(userDn)) {
                            user = userService.getUserByDn(userDn);
                        }
                    }

                    if (user == null) {
                        boolean authenticated = authenticationService.authenticate(username, password);
                        if (authenticated) {
                            user = authenticationService.getAuthenticatedUser();
                        }
                    }

                    boolean authenticatedByRoScript = false;
                    if (externalResourceOwnerPasswordCredentialsService.getCustomScriptConfigurations() != null && !externalResourceOwnerPasswordCredentialsService.getCustomScriptConfigurations().isEmpty()) {
                        final ExternalResourceOwnerPasswordCredentialsContext context = new ExternalResourceOwnerPasswordCredentialsContext(request, response, appConfiguration, attributeService, userService);
                        context.setUser(user);
                        if (externalResourceOwnerPasswordCredentialsService.executeExternalAuthenticate(context)) {
                            log.trace("RO PC - User is authenticated successfully by external script.");
                            authenticatedByRoScript = true;
                            user = context.getUser();
                        }
                    }

                    if (user != null || authenticatedByRoScript) {
                        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant = authorizationGrantList.createResourceOwnerPasswordCredentialsGrant(user, client);

                        RefreshToken reToken = null;
                        if (client.getGrantTypes() != null
                                && client.getGrantTypes().length > 0
                                && Arrays.asList(client.getGrantTypes()).contains(GrantType.REFRESH_TOKEN)) {
                            reToken = resourceOwnerPasswordCredentialsGrant.createRefreshToken();
                        }

                        if (scope != null && !scope.isEmpty()) {
                            scope = resourceOwnerPasswordCredentialsGrant.checkScopesPolicy(scope);
                        }

                        AccessToken accessToken = resourceOwnerPasswordCredentialsGrant.createAccessToken(request.getHeader("X-ClientCert")); // create token after scopes are checked

                        IdToken idToken = null;
                        if (appConfiguration.getOpenidScopeBackwardCompatibility() && resourceOwnerPasswordCredentialsGrant.getScopes().contains("openid")) {
                            boolean includeIdTokenClaims = Boolean.TRUE.equals(
                                    appConfiguration.getLegacyIdTokenClaims());
                            idToken = resourceOwnerPasswordCredentialsGrant.createIdToken(
                                    null, null, null, null, resourceOwnerPasswordCredentialsGrant, includeIdTokenClaims, idTokenTokingBindingPreprocessing);
                        }

                        oAuth2AuditLog.updateOAuth2AuditLog(resourceOwnerPasswordCredentialsGrant, true);
                        builder.entity(getJSonResponse(accessToken,
                                accessToken.getTokenType(),
                                accessToken.getExpiresIn(),
                                reToken,
                                scope,
                                idToken));
                    } else {
                        log.error("Invalid user", new RuntimeException("User is empty"));
                        builder = error(401, TokenErrorResponseType.INVALID_CLIENT);
                    }
                }
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        }

        return response(builder, oAuth2AuditLog);
    }

    private void validatePKCE(AuthorizationCodeGrant grant, String codeVerifier, OAuth2AuditLog oAuth2AuditLog) {
        log.trace("PKCE validation, code_verifier: {}, code_challenge: {}, method: {}",
                codeVerifier, grant.getCodeChallenge(), grant.getCodeChallengeMethod());

        if (Strings.isNullOrEmpty(grant.getCodeChallenge()) && Strings.isNullOrEmpty(codeVerifier)) {
            return; // if no code challenge then it's valid, no PKCE check
        }

        if (!CodeVerifier.matched(grant.getCodeChallenge(), grant.getCodeChallengeMethod(), codeVerifier)) {
            log.error("PKCE check fails. Code challenge does not match to request code verifier, " +
                    "grantId:" + grant.getGrantId() + ", codeVerifier: " + codeVerifier);
            throw new WebApplicationException(response(error(401, TokenErrorResponseType.INVALID_GRANT), oAuth2AuditLog));
        }
    }

    private Response response(ResponseBuilder builder, OAuth2AuditLog oAuth2AuditLog) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");

        applicationAuditLogger.sendMessage(oAuth2AuditLog);

        return builder.build();
    }

    private ResponseBuilder error(int p_status, TokenErrorResponseType p_type) {
        return Response.status(p_status).entity(errorResponseFactory.getErrorAsJson(p_type));
    }

    /**
     * Builds a JSon String with the structure for token issues.
     */
    public String getJSonResponse(AccessToken accessToken, TokenType tokenType,
                                  Integer expiresIn, RefreshToken refreshToken, String scope,
                                  IdToken idToken) {
        JSONObject jsonObj = new JSONObject();
        try {
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
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }
}