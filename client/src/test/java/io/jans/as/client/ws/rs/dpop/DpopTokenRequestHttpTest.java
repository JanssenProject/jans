/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.dpop;

import io.jans.as.client.*;
import io.jans.as.client.service.ClientFactory;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.*;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.EllipticEdvardsCurve;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.KeyType;
import io.jans.as.model.jwt.DPoP;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import sun.security.ec.ECPublicKeyImpl;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class DpopTokenRequestHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "ES256_keyId", "dnName", "keyStoreFile", "keyStoreSecret"})
    @Test
    public void claimsRequestWithEssentialNameClaim(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String keyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret) throws Exception {
        showTitle("claimsRequestWithEssentialNameClaim");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setAccessTokenAsJwt(true);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 201, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scope = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scope, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());

        String authorizationCode = authorizationResponse.getCode();

        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        ECPublicKeyImpl publicKey = (ECPublicKeyImpl) cryptoProvider.getPublicKey(keyId);

        JSONWebKey jsonWebKey = new JSONWebKey();
        jsonWebKey.setKty(KeyType.EC);
        jsonWebKey.setX(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineX()));
        jsonWebKey.setY(Base64Util.base64urlencodeUnsignedBigInt(publicKey.getW().getAffineY()));
        jsonWebKey.setCrv(EllipticEdvardsCurve.P_256);
        String jwkThumbprint = jsonWebKey.getJwkThumbprint();

        String jti1 = DPoP.generateJti();
        DPoP dpop1 = new DPoP(AsymmetricSignatureAlgorithm.ES256, jsonWebKey, jti1, HttpMethod.POST,
                tokenEndpoint, keyId, cryptoProvider);

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.NONE);
        tokenRequest.setDpop(dpop1);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertEquals(tokenResponse.getTokenType(), TokenType.DPOP);

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // 4. JWK Thumbprint Confirmation Method
        Jwt accessTokenJwt = Jwt.parse(accessToken);
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.ISSUER));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.NOT_BEFORE));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.EXPIRATION_TIME));
        assertTrue(accessTokenJwt.getClaims().hasClaim(JwtClaimName.CNF));
        assertTrue(accessTokenJwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).has(JwtClaimName.JKT));
        assertEquals(accessTokenJwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).get(JwtClaimName.JKT), jwkThumbprint);

        // 5. JWK Thumbprint Confirmation Method in Token Introspection
        IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(introspectionEndpoint, clientExecutor(true));
        String jwtAsString = introspectionService.introspectTokenWithResponseAsJwt("Bearer " + accessToken, accessToken, true);

        Jwt jwt = Jwt.parse(jwtAsString);
        assertTrue(Boolean.parseBoolean(jwt.getClaims().getClaimAsString("active")));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.ISSUER));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.NOT_BEFORE));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.EXPIRATION_TIME));
        assertTrue(jwt.getClaims().hasClaim(JwtClaimName.CNF));
        assertTrue(jwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).has(JwtClaimName.JKT));
        assertEquals(jwt.getClaims().getClaimAsJSON(JwtClaimName.CNF).get(JwtClaimName.JKT), jwkThumbprint);

        // 5. Request new access token using the refresh token.
        String accessTokenHash = Base64Util.base64urlencode(JwtUtil.getMessageDigestSHA256(accessToken));
        String jti2 = DPoP.generateJti();
        DPoP dpop2 = new DPoP(AsymmetricSignatureAlgorithm.ES256, jsonWebKey, jti2, HttpMethod.POST,
                tokenEndpoint, accessTokenHash, keyId, cryptoProvider);

        TokenRequest tokenRequest2 = new TokenRequest(GrantType.REFRESH_TOKEN);
        tokenRequest2.setRefreshToken(refreshToken);
        tokenRequest2.setAuthenticationMethod(AuthenticationMethod.NONE);
        tokenRequest2.setDpop(dpop2);

        TokenClient tokenClient2 = new TokenClient(tokenEndpoint);
        tokenClient2.setRequest(tokenRequest2);
        TokenResponse tokenResponse2 = tokenClient2.exec();

        showClient(tokenClient2);
        assertEquals(tokenResponse2.getStatus(), 200, "Unexpected response code: " + tokenResponse2.getStatus());
        assertNotNull(tokenResponse2.getEntity(), "The entity is null");
        assertNotNull(tokenResponse2.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse2.getRefreshToken(), "The refresh token is null");
        assertNotNull(tokenResponse2.getTokenType(), "The token type is null");
        assertEquals(tokenResponse2.getTokenType(), TokenType.DPOP);
    }
}
