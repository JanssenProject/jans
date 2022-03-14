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
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoRequest;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.model.authorize.Claim;
import io.jans.as.client.model.authorize.ClaimValue;
import io.jans.as.client.model.authorize.JwtAuthorizationRequest;
import io.jans.as.client.model.authorize.UserInfoMember;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * Functional tests for User Info Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version May 14, 2019
 */
public class UserInfoRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoImplicitFlow(final String userId, final String userSecret,
                                            final String redirectUris, final String redirectUri,
                                            final String sectorIdentifierUri) {
        showTitle("requestUserInfoImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Register client
        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoWithNotAllowedScopeImplicitFlow(final String userId, final String userSecret,
                                                               final String redirectUris, final String redirectUri,
                                                               final String sectorIdentifierUri) {
        showTitle("requestUserInfoWithNotAllowedScopeImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Register client
        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "mobile_phone");
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .claimsNoPresence("phone_mobile_number")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoDynamicScopesImplicitFlow(final String userId, final String userSecret,
                                                         final String redirectUris, final String redirectUri,
                                                         final String sectorIdentifierUri) {
        showTitle("requestUserInfoDynamicScopesImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "org_name", "work_phone");

        // 1. Register client
        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.ADDRESS)
                .claimsPresence("org_name", "work_phone")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoPasswordFlow(final String userId, final String userSecret,
                                            final String redirectUris, final String sectorIdentifierUri) {
        showTitle("requestUserInfoPasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        String username = userId;
        String password = userSecret;
        String scope = "openid profile address email";

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient.execResourceOwnerPasswordCredentialsGrant(username, password, scope,
                clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(response1)
                .notNullScope()
                .check();

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .claimsNoPresence("org_name", "work_phone")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoWithNotAllowedScopePasswordFlow(final String userId, final String userSecret,
                                                               final String redirectUris, final String sectorIdentifierUri) {
        showTitle("requestUserInfoWithNotAllowedScopePasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        String username = userId;
        String password = userSecret;
        String scope = "openid profile address email mobile_phone";

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient.execResourceOwnerPasswordCredentialsGrant(username, password, scope,
                clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(response1)
                .notNullScope()
                .check();

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .claimsNoPresence("phone_mobile_number")
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoDynamicScopesPasswordFlow(final String userId, final String userSecret,
                                                         final String redirectUris, final String sectorIdentifierUri) {
        showTitle("requestUserInfoDynamicScopesPasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        String username = userId;
        String password = userSecret;
        String scope = "openid profile address email org_name work_phone";

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient.execResourceOwnerPasswordCredentialsGrant(username, password, scope,
                clientId, clientSecret);

        showClient(tokenClient);
        AssertBuilder.tokenResponse(response1)
                .notNullScope()
                .check();

        String accessToken = response1.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response2 = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(response2)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .claimsPresence("org_name", "work_phone")
                .check();
    }

    @Test
    public void requestUserInfoInvalidRequest() {
        showTitle("requestUserInfoInvalidRequest");

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response = userInfoClient.execUserInfo(null);

        showClient(userInfoClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Test
    public void requestUserInfoInvalidToken() {
        showTitle("requestUserInfoInvalidToken");

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response = userInfoClient.execUserInfo("INVALID_ACCESS_TOKEN");

        showClient(userInfoClient);
        assertEquals(response.getStatus(), 401, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoInsufficientScope(final String userId, final String userSecret,
                                                 final String redirectUris, final String redirectUri,
                                                 final String sectorIdentifierUri) {
        showTitle("requestUserInfoInsufficientScope");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = register(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("picture");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse)
                .nullScope()
                .responseTypes(responseTypes)
                .check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 403, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoExpiredAccessToken(final String redirectUris, final String redirectUri,
                                                  final String userId, final String userSecret,
                                                  final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoRS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setAccessTokenLifetime(3);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        {
            // 3. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setJwksUri(jwksUri);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(userInfoResponse)
                    .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL)
                    .check();
        }

        Thread.sleep(4000);

        {
            // 3. Request user info with expired access token
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setJwksUri(jwksUri);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 401, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getErrorType(), "Unexpected result: errorType not found");
            assertNotNull(userInfoResponse.getErrorDescription(), "Unexpected result: errorDescription not found");
        }
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoAdditionalClaims(final String userId, final String userSecret,
                                                final String redirectUris, final String redirectUri,
                                                final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoAdditionalClaims");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.TOKEN);
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Client Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setClaims(Arrays.asList(
                "o"));

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("invalid", ClaimValue.createEssential(false)));
        //jwtAuthorizationRequest.addUserInfoClaim(new Claim("gluuStatus", ClaimValue.createEssential(true)));
        //jwtAuthorizationRequest.addUserInfoClaim(new Claim("gluuWhitePagesListed", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("o", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info (AUTHORIZATION_REQUEST_HEADER_FIELD)
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        userInfoRequest.setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();

        // Custom Claims
        //assertNotNull(response2.getClaim("gluuStatus"), "Unexpected result: gluuStatus not found");
        //assertNotNull(response2.getClaim("gluuWhitePagesListed"), "Unexpected result: gluuWhitePagesListed not found");
        assertNotNull(userInfoResponse.getClaim("o"), "Unexpected result: organization not found");

        // 4. Request user info (FORM_ENCODED_BODY_PARAMETER)
        UserInfoRequest userInfoRequest2 = new UserInfoRequest(accessToken);
        userInfoRequest2.setAuthorizationMethod(AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER);
        UserInfoClient userInfoClient2 = new UserInfoClient(userInfoEndpoint);
        userInfoClient2.setRequest(userInfoRequest2);
        UserInfoResponse response3 = userInfoClient2.exec();

        showClient(userInfoClient2);
        AssertBuilder.userInfoResponse(response3)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();

        // 5. Request user info (URL_QUERY_PARAMETER)
        UserInfoRequest userInfoRequest3 = new UserInfoRequest(accessToken);
        userInfoRequest3.setAuthorizationMethod(AuthorizationMethod.URL_QUERY_PARAMETER);
        UserInfoClient userInfoClient3 = new UserInfoClient(userInfoEndpoint);
        userInfoClient3.setRequest(userInfoRequest3);
        UserInfoResponse response4 = userInfoClient3.exec();

        showClient(userInfoClient3);
        AssertBuilder.userInfoResponse(response4)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri", "clientJwksUri",
            "postLogoutRedirectUri"})
    @Test
    public void claimsRequestWithEssentialNameClaim(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri, final String clientJwksUri, final String postLogoutRedirectUri) throws Exception {
        showTitle("claimsRequestWithEssentialNameClaim");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.AUTHORIZATION_CODE
        );

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setContacts(Arrays.asList("javier@gluu.org", "javier.rojas.blum@gmail.com"));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setJwksUri(clientJwksUri);
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        AuthCryptoProvider cryptoProvider = new AuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        JSONObject claimsObj = new JSONObject();
        UserInfoMember userInfoMember = new UserInfoMember();
        userInfoMember.getClaims().add(new Claim("name", ClaimValue.createEssential(true)));
        claimsObj.put("userinfo", userInfoMember.toJSONObject());

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);
        authorizationRequest.setClaims(claimsObj);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);
        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();

        String authorizationCode = authorizationResponse.getCode();

        // 3. Request access token using the authorization code.
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
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 4. Request user info
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoHS256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoHS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoHS384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoHS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoHS512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoHS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoRS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoRS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoRS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoES256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoES384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoES512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoPS256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoPS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoPS384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoRS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoPS512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) {
        showTitle("requestUserInfoPS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret",
            "clientJwksUri", "sectorIdentifierUri", "RSA_OAEP_keyId", "keyStoreFile",
            "keyStoreSecret"})
    @Test
    public void requestUserInfoAlgRSAOAEPEncA256GCM(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String jwksUri, final String sectorIdentifierUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) {
        try {
            showTitle("requestUserInfoAlgRSAOAEPEncA256GCM");

            List<ResponseType> responseTypes = Arrays.asList(
                    ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            // 1. Dynamic Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA_OAEP);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(userInfoResponse)
                    .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL)
                    .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret",
            "clientJwksUri", "sectorIdentifierUri", "RSA1_5_keyId", "keyStoreFile",
            "keyStoreSecret"})
    @Test
    public void requestUserInfoAlgRSA15EncA128CBCPLUSHS256(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String jwksUri, final String sectorIdentifierUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) {
        try {
            showTitle("requestUserInfoAlgRSA15EncA128CBCPLUSHS256");

            List<ResponseType> responseTypes = Arrays.asList(
                    ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            // 1. Dynamic Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128CBC_PLUS_HS256);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(userInfoResponse)
                    .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL)
                    .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret",
            "clientJwksUri", "sectorIdentifierUri", "RSA1_5_keyId", "keyStoreFile",
            "keyStoreSecret"})
    @Test
    public void requestUserInfoAlgRSA15EncA256CBCPLUSHS512(
            final String redirectUris, final String redirectUri, final String userId, final String userSecret,
            final String jwksUri, final String sectorIdentifierUri, final String keyId, final String keyStoreFile,
            final String keyStoreSecret) {
        try {
            showTitle("requestUserInfoAlgRSA15EncA256CBCPLUSHS512");

            List<ResponseType> responseTypes = Arrays.asList(
                    ResponseType.TOKEN,
                    ResponseType.ID_TOKEN);

            // 1. Dynamic Registration
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                    StringUtils.spaceSeparatedToList(redirectUris));
            registerRequest.setJwksUri(jwksUri);
            registerRequest.setResponseTypes(responseTypes);
            registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.RSA1_5);
            registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256CBC_PLUS_HS512);
            registerRequest.setSubjectType(SubjectType.PAIRWISE);
            registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

            RegisterClient registerClient = new RegisterClient(registrationEndpoint);
            registerClient.setRequest(registerRequest);
            RegisterResponse registerResponse = registerClient.exec();

            showClient(registerClient);
            AssertBuilder.registerResponse(registerResponse).created().check();

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            AuthCryptoProvider cryptoProvider = new AuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            AssertBuilder.userInfoResponse(userInfoResponse)
                    .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                    .notNullClaimsPersonalData()
                    .claimsPresence(JwtClaimName.EMAIL)
                    .check();
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoAlgA128KWEncA128GCM(final String redirectUris, final String redirectUri,
                                                   final String userId, final String userSecret,
                                                   final String sectorIdentifierUri) {
        showTitle("requestUserInfoAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A128KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A128GCM);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info (encrypted)
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoAlgA256KWEncA256GCM(final String redirectUris, final String redirectUri,
                                                   final String userId, final String userSecret,
                                                   final String sectorIdentifierUri) {
        showTitle("requestUserInfoAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoEncryptedResponseAlg(KeyEncryptionAlgorithm.A256KW);
        registerRequest.setUserInfoEncryptedResponseEnc(BlockEncryptionAlgorithm.A256GCM);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info (encrypted)
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }

    private RegisterResponse register(final String redirectUris, final List<ResponseType> responseTypes,
                                      final List<GrantType> grantTypes, final String sectorIdentifierUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        return registerResponse;
    }

    private AuthorizationResponse requestAuthorization(final String userId, final String userSecret, final String redirectUri,
                                                       List<ResponseType> responseTypes, String clientId) {
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        return requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);
    }

    private AuthorizationResponse requestAuthorization(
            final String userId, final String userSecret, final String redirectUri, List<ResponseType> responseTypes,
            String clientId, List<String> scopes) {
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
        assertNotNull(authorizationResponse.getIdToken(), "The id token must be null");
        return authorizationResponse;
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoWithoutOpenidScope(final String userId, final String userSecret,
                                                  final String redirectUris, final String redirectUri,
                                                  final String sectorIdentifierUri) {
        showTitle("requestUserInfoWithoutOpenidScope");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("profile", "address", "email");

        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        AssertBuilder.authorizationResponse(authorizationResponse).responseTypes(responseTypes).check();
        assertNotNull(authorizationResponse.getIdToken(), "The id token must be null");

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 403, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(userInfoResponse.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoSubjectTypePublic(final String redirectUris, final String redirectUri,
                                                 final String userId, final String userSecret,
                                                 final String sectorIdentifierUri) {
        showTitle("requestUserInfoSubjectTypePublic");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.PS512);
        registerRequest.setSubjectType(SubjectType.PUBLIC);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .claimsPresence(JwtClaimName.ISSUER, JwtClaimName.AUDIENCE)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL)
                .check();
    }
}