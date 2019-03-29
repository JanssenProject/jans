/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.common.SubjectType;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * OC5:FeatureTest-Providing ID Token with max age Restriction
 *
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class ProvidingIdTokenWithMaxAgeRestriction extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "clientJwksUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestriction(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String clientJwksUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setContacts(Arrays.asList("javier@gluu.org"));
        registerRequest.setGrantTypes(Arrays.asList(GrantType.AUTHORIZATION_CODE));
        registerRequest.setPostLogoutRedirectUris(StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setRequireAuthTime(true);
        registerRequest.setDefaultMaxAge(3600);
        registerRequest.setResponseTypes(responseTypes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        String sessionId;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());
            assertEquals(authorizationResponse.getState(), state);

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            String idToken = tokenResponse.getIdToken();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getIdToken(), "The ID Token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

            // 4. Validate id_token
            Jwt jwt = Jwt.parse(idToken);
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));

            RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                    jwksUri,
                    jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
            RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

            assertTrue(rsaSigner.validate(jwt));
        }

        Thread.sleep(60000);

        {
            // 5. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
            authorizationRequest.setState(state);
            authorizationRequest.setMaxAge(30);
            authorizationRequest.setSessionId(sessionId);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());
            assertEquals(authorizationResponse.getState(), state);

            String authorizationCode = authorizationResponse.getCode();

            // 6. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            String idToken = tokenResponse.getIdToken();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getIdToken(), "The ID Token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

            // 7. Validate id_token
            Jwt jwt = Jwt.parse(idToken);
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
            assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));

            RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                    jwksUri,
                    jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
            RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

            assertTrue(rsaSigner.validate(jwt));
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void providingIdTokenWithMaxAgeRestrictionJwtAuthorizationRequest(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Providing ID Token with max age Restriction (JWT Authorization Request)");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        String sessionId;
        {
            // 2. Request authorization
            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);

            AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                    authorizationEndpoint, authorizationRequest, userId, userSecret);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();
            sessionId = authorizationResponse.getSessionId();

            // 3. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }

        Thread.sleep(60000);

        {
            // 4. Request authorization
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

            List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
            String nonce = UUID.randomUUID().toString();
            String state = UUID.randomUUID().toString();

            AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
            authorizationRequest.setState(state);
            authorizationRequest.setSessionId(sessionId);

            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(30);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            authorizationRequest.setRequest(authJwt);

            AuthorizationResponse authorizationResponse = authenticateResourceOwner(
                    authorizationEndpoint, authorizationRequest, userId, userSecret, false);

            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getCode());
            assertNotNull(authorizationResponse.getIdToken());
            assertNotNull(authorizationResponse.getState());
            assertNotNull(authorizationResponse.getScope());

            String authorizationCode = authorizationResponse.getCode();

            // 5. Get Access Token
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(authorizationCode);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            TokenResponse tokenResponse = tokenClient.exec();

            showClient(tokenClient);
            assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
            assertNotNull(tokenResponse.getEntity(), "The entity is null");
            assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
            assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
            assertNotNull(tokenResponse.getTokenType(), "The token type is null");
            assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        }
    }
}