/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma.wrapper;

import org.xdi.oxauth.client.*;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version June 19, 2015
 */

public class UmaClient {

    @Deprecated
    public static Token requestAat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.AUTHORIZATION);
    }

    public static Token requestAat(final String tokenUrl, final String umaClientId, final String umaClientSecret, String... scopeArray) throws Exception {
        return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.AUTHORIZATION, scopeArray);
    }

    @Deprecated
    public static Token requestPat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri, String... scopeArray) throws Exception {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.PROTECTION, scopeArray);
    }

    public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret, String... scopeArray) throws Exception {
        return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.PROTECTION, scopeArray);
    }

    @Deprecated
    public static Token request(final String authorizeUrl, final String tokenUrl,
                                final String umaUserId, final String umaUserSecret,
                                final String umaClientId, final String umaClientSecret,
                                final String umaRedirectUri, UmaScopeType p_type, String... scopeArray) throws Exception {
        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);

        List<String> scopes = new ArrayList<String>();
        scopes.add(p_type.getValue());
        if (scopeArray != null && scopeArray.length > 0) {
            scopes.addAll(Arrays.asList(scopeArray));
        }

        String state = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, umaClientId, scopes, umaRedirectUri, null);
        request.setState(state);
        request.setAuthUsername(umaUserId);
        request.setAuthPassword(umaUserSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizeUrl);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        String scope = response1.getScope();
        String authorizationCode = response1.getCode();

        if (Util.allNotBlank(authorizationCode)) {

            // 2. Request access token using the authorization code.
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(umaRedirectUri);
            tokenRequest.setAuthUsername(umaClientId);
            tokenRequest.setAuthPassword(umaClientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            tokenRequest.setScope(scope);

            TokenClient tokenClient1 = new TokenClient(tokenUrl);
            tokenClient1.setRequest(tokenRequest);
            TokenResponse response2 = tokenClient1.exec();

            if (response2.getStatus() == 200) {
                final String patToken = response2.getAccessToken();
                final String patRefreshToken = response2.getRefreshToken();
                final Integer expiresIn = response2.getExpiresIn();
                if (Util.allNotBlank(patToken, patRefreshToken)) {
                    return new Token(authorizationCode, patRefreshToken, patToken, scope, expiresIn);
                }
            }
        }

        return null;
    }

    public static Token request(final String tokenUrl, final String umaClientId, final String umaClientSecret, UmaScopeType scopeType, String... scopeArray) throws Exception {

        String scope = scopeType.getValue();
        if (scopeArray != null && scopeArray.length > 0) {
            for (String s : scopeArray) {
                scope = scope + " " + s;
            }
        }

        TokenClient tokenClient = new TokenClient(tokenUrl);
        TokenResponse response = tokenClient.execClientCredentialsGrant(scope, umaClientId, umaClientSecret);

        if (response.getStatus() == 200) {
            final String patToken = response.getAccessToken();
            final Integer expiresIn = response.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
            }
        }

        return null;
    }

    public static Token requestWithClientSecretJwt(final String tokenUrl,
                                                   final String umaClientId,
                                                   final String umaClientSecret,
                                                   AuthenticationMethod authenticationMethod,
                                                   SignatureAlgorithm signatureAlgorithm,
                                                   String audience,
                                                   UmaScopeType scopeType,
                                                   String... scopeArray) throws Exception {

        String scope = scopeType.getValue();
        if (scopeArray != null && scopeArray.length > 0) {
            for (String s : scopeArray) {
                scope = scope + " " + s;
            }
        }

        TokenRequest request = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        request.setAuthUsername(umaClientId);
        request.setAuthPassword(umaClientSecret);
        request.setScope(scope);
        request.setAuthenticationMethod(authenticationMethod);
        request.setAlgorithm(signatureAlgorithm);
        request.setAudience(audience);

        return request(tokenUrl, request);
    }

    public static Token request(final String tokenUrl, final TokenRequest tokenRequest) throws Exception {
    	if (tokenRequest.getGrantType() != GrantType.CLIENT_CREDENTIALS) {
    		return null;
    	}

        TokenClient tokenClient = new TokenClient(tokenUrl);

        tokenClient.setRequest(tokenRequest);

        TokenResponse response = tokenClient.exec();

        if (response.getStatus() == 200) {
            final String patToken = response.getAccessToken();
            final Integer expiresIn = response.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, response.getScope(), expiresIn);
            }
        }

        return null;
    }

}
