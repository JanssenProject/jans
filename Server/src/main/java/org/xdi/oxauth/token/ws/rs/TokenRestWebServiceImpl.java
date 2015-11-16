/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.token.ws.rs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.annotations.common.util.StringHelper;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidJweException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.OAuthCredentials;
import org.xdi.oxauth.model.session.SessionClient;
import org.xdi.oxauth.model.token.PersistentJwt;
import org.xdi.oxauth.model.token.TokenErrorResponseType;
import org.xdi.oxauth.model.token.TokenParamsValidator;
import org.xdi.oxauth.service.*;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.security.StringEncrypter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides interface for token REST web services
 *
 * @author Javier Rojas Blum
 * @version November 16, 2015
 */
@Name("requestTokenRestWebService")
public class TokenRestWebServiceImpl implements TokenRestWebService {

    @Logger
    private Log log;

    @In
    private OAuthCredentials credentials;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @In
    private SessionClient sessionClient;

    @In
    private UserService userService;

    @In
    private AuthenticationFilterService authenticationFilterService;

    @In
    private FederationDataService federationDataService;

    @In
    private AuthenticationService authenticationService;

    @Override
    public Response requestAccessToken(String grantType, String code,
                                       String redirectUri, String username, String password, String scope,
                                       String assertion, String refreshToken, String oxAuthExchangeToken,
                                       String clientId, String clientSecret,
                                       HttpServletRequest request, SecurityContext sec) {
        log.debug(
                "Attempting to request access token: grantType = {0}, code = {1}, redirectUri = {2}, username = {3}, refreshToken = {4}, clientId = {5}, ExtraParams = {6}, isSecure = {7}",
                grantType, code, redirectUri, username, refreshToken, clientId, request.getParameterMap(), sec.isSecure());
        final Mode serverMode = ConfigurationFactory.instance().getConfiguration().getModeEnum();
        scope = ServerUtil.urlDecode(scope); // it may be encoded in uma case
        ResponseBuilder builder = Response.ok();

        try {
            if (!TokenParamsValidator.validateParams(grantType, code, redirectUri, username, password,
                    scope, assertion, refreshToken, oxAuthExchangeToken)) {
                builder = error(400, TokenErrorResponseType.INVALID_REQUEST);
            } else {
                GrantType gt = GrantType.fromString(grantType);

                Client client = sessionClient.getClient();

                if (ConfigurationFactory.instance().getConfiguration().getFederationEnabled()) {
                    if (!federationDataService.hasAnyActiveTrust(client)) {
                        log.debug("Forbid token issuing. Client is not in any trust relationship however federation is enabled for server. Client id: {0}, redirectUris: {1}",
                                client.getClientId(), client.getRedirectUris());
                        return error(400, TokenErrorResponseType.UNAUTHORIZED_CLIENT).build();
                    }
                }

                if (gt == GrantType.AUTHORIZATION_CODE) {
    				if (client == null) {
    					return sendResponse(error(400, TokenErrorResponseType.INVALID_GRANT));
    				}

    				GrantService grantService = GrantService.instance();
                    AuthorizationCodeGrant authorizationCodeGrant = authorizationGrantList.getAuthorizationCodeGrant(client.getClientId(), code);

                    if (authorizationCodeGrant != null) {
                        AccessToken accToken = authorizationCodeGrant.createAccessToken();
                        log.debug("Issuing access token: {0}", accToken.getCode());

                        RefreshToken reToken = authorizationCodeGrant.createRefreshToken();

                        if (scope != null && !scope.isEmpty()) {
                            scope = authorizationCodeGrant.checkScopesPolicy(scope);
                        }

                        IdToken idToken = null;
                        if (authorizationCodeGrant.getScopes().contains("openid")) {
                            String nonce = authorizationCodeGrant.getNonce();
                            idToken = authorizationCodeGrant.createIdToken(
                                    nonce, null, accToken, authorizationCodeGrant.getAcrValues());
                        }

                        builder.entity(getJSonResponse(accToken,
                                accToken.getTokenType(),
                                accToken.getExpiresIn(),
                                reToken,
                                scope,
                                idToken));

                        switch (serverMode) {
                            case IN_MEMORY:
                                authorizationCodeGrant.getAuthorizationCode().setUsed(true);
                                break;
                            case LDAP:
                                grantService.removeByCode(authorizationCodeGrant.getAuthorizationCode().getCode(), authorizationCodeGrant.getClientId());
                                break;
                        }
                    } else {
                        // if authorization code is not found and mode is LDAP then code was already used = remove all grants with this auth code
                        if (serverMode == Mode.LDAP) {
                            grantService.removeAllByAuthorizationCode(code);
                        }
                        builder = error(400, TokenErrorResponseType.INVALID_GRANT);
                    }
                } else if (gt == GrantType.REFRESH_TOKEN) {
    				if (client == null) {
    					return sendResponse(error(401, TokenErrorResponseType.INVALID_GRANT));
    				}

                    AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(client.getClientId(), refreshToken);

                    if (authorizationGrant != null) {
                        AccessToken accToken = authorizationGrant.createAccessToken();
                        RefreshToken reToken = authorizationGrant.createRefreshToken();

                        if (scope != null && !scope.isEmpty()) {
                            scope = authorizationGrant.checkScopesPolicy(scope);
                        }

                        IdToken idToken = null;
                        if (authorizationGrant.getScopes().contains("openid")) {
                            idToken = authorizationGrant.createIdToken(
                                    null, null, null,
                                    authorizationGrant.getAcrValues());
                        }

                        builder.entity(getJSonResponse(accToken,
                                accToken.getTokenType(),
                                accToken.getExpiresIn(),
                                reToken,
                                scope,
                                idToken));
                    } else {
                        builder = error(401, TokenErrorResponseType.INVALID_GRANT);
                    }
                } else if (gt == GrantType.CLIENT_CREDENTIALS) {
    				if (client == null) {
    					return sendResponse(error(401, TokenErrorResponseType.INVALID_GRANT));
    				}

                    ClientCredentialsGrant clientCredentialsGrant = authorizationGrantList.createClientCredentialsGrant(new User(), client); // TODO: fix the user arg

                    AccessToken accessToken = clientCredentialsGrant.createAccessToken();

                    if (scope != null && !scope.isEmpty()) {
                        scope = clientCredentialsGrant.checkScopesPolicy(scope);
                    }

                    IdToken idToken = null;
                    if (clientCredentialsGrant.getScopes().contains("openid")) {
                        idToken = clientCredentialsGrant.createIdToken(
                                null, null, null, clientCredentialsGrant.getAcrValues());
                    }

                    builder.entity(getJSonResponse(accessToken,
                            accessToken.getTokenType(),
                            accessToken.getExpiresIn(),
                            null,
                            scope,
                            idToken));
                } else if (gt == GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS) {
    				if (client == null) {
    					return sendResponse(error(401, TokenErrorResponseType.INVALID_CLIENT));
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
                            user = credentials.getUser();
                        }
                    }

                    if (user != null) {
                        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant = authorizationGrantList.createResourceOwnerPasswordCredentialsGrant(user, client);
                        AccessToken accessToken = resourceOwnerPasswordCredentialsGrant.createAccessToken();
                        RefreshToken reToken = resourceOwnerPasswordCredentialsGrant.createRefreshToken();

                        if (scope != null && !scope.isEmpty()) {
                            scope = resourceOwnerPasswordCredentialsGrant.checkScopesPolicy(scope);
                        }

                        IdToken idToken = null;
                        if (resourceOwnerPasswordCredentialsGrant.getScopes().contains("openid")) {
                            idToken = resourceOwnerPasswordCredentialsGrant.createIdToken(
                                    null, null, null, resourceOwnerPasswordCredentialsGrant.getAcrValues());
                        }

                        builder.entity(getJSonResponse(accessToken,
                                accessToken.getTokenType(),
                                accessToken.getExpiresIn(),
                                reToken,
                                scope,
                                idToken));
                    } else {
                        builder = error(401, TokenErrorResponseType.INVALID_CLIENT);
                    }
                } else if (gt == GrantType.EXTENSION) {
                    builder = error(501, TokenErrorResponseType.INVALID_GRANT);
                } else if (gt == GrantType.OXAUTH_EXCHANGE_TOKEN) {
                    AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(oxAuthExchangeToken);

                    if (authorizationGrant != null) {
                        final AccessToken accessToken = authorizationGrant.createLongLivedAccessToken();
                        final List<String> scopes = new ArrayList<String>();
                        if (authorizationGrant.getScopes() != null) {
                            scopes.addAll(authorizationGrant.getScopes());
                        }


                        PersistentJwt persistentJwt = new PersistentJwt();
                        persistentJwt.setUserId(authorizationGrant.getUserId());
                        persistentJwt.setClientId(authorizationGrant.getClient().getClientId());
                        persistentJwt.setAuthorizationGrantType(authorizationGrant.getAuthorizationGrantType());
                        persistentJwt.setAuthenticationTime(authorizationGrant.getAuthenticationTime());
                        persistentJwt.setScopes(scopes);
                        persistentJwt.setAccessTokens(authorizationGrant.getAccessTokens());
                        persistentJwt.setRefreshTokens(authorizationGrant.getRefreshTokens());
                        persistentJwt.setLongLivedAccessToken(authorizationGrant.getLongLivedAccessToken());
                        persistentJwt.setIdToken(authorizationGrant.getIdToken());

                        if (ConfigurationFactory.instance().getConfiguration().getModeEnum() == Mode.IN_MEMORY) {
                            userService.saveLongLivedToken(authorizationGrant.getUserId(), persistentJwt);
                        }

                        builder.entity(getJSonResponse(accessToken,
                                accessToken.getTokenType(),
                                accessToken.getExpiresIn(),
                                null, null, null));
                    } else {
                        builder = error(401, TokenErrorResponseType.INVALID_GRANT);
                    }
                }
            }
        } catch (SignatureException e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        } catch (StringEncrypter.EncryptionException e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        } catch (InvalidJwtException e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        } catch (InvalidJweException e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            builder = Response.status(500);
            log.error(e.getMessage(), e);
        }

        return sendResponse(builder);
    }

	private Response sendResponse(ResponseBuilder builder) {
		CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");
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