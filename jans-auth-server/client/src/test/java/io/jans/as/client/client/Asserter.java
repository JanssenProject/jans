/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.client;

import io.jans.as.client.*;
import io.jans.as.client.par.ParResponse;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.RegisterRequestParam;
import io.jans.as.model.token.TokenErrorResponseType;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.client.BaseTest.clientEngine;
import static io.jans.as.model.register.RegisterRequestParam.*;
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

    public static void assertRegisterResponseOk(RegisterResponse response, int status, boolean checkClientUri) {
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        if (checkClientUri) {
            assertNotNull(response.getRegistrationClientUri());//Review usage
        }
    }

    public static void assertRegisterResponseFail(RegisterResponse registerResponse) {
        assertEquals(registerResponse.getStatus(), 400, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getEntity(), "The entity is null");
        assertNotNull(registerResponse.getErrorType(), "The error type is null");
        assertNotNull(registerResponse.getErrorDescription(), "The error description is null");
    }


    public static void assertRegisterResponseClaimsNotNull(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertNotNull(response.getClaims().get(claim.toString()), "Claim " + claim.toString() + " is null in response claims - code" + response.getEntity());
        }
    }

    public static void assertRegisterResponseClaimsAreContained(RegisterResponse response, RegisterRequestParam... claimsToVerify) {
        if (response == null || claimsToVerify == null) {
            return;
        }
        for (RegisterRequestParam claim : claimsToVerify) {
            assertTrue(response.getClaims().containsKey(claim.toString()), "Claim " + claim.toString() + " is not contained in response claims - code" + response.getEntity());
        }
    }

    public static void assertRegisterResponseClaimsBackChannel(RegisterResponse registerResponse, AsymmetricSignatureAlgorithm authRequestSigningAlgorithm, BackchannelTokenDeliveryMode tokenDeliveryMode, Boolean userCodeParameter) {
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()));
        assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()));
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_TOKEN_DELIVERY_MODE.toString()), tokenDeliveryMode.getValue());
        assertEquals(registerResponse.getClaims().get(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString()), authRequestSigningAlgorithm.getValue());
        if (userCodeParameter != null) {
            assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_USER_CODE_PARAMETER.toString()));
            assertEquals(registerResponse.getClaims().get(BACKCHANNEL_USER_CODE_PARAMETER.toString()), String.valueOf(userCodeParameter));
        }

        if (tokenDeliveryMode.equals(BackchannelTokenDeliveryMode.PING) || tokenDeliveryMode.equals(BackchannelTokenDeliveryMode.PUSH)) {
            assertTrue(registerResponse.getClaims().containsKey(BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString()));
        }
    }

    public static void assertTokenResponseOk(TokenResponse response, boolean checkRefreshToken) {
        assertNotNull(response);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        assertNotNull(response.getIdToken(), "The id token is null");
        if (checkRefreshToken) {
            assertNotNull(response.getRefreshToken(), "The refresh token is null");
        }
    }

    public static void assertTokenResponseOk(TokenResponse response, boolean checkRefreshToken, boolean checkIdToken) {
        assertNotNull(response);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getAccessToken(), "The access token is null");
        assertNotNull(response.getExpiresIn(), "The expires in value is null");
        assertNotNull(response.getTokenType(), "The token type is null");
        if (checkIdToken) {
            assertNotNull(response.getIdToken(), "The id token is null");
        }
        if (checkRefreshToken) {
            assertNotNull(response.getRefreshToken(), "The refresh token is null");
        }
    }

    public static void assertTokenResponseFail(TokenResponse tokenResponse, int status, TokenErrorResponseType errorResponseType) {
        assertEquals(tokenResponse.getStatus(), status, "Unexpected HTTP status resposne: " + tokenResponse.getEntity());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertEquals(tokenResponse.getErrorType(), errorResponseType, "Unexpected error type, should be " + errorResponseType.getParameter());
        assertNotNull(tokenResponse.getErrorDescription());
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

    public static void assertAuthorizationResponse(AuthorizationResponse authorizationResponse, List<ResponseType> responseTypes, boolean checkState, boolean checkScope) {
        assertNotNull(authorizationResponse);
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        if (checkScope) {
            assertNotNull(authorizationResponse.getScope(), "The scope is null");
        }

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


    public static void assertBackchannelAuthentication(BackchannelAuthenticationResponse backchannelAuthenticationResponse, boolean assertNotNullInterval) {
        assertEquals(backchannelAuthenticationResponse.getStatus(), 200, "Unexpected response code: " + backchannelAuthenticationResponse.getEntity());
        assertNotNull(backchannelAuthenticationResponse.getAuthReqId());
        assertNotNull(backchannelAuthenticationResponse.getExpiresIn());
        if (assertNotNullInterval) {
            assertNotNull(backchannelAuthenticationResponse.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
        } else {
            assertNull(backchannelAuthenticationResponse.getInterval()); // This parameter will only be present if the Client is registered to use the Poll or Ping modes.
        }
    }

    public static void assertBackchannelAuthenticationFail(BackchannelAuthenticationResponse response, int status, BackchannelAuthenticationErrorResponseType errorResponseType) {
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertEquals(errorResponseType, response.getErrorType());
        assertNotNull(response.getErrorDescription(), "The error description is null");
    }

    public static void assertUserInfoBasicResponseOk(UserInfoResponse userInfoResponse, int status) {
        assertEquals(userInfoResponse.getStatus(), status, "Unexpected response code: " + userInfoResponse.getEntity());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
    }

    public static void assertUserInfoBasicMinimumResponseOk(UserInfoResponse userInfoResponse, int status) {
        assertEquals(userInfoResponse.getStatus(), status, "Unexpected response code: " + userInfoResponse.getEntity());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
    }

    public static void assertUserInfoAddressNotNull(UserInfoResponse userInfoResponse) {
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS_STREET_ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS_COUNTRY));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                JwtClaimName.ADDRESS_STREET_ADDRESS,
                JwtClaimName.ADDRESS_COUNTRY,
                JwtClaimName.ADDRESS_LOCALITY,
                JwtClaimName.ADDRESS_REGION)));
    }

    public static void assertUserInfoAddressMinimumNotNull(UserInfoResponse userInfoResponse) {
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS).containsAll(Arrays.asList(
                JwtClaimName.ADDRESS_STREET_ADDRESS,
                JwtClaimName.ADDRESS_COUNTRY,
                JwtClaimName.ADDRESS_LOCALITY,
                JwtClaimName.ADDRESS_REGION)));
    }

    public static void assertUserInfoPersonalDataNotNull(UserInfoResponse userInfoResponse) {
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    public static void assertUserInfoPersonalDataNotNull(UserInfoResponse userInfoResponse, boolean checkEmail) {
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        if (checkEmail) {
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        }
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    public static void assertJweStandarClaimsNotNull(Jwe jwe, boolean checkAccessTokenHash) {
        assertNotNull(jwe);
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwe.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));

        assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        if (checkAccessTokenHash) {
            assertNotNull(jwe.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        }
    }

    public static void assertJwtStandarClaimsNotNull(Jwt jwt, boolean checkAccessTokenHash) {
        assertNotNull(jwt);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));

        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        if (checkAccessTokenHash) {
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        }
    }

    public static void assertJwtStandarClaimsNotNull(Jwt jwt, boolean checkAccessTokenHash, boolean checkAuthenticationTime) {
        assertNotNull(jwt);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        if(checkAuthenticationTime){
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));
        }

        if (checkAccessTokenHash) {
            assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
        }
    }

    public static void assertJwtAddressClaimsNotNull(Jwt jwt) {
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ADDRESS_STREET_ADDRESS));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ADDRESS_COUNTRY));
        assertNotNull(jwt.getClaims().getClaim(JwtClaimName.ADDRESS));
        assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_STREET_ADDRESS));
        assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_COUNTRY));
        assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_LOCALITY));
        assertNotNull(jwt.getClaims().getClaimAsJSON(JwtClaimName.ADDRESS).has(JwtClaimName.ADDRESS_REGION));
    }

}
