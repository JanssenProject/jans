/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.uma.wrapper;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.uma.exception.UmaException;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import io.jans.util.StringHelper;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 */

public class UmaClient {

    private UmaClient() {
    }

    public static Token requestPat(final String tokenUrl, final String clientKeyStoreFile, final String clientKeyStorePassword,
                                   final String clientId, final String keyId) throws UmaException {
        TokenRequest tokenRequest = TokenRequest.builder().pat().grantType(GrantType.CLIENT_CREDENTIALS).build();

        return request(tokenUrl, clientKeyStoreFile, clientKeyStorePassword, clientId, keyId, tokenRequest);
    }


    /**
     * @deprecated use request() method directly
     */
    @SuppressWarnings("java:S107")
    @Deprecated
    public static Token requestPat(final String authorizeUrl, final String tokenUrl,
                                   final String umaUserId, final String umaUserSecret,
                                   final String umaClientId, final String umaClientSecret,
                                   final String umaRedirectUri, String... scopeArray) {
        return request(authorizeUrl, tokenUrl, umaUserId, umaUserSecret, umaClientId, umaClientSecret, umaRedirectUri, UmaScopeType.PROTECTION, scopeArray);
    }

    public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret, String... scopeArray) {
        return requestPat(tokenUrl, umaClientId, umaClientSecret, null, scopeArray);
    }

    @SuppressWarnings("java:S1874")
    public static Token requestPat(final String tokenUrl, final String umaClientId, final String umaClientSecret, ClientHttpEngine engine, String... scopeArray) {
        return request(tokenUrl, umaClientId, umaClientSecret, UmaScopeType.PROTECTION, engine, scopeArray);
    }

    /**
     * @deprecated use request() method directly
     */
    @SuppressWarnings("java:S107")
    @Deprecated
    public static Token request(final String authorizeUrl, final String tokenUrl,
                                final String umaUserId, final String umaUserSecret,
                                final String umaClientId, final String umaClientSecret,
                                final String umaRedirectUri, UmaScopeType type, String... scopeArray) {
        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = new ArrayList<>();
        responseTypes.add(ResponseType.CODE);
        responseTypes.add(ResponseType.ID_TOKEN);

        List<String> scopes = new ArrayList<>();
        scopes.add(type.getValue());
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

    @SuppressWarnings("java:S1874")
    public static Token request(final String tokenUrl, final String umaClientId, final String umaClientSecret, UmaScopeType scopeType,
                                ClientHttpEngine engine, String... scopeArray) {

        StringBuilder scope = new StringBuilder(scopeType.getValue());
        if (scopeArray != null && scopeArray.length > 0) {
            for (String s : scopeArray) {
                scope.append(" ").append(s);
            }
        }

        TokenClient tokenClient = new TokenClient(tokenUrl);
        if (engine != null) {
            tokenClient.setExecutor(engine);
        }
        TokenResponse response = tokenClient.execClientCredentialsGrant(scope.toString(), umaClientId, umaClientSecret);

        if (response.getStatus() == 200) {
            final String patToken = response.getAccessToken();
            final Integer expiresIn = response.getExpiresIn();
            if (Util.allNotBlank(patToken)) {
                return new Token(null, null, patToken, scopeType.getValue(), expiresIn);
            }
        }

        return null;
    }

    @SuppressWarnings("java:S107")
    public static Token requestWithClientSecretJwt(final String tokenUrl,
                                                   final String umaClientId,
                                                   final String umaClientSecret,
                                                   AuthenticationMethod authenticationMethod,
                                                   SignatureAlgorithm signatureAlgorithm,
                                                   String audience,
                                                   UmaScopeType scopeType,
                                                   String... scopeArray) {

        StringBuilder scope = new StringBuilder(scopeType.getValue());
        if (scopeArray != null && scopeArray.length > 0) {
            for (String s : scopeArray) {
                scope.append(" ").append(s);
            }
        }

        TokenRequest request = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        request.setAuthUsername(umaClientId);
        request.setAuthPassword(umaClientSecret);
        request.setScope(scope.toString());
        request.setAuthenticationMethod(authenticationMethod);
        request.setAlgorithm(signatureAlgorithm);
        request.setAudience(audience);

        return request(tokenUrl, request);
    }

    public static Token request(final String tokenUrl, final TokenRequest tokenRequest) {
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

    private static Token request(final String tokenUrl, final String clientKeyStoreFile,
                                 final String clientKeyStorePassword, final String clientId, final String keyId, TokenRequest tokenRequest)
            throws UmaException {
        AuthCryptoProvider cryptoProvider;
        try {
            cryptoProvider = new AuthCryptoProvider(clientKeyStoreFile, clientKeyStorePassword, null);
        } catch (Exception ex) {
            throw new UmaException("Failed to initialize crypto provider");
        }

        try {
            String tmpKeyId = keyId;
            if (StringHelper.isEmpty(keyId)) {
                // Get first key
            	tmpKeyId = cryptoProvider.getKeys().stream().filter(k -> k.contains("_sig_")).findFirst().orElse(null);

                if (StringHelper.isEmpty(tmpKeyId)) {
                    throw new UmaException("Unable to find a key in the keystore with use = sig");
                }
            } else if (keyId.contains("_enc_")) {
                throw new UmaException("Encryption keys not allowed. Supply a key having use = sig");
            }

            SignatureAlgorithm algorithm = cryptoProvider.getSignatureAlgorithm(tmpKeyId);


            tokenRequest.setAuthenticationMethod(AuthenticationMethod.PRIVATE_KEY_JWT);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setCryptoProvider(cryptoProvider);
            tokenRequest.setAlgorithm(algorithm);
            tokenRequest.setKeyId(tmpKeyId);
            tokenRequest.setAudience(tokenUrl);

            return UmaClient.request(tokenUrl, tokenRequest);
        } catch (Exception ex) {
            throw new UmaException("Failed to obtain valid UMA PAT token", ex);
        }
    }

}
