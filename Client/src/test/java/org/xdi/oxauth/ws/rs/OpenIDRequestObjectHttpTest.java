/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.AuthorizationRequest;
import org.xdi.oxauth.client.AuthorizationResponse;
import org.xdi.oxauth.client.AuthorizeClient;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.RegisterClient;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.client.RegisterResponse;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoResponse;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.StringUtils;
import org.xdi.oxauth.model.util.Util;

/**
 * Functional tests for OpenID Request Object (HTTP)
 *
 * @author Javier Rojas Blum Date: 09.04.2012
 */
public class OpenIDRequestObjectHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod1(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod1");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 2. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod2(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod2");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 2. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response2.getStatus(), 200, "Unexpected response code: " + response2.getStatus());
        assertNotNull(response2.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response2.getClaim(JwtClaimName.NAME));
        assertNotNull(response2.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response2.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response2.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response2.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod3(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod3");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid");
        String state = "STATE0";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("name", ClaimValue.createNull()));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getCode(), "The code is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod4(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod4");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid");
        String state = "af0ifjsldkj";
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS384, clientSecret);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The accessToken is null");
        assertNotNull(response.getTokenType(), "The tokenType is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod5(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod5");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid");
        String state = "af0ifjsldkj";
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS512, clientSecret);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue(userId)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The accessToken is null");
        assertNotNull(response.getTokenType(), "The tokenType is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethod6(final String userId, final String userSecret,
                                        final String clientId, final String clientSecret,
                                        final String redirectUri) throws Exception {
        showTitle("requestParameterMethod6");

        // 1. Request authorization
        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("name", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 2. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void requestParameterMethodRS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris, final String jwksUri,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS256, privateKey);
        jwtAuthorizationRequest.setKeyId("RS256SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS384_modulus", "RS384_privateExponent"})
    @Test
    public void requestParameterMethodRS384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris, final String jwksUri,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS384, privateKey);
        jwtAuthorizationRequest.setKeyId("RS384SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS512_modulus", "RS512_privateExponent"})
    @Test
    public void requestParameterMethodRS512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris, final String jwksUri,
            final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS512, privateKey);
        jwtAuthorizationRequest.setKeyId("RS512SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES256_d"})
    @Test
    public void requestParameterMethodES256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES256, privateKey);
        jwtAuthorizationRequest.setKeyId("ES256SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES384_d"})
    @Test
    public void requestParameterMethodES384(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES384");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES384, privateKey);
        jwtAuthorizationRequest.setKeyId("ES384SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES512_d"})
    @Test
    public void requestParameterMethodES512(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES512, privateKey);
        jwtAuthorizationRequest.setKeyId("ES512SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS256_modulus", "RS256_privateExponent"})
    @Test
    public void requestParameterMethodRS256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS256X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS256, privateKey);
        jwtAuthorizationRequest.setKeyId("RS256SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS384_modulus", "RS384_privateExponent"})
    @Test
    public void requestParameterMethodRS384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS384X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS384, privateKey);
        jwtAuthorizationRequest.setKeyId("RS384SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri",
            "RS512_modulus", "RS512_privateExponent"})
    @Test
    public void requestParameterMethodRS512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String modulus, final String privateExponent) throws Exception {
        showTitle("requestParameterMethodRS512X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.RS512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPrivateKey privateKey = new RSAPrivateKey(modulus, privateExponent);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.RS512, privateKey);
        jwtAuthorizationRequest.setKeyId("RS512SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES256_d"})
    @Test
    public void requestParameterMethodES256X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES256X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES256);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES256, privateKey);
        jwtAuthorizationRequest.setKeyId("ES256SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES384_d"})
    @Test
    public void requestParameterMethodES384X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES384X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES384);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES384, privateKey);
        jwtAuthorizationRequest.setKeyId("ES384SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris", "clientJwksUri", "ES512_d"})
    @Test
    public void requestParameterMethodES512X509Cert(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris,
            final String jwksUri, final String d) throws Exception {
        showTitle("requestParameterMethodES512X509Cert");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setJwksUri(jwksUri);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.ES512);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        ECDSAPrivateKey privateKey = new ECDSAPrivateKey(d);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.ES512, privateKey);
        jwtAuthorizationRequest.setKeyId("ES512SIG");
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestParameterMethodFail1(final String userId, final String userSecret,
                                            final String clientId, final String redirectUri) throws Exception {
        showTitle("requestParameterMethodFail1");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setRequest("INVALID_OPENID_REQUEST_OBJECT");
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethodFail2(final String userId, final String userSecret,
                                            final String clientId, final String clientSecret,
                                            final String redirectUri) throws Exception {
        showTitle("requestParameterMethodFail2");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt + "INVALID_KEY");

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethodFail3(final String userId, final String userSecret,
                                            final String clientId, final String clientSecret,
                                            final String redirectUri) throws Exception {
        showTitle("requestParameterMethodFail3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        jwtAuthorizationRequest.setClientId("INVALID_CLIENT_ID");
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri"})
    @Test
    public void requestParameterMethodFail4(final String userId, final String userSecret,
                                            final String clientId, final String clientSecret,
                                            final String redirectUri) throws Exception {
        showTitle("requestParameterMethodFail4");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request, SignatureAlgorithm.HS256, clientSecret);
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.SUBJECT_IDENTIFIER, ClaimValue.createSingleValue("INVALID_USER_ID")));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri",
            "requestFileBasePath", "requestFileBaseUrl"})
    @Test // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethod(final String userId, final String userSecret,
                                  final String clientId, final String clientSecret, final String redirectUri,
                                  final String requestFileBasePath, final String requestFileBaseUrl) throws Exception {
        showTitle("requestFileMethod");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        try {
            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = JwtUtil.base64urlencode(JwtUtil.getMessageDigestSHA256(authJwt));
            String fileName = UUID.randomUUID().toString() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName;// + "#" + hash;
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(authJwt);
            bw.close();
            fw.close();
            authorizationRequest.setRequestUri(fileUrl);
            System.out.println("Request JWT: " + authJwt);
            System.out.println("Request File Path: " + filePath);
            System.out.println("Request File URL: " + fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The accessToken is null");
        assertNotNull(response.getTokenType(), "The tokenType is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "redirectUri"})
    @Test
    public void requestFileMethodFail1(final String userId, final String userSecret,
                                       final String clientId, final String redirectUri) throws Exception {
        showTitle("requestFileMethodFail1");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        authorizationRequest.setRequest("FAKE_REQUEST");
        authorizationRequest.setRequestUri("FAKE_REQUEST_URI");

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "redirectUri", "requestFileBaseUrl"})
    @Test
    public void requestFileMethodFail2(final String userId, final String userSecret,
                                       final String clientId, final String redirectUri,
                                       final String requestFileBaseUrl) throws Exception {
        showTitle("requestFileMethodFail2");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);

        authorizationRequest.setRequestUri(requestFileBaseUrl + "/FAKE_REQUEST_URI");

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "clientId", "clientSecret", "redirectUri",
            "requestFileBasePath", "requestFileBaseUrl"})
    @Test // This tests requires a place to publish a request object via HTTPS
    public void requestFileMethodFail3(final String userId, final String userSecret,
                                       final String clientId, final String clientSecret, final String redirectUri,
                                       final String requestFileBasePath, final String requestFileBaseUrl) throws Exception {
        showTitle("requestFileMethodFail3");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        try {
            JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(authorizationRequest, SignatureAlgorithm.HS256, clientSecret);
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
            jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
            jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
            jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
            String authJwt = jwtAuthorizationRequest.getEncodedJwt();
            String hash = "INVALID_HASH";
            String fileName = UUID.randomUUID().toString() + ".txt";
            String filePath = requestFileBasePath + File.separator + fileName;
            String fileUrl = requestFileBaseUrl + "/" + fileName + "#" + hash;
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(authJwt);
            bw.close();
            fw.close();
            authorizationRequest.setRequestUri(fileUrl);
            System.out.println("Request JWT: " + authJwt);
            System.out.println("Request File Path: " + filePath);
            System.out.println("Request File URL: " + fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getErrorType(), "The error type is null");
        assertNotNull(response.getErrorDescription(), "The error description is null");
        assertNotNull(response.getState(), "The state is null");
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
    @Test
    public void requestParameterMethodAlgNone(final String userId, final String userSecret, final String redirectUri,
                                              final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgNone");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setRequestObjectSigningAlg(SignatureAlgorithm.NONE);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
//    @Test
    public void requestParameterMethodAlgRSAOAEPEncA256GCM(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgRSAOAEPEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();

        // 2. Request authorization
        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwksUri, "1");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                KeyEncryptionAlgorithm.RSA_OAEP, BlockEncryptionAlgorithm.A256GCM, publicKey);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
//    @Test
    public void requestParameterMethodAlgRSA15EncA128CBCPLUSHS256(
            final String userId, final String userSecret, final String redirectUri, final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgRSA15EncA128CBCPLUSHS256");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwksUri, "1");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A128CBC_PLUS_HS256, publicKey);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
//    @Test
    public void requestParameterMethodAlgRSA15EncA256CBCPLUSHS512(
            final String userId, final String userSecret, final String redirectUri,
            final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgRSA15EncA256CBCPLUSHS512");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(jwksUri, "1");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                KeyEncryptionAlgorithm.RSA1_5, BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, publicKey);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
    @Test
    public void requestParameterMethodAlgA128KWEncA128GCM(
            final String userId, final String userSecret, final String redirectUri,
            final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                KeyEncryptionAlgorithm.A128KW, BlockEncryptionAlgorithm.A128GCM, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"userId", "userSecret", "redirectUri", "redirectUris"})
    @Test
    public void requestParameterMethodAlgA256KWEncA256GCM(
            final String userId, final String userSecret, final String redirectUri,
            final String redirectUris) throws Exception {
        showTitle("requestParameterMethodAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN);

        // 1. Dynamic Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());

        String clientId = response.getClientId();
        String clientSecret = response.getClientSecret();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(request,
                KeyEncryptionAlgorithm.A256KW, BlockEncryptionAlgorithm.A256GCM, clientSecret.getBytes(Util.UTF8_STRING_ENCODING));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NAME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.NICKNAME, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.EMAIL_VERIFIED, ClaimValue.createNull()));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim(JwtClaimName.PICTURE, ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_TIME, ClaimValue.createNull()));
        jwtAuthorizationRequest.addIdTokenClaim(new Claim(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE, ClaimValue.createValueList(new String[]{"2"})));
        jwtAuthorizationRequest.getIdTokenMember().setMaxAge(86400);
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        request.setRequest(authJwt);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response1 = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response1.getStatus(), 302, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getLocation(), "The location is null");
        assertNotNull(response1.getAccessToken(), "The accessToken is null");
        assertNotNull(response1.getTokenType(), "The tokenType is null");
        assertNotNull(response1.getIdToken(), "The idToken is null");
        assertNotNull(response1.getState(), "The state is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response3 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));
    }
}