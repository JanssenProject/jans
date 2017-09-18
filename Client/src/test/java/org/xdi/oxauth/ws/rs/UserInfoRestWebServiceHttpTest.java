/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.*;
import org.xdi.oxauth.client.model.authorize.Claim;
import org.xdi.oxauth.client.model.authorize.ClaimValue;
import org.xdi.oxauth.client.model.authorize.JwtAuthorizationRequest;
import org.xdi.oxauth.client.model.authorize.UserInfoMember;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.crypto.OxAuthCryptoProvider;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * Functional tests for User Info Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version September 6, 2017
 */
public class UserInfoRestWebServiceHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoImplicitFlow(final String userId, final String userSecret,
                                            final String redirectUris, final String redirectUri,
                                            final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNotNull(response2.getClaim(JwtClaimName.ADDRESS));
        assertNull(response2.getClaim("org_name"));
        assertNull(response2.getClaim("work_phone"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoWithNotAllowedScopeImplicitFlow(final String userId, final String userSecret,
                                                               final String redirectUris, final String redirectUri,
                                                               final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoWithNotAllowedScopeImplicitFlow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "mobile_phone");
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNotNull(response2.getClaim(JwtClaimName.ADDRESS));
        assertNull(response2.getClaim("phone_mobile_number"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoDynamicScopesImplicitFlow(final String userId, final String userSecret,
                                                         final String redirectUris, final String redirectUri,
                                                         final String sectorIdentifierUri) throws Exception {
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
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        AuthorizationResponse response1 = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId, scopes);

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNotNull(response2.getClaim(JwtClaimName.ADDRESS));
        assertNotNull(response2.getClaim("org_name"));
        assertNotNull(response2.getClaim("work_phone"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoPasswordFlow(final String userId, final String userSecret,
                                            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoPasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
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
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNull(response2.getClaim("org_name"));
        assertNull(response2.getClaim("work_phone"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoWithNotAllowedScopePasswordFlow(final String userId, final String userSecret,
                                                               final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoWithNotAllowedScopePasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
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
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNull(response2.getClaim("phone_mobile_number"));
    }

    @Parameters({"userId", "userSecret", "redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoDynamicScopesPasswordFlow(final String userId, final String userSecret,
                                                         final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoDynamicScopesPasswordFlow");

        List<ResponseType> responseTypes = new ArrayList<ResponseType>();
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
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
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");

        String accessToken = response1.getAccessToken();

        // 3. Request user info
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
        assertNotNull(response2.getClaim("org_name"));
        assertNotNull(response2.getClaim("work_phone"));
    }

    @Test
    public void requestUserInfoInvalidRequest() throws Exception {
        showTitle("requestUserInfoInvalidRequest");

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response = userInfoClient.execUserInfo(null);

        showClient(userInfoClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Test
    public void requestUserInfoInvalidToken() throws Exception {
        showTitle("requestUserInfoInvalidToken");

        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse response = userInfoClient.execUserInfo("INVALID_ACCESS_TOKEN");

        showClient(userInfoClient);
        assertEquals(response.getStatus(), 400, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getErrorType(), "Unexpected result: errorType not found");
        assertNotNull(response.getErrorDescription(), "Unexpected result: errorDescription not found");
    }

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoInsufficientScope(final String userId, final String userSecret,
                                                 final String redirectUris, final String redirectUri,
                                                 final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoInsufficientScope");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN
        );
        List<GrantType> grantTypes = Arrays.asList(
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS
        );

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        List<String> scopes = Arrays.asList("picture");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");
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

        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        JwtAuthorizationRequest jwtAuthorizationRequest = new JwtAuthorizationRequest(
                authorizationRequest, SignatureAlgorithm.HS256, clientSecret, cryptoProvider);
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("invalid", ClaimValue.createEssential(false)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("iname", ClaimValue.createNull()));
        //jwtAuthorizationRequest.addUserInfoClaim(new Claim("gluuStatus", ClaimValue.createEssential(true)));
        //jwtAuthorizationRequest.addUserInfoClaim(new Claim("gluuWhitePagesListed", ClaimValue.createEssential(true)));
        jwtAuthorizationRequest.addUserInfoClaim(new Claim("o", ClaimValue.createEssential(true)));
        String authJwt = jwtAuthorizationRequest.getEncodedJwt();
        authorizationRequest.setRequest(authJwt);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info (AUTHORIZATION_REQUEST_HEADER_FIELD)
        UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);
        userInfoRequest.setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setRequest(userInfoRequest);
        UserInfoResponse userInfoResponse = userInfoClient.exec();

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));

        // Custom Claims
        assertNotNull(userInfoResponse.getClaim("iname"), "Unexpected result: iname not found");
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
        assertEquals(response3.getStatus(), 200, "Unexpected response code: " + response3.getStatus());
        assertNotNull(response3.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response3.getClaim(JwtClaimName.NAME));
        assertNotNull(response3.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response3.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response3.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response3.getClaim(JwtClaimName.LOCALE));

        // 5. Request user info (URL_QUERY_PARAMETER)
        UserInfoRequest userInfoRequest3 = new UserInfoRequest(accessToken);
        userInfoRequest3.setAuthorizationMethod(AuthorizationMethod.URL_QUERY_PARAMETER);
        UserInfoClient userInfoClient3 = new UserInfoClient(userInfoEndpoint);
        userInfoClient3.setRequest(userInfoRequest3);
        UserInfoResponse response4 = userInfoClient3.exec();

        showClient(userInfoClient3);
        assertEquals(response4.getStatus(), 200, "Unexpected response code: " + response4.getStatus());
        assertNotNull(response4.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(response4.getClaim(JwtClaimName.NAME));
        assertNotNull(response4.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(response4.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(response4.getClaim(JwtClaimName.EMAIL));
        assertNotNull(response4.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(response4.getClaim(JwtClaimName.LOCALE));
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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientIdIssuedAt());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization
        OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider();

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

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getCode());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());

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
        assertEquals(tokenResponse.getStatus(), 200, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getEntity(), "The entity is null");
        assertNotNull(tokenResponse.getAccessToken(), "The access token is null");
        assertNotNull(tokenResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(tokenResponse.getTokenType(), "The token type is null");
        assertNotNull(tokenResponse.getRefreshToken(), "The refresh token is null");

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
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoHS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoHS384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoHS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoHS512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoHS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.HS512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setSharedKey(clientSecret);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoRS256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoRS384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoRS512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoRS512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.RS512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES256(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoES256");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES256);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES384(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoES384");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES384);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoES512(final String redirectUris, final String redirectUri,
                                     final String userId, final String userSecret,
                                     final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoES512");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setUserInfoSignedResponseAlg(SignatureAlgorithm.ES512);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

        String clientId = registerResponse.getClientId();

        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

        String accessToken = authorizationResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setJwksUri(jwksUri);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ISSUER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.AUDIENCE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "sectorIdentifierUri",
            "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "sectorIdentifierUri",
            "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "clientJwksUri", "sectorIdentifierUri",
            "RS256_keyId", "keyStoreFile", "keyStoreSecret"})
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
            RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
            assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
            assertNotNull(registerResponse.getClientId());
            assertNotNull(registerResponse.getClientSecret());
            assertNotNull(registerResponse.getRegistrationAccessToken());
            assertNotNull(registerResponse.getClientSecretExpiresAt());

            String clientId = registerResponse.getClientId();

            AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, clientId);

            String accessToken = authorizationResponse.getAccessToken();

            // 3. Request user info (encrypted)
            OxAuthCryptoProvider cryptoProvider = new OxAuthCryptoProvider(keyStoreFile, keyStoreSecret, null);
            PrivateKey privateKey = cryptoProvider.getPrivateKey(keyId);

            UserInfoRequest userInfoRequest = new UserInfoRequest(accessToken);

            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            userInfoClient.setPrivateKey(privateKey);
            userInfoClient.setRequest(userInfoRequest);
            UserInfoResponse userInfoResponse = userInfoClient.exec();

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
        } catch (Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoAlgA128KWEncA128GCM(final String redirectUris, final String redirectUri,
                                                   final String userId, final String userSecret,
                                                   final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoAlgA128KWEncA128GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    @Parameters({"redirectUris", "redirectUri", "userId", "userSecret", "sectorIdentifierUri"})
    @Test
    public void requestUserInfoAlgA256KWEncA256GCM(final String redirectUris, final String redirectUri,
                                                   final String userId, final String userSecret,
                                                   final String sectorIdentifierUri) throws Exception {
        showTitle("requestUserInfoAlgA256KWEncA256GCM");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Dynamic Registration
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
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
        assertEquals(registerResponse.getStatus(), 200, "Unexpected response code: " + registerResponse.getEntity());
        assertNotNull(registerResponse.getClientId());
        assertNotNull(registerResponse.getClientSecret());
        assertNotNull(registerResponse.getRegistrationAccessToken());
        assertNotNull(registerResponse.getClientSecretExpiresAt());

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
        assertEquals(userInfoResponse.getStatus(), 200, "Unexpected response code: " + userInfoResponse.getStatus());
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.GIVEN_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.PICTURE));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.ZONEINFO));
        assertNotNull(userInfoResponse.getClaim(JwtClaimName.LOCALE));
    }

    private RegisterResponse registerClient(final String redirectUris, final List<ResponseType> responseTypes,
                                            final List<GrantType> grantTypes, final String sectorIdentifierUri) {
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);

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

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");
        assertNotNull(authorizationResponse.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse.getScope(), "The scope must be null");
        assertNotNull(authorizationResponse.getIdToken(), "The id token must be null");
        return authorizationResponse;
    }

}