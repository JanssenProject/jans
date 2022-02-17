/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.JwkClient;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.par.ParResponse;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;

import static io.jans.as.client.BaseTest.clientEngine;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version February 11, 2022
 */

public class Asserter {

    private Asserter() {
    }

    public static void assertParResponse(ParResponse response) {
        assertNotNull(response);
        assertTrue(StringUtils.isNotBlank(response.getRequestUri()));
        assertNotNull(response.getExpiresIn());
    }

    public static void assertOk(RegisterResponse response) {
        assertEquals(response.getStatus(), 201, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getRegistrationClientUri());
    }

    public static void assertBadRequest(RegisterResponse registerResponse) {
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getEntity(), "The entity is null");
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }

    public static void assertTokenResponse(TokenResponse response) {
        assertNotNull(response);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        assertNotNull(response.getRefreshToken(), "The refresh token is null");
    }

    public static void assertAuthorizationResponse(AuthorizationResponse response) {
        assertAuthorizationResponse(response, true);
    }

    public static void assertAuthorizationResponse(AuthorizationResponse response, boolean checkState) {
        assertNotNull(response);
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getCode(), "The authorization code is null");
        assertNotNull(response.getScope(), "The scope is null");
        if (checkState) {
            assertNotNull(response.getState(), "The state is null");
        }
    }

    public static void assertAuthorizationResponse(AuthorizationResponse authorizationResponse, List<ResponseType> responseTypes, boolean checkState) {
        assertNotNull(authorizationResponse);
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getScope(), "The scope is null");
        if (checkState) {
            assertNotNull(authorizationResponse.getState(), "The state is null");
        }
        if (responseTypes.contains(ResponseType.CODE)) {
            assertNotNull(authorizationResponse.getCode(), "The authorization code is null");
        }
        if (responseTypes.contains(ResponseType.TOKEN)) {
            assertNotNull(authorizationResponse.getAccessToken(), "The access_token is null");
            assertNotNull(authorizationResponse.getTokenType());
            assertNotNull(authorizationResponse.getExpiresIn());
        }
        if (responseTypes.contains(ResponseType.ID_TOKEN)) {
            assertNotNull(authorizationResponse.getIdToken(), "The id_token is null");
        }
    }

    public static void validateIdToken(String idToken, String jwksUri, SignatureAlgorithm alg) throws InvalidJwtException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Jwt jwt = Jwt.parse(idToken);
        Asserter.assertIdToken(jwt, JwtClaimName.CODE_HASH);

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwksUri, jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID), clientEngine(true));
        RSASigner rsaSigner = new RSASigner(alg, publicKey);
        assertTrue(rsaSigner.validate(jwt));
    }

    public static void assertIdToken(Jwt idToken, String... claimsPresence) {
        assertNotNull(idToken);
        assertNotNull(idToken.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(idToken.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.OX_OPENID_CONNECT_VERSION));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE));
        assertNotNull(idToken.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES));

        if (claimsPresence == null) {
            return;
        }

        for (String claim : claimsPresence) {
            assertNotNull(claim, "Claim " + claim + " is not found in id_token. ");
        }
    }
}
