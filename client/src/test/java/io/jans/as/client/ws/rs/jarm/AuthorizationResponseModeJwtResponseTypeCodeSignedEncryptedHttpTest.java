/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.JwtUtil;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * @author Javier Rojas Blum
 * @version September 9, 2021
 */
public class AuthorizationResponseModeJwtResponseTypeCodeSignedEncryptedHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "RSA_OAEP_keyId",
            "PS256_keyId", "dnName", "keyStoreFile", "keyStoreSecret", "sectorIdentifierUri"})
    @Test
    public void authorizationRequestObjectPS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String clientJwksUri, final String encryptionKeyId, final String signingKeyId, final String dnName, final String keyStoreFile,
            final String keyStoreSecret, final String sectorIdentifierUri) throws Exception {
        showTitle("requestParameterMethodPS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);

        // 1. Dynamic Client Registration
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, sectorIdentifierUri, clientJwksUri,
                SignatureAlgorithm.PS256, KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM);

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setResponseMode(ResponseMode.JWT);
        authorizationRequest.setState(state);

        AuthCryptoProvider cryptoProvider1 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);

        JwtAuthorizationRequest jwsAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.PS256, cryptoProvider1);
        jwsAuthorizationRequest.setKeyId(signingKeyId);
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwsAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwsAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwsAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        Jwt authJws = Jwt.parse(jwsAuthorizationRequest.getEncodedJwt());

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse jwkResponse = jwkClient.exec();
        String serverKeyId = jwkResponse.getKeyId(Algorithm.RSA_OAEP);
        assertNotNull(serverKeyId);

        JSONObject jwks = JwtUtil.getJSONWebKeys(jwksUri);
        AuthCryptoProvider cryptoProvider2 = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, dnName);
        privateKey = cryptoProvider2.getPrivateKey(encryptionKeyId);

        JwtAuthorizationRequest jweAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, cryptoProvider2);
        jweAuthorizationRequest.setKeyId(serverKeyId);
        jweAuthorizationRequest.setNestedPayload(authJws);
        String authJwe = jweAuthorizationRequest.getEncodedJwt(jwks);

        authorizationRequest.setRequest(authJwe);

        authorizationRequest(authorizationRequest, ResponseMode.QUERY_JWT, userId, userSecret);
        privateKey = null; // Clear private key to do not affect to other tests
    }
}
