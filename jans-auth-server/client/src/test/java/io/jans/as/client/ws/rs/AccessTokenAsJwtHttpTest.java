/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;



import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class AccessTokenAsJwtHttpTest extends BaseTest {

    /**
     * Test for the complete Authorization Code Flow.
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void accessTokenAsJwt(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("accessTokenAsJwt");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN, ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        RegisterResponse registerResponse = registerClient(redirectUri, responseTypes, scopes);

        String clientId = registerResponse.getClientId();

        // Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String accessToken = authorizationResponse.getAccessToken();

        // Validate access token as jwt
        Jwt jwt = Jwt.parse(accessToken);
        assertEquals(clientId, jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString("scope"));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
    }

    private RegisterResponse registerClient(String redirectUris, List<ResponseType> responseTypes, List<String> scopes) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "access token as JWT test", StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(scopes);
        registerRequest.setAccessTokenAsJwt(true);
        registerRequest.setAccessTokenSigningAlg(SignatureAlgorithm.RS512);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientEngine(true));
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        return registerResponse;
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, List<String> scopes, String clientId, String nonce) {
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).check();
        return authorizationResponse;
    }
}
