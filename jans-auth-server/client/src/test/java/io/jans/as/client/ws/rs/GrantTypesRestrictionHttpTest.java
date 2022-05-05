/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.AuthorizeClient;
import io.jans.as.client.BaseTest;
import io.jans.as.client.EndSessionClient;
import io.jans.as.client.EndSessionRequest;
import io.jans.as.client.EndSessionResponse;
import io.jans.as.client.JwkClient;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtHeaderName;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import jakarta.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.jans.as.client.client.Asserter.*;
import static io.jans.as.model.register.RegisterRequestParam.APPLICATION_TYPE;
import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class GrantTypesRestrictionHttpTest extends BaseTest {

    @Test(dataProvider = "grantTypesRestrictionDataProvider")
    public void grantTypesRestriction(
            final List<ResponseType> responseTypes, final List<ResponseType> expectedResponseTypes,
            final List<GrantType> grantTypes, final List<GrantType> expectedGrantTypes,
            final String userId, final String userSecret,
            final String redirectUris, final String redirectUri, final String sectorIdentifierUri,
            final String postLogoutRedirectUri, final String logoutUri) throws Exception {
        showTitle("grantTypesRestriction");

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "user_name");

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setScope(scopes);
        registerRequest.setSubjectType(SubjectType.PAIRWISE);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setPostLogoutRedirectUris(Arrays.asList(postLogoutRedirectUri));
        registerRequest.setFrontChannelLogoutUri(logoutUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();
        assertNotNull(registerResponse.getResponseTypes());
        assertTrue(registerResponse.getResponseTypes().containsAll(expectedResponseTypes));
        assertNotNull(registerResponse.getGrantTypes());
        assertTrue(registerResponse.getGrantTypes().containsAll(expectedGrantTypes));

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();
        String registrationAccessToken = registerResponse.getRegistrationAccessToken();
        String registrationClientUri = registerResponse.getRegistrationClientUri();

        // 2. Client read
        RegisterRequest readRequest = new RegisterRequest(registrationAccessToken);
        readRequest.setHttpMethod(HttpMethod.GET);

        RegisterClient readClient = new RegisterClient(registrationClientUri);
        readClient.setRequest(readRequest);
        RegisterResponse readResponse = readClient.exec();

        showClient(readClient);
        AssertBuilder.registerResponse(readResponse).ok()
                .notNullRegistrationClientUri()
                .check();
        assertRegisterResponseClaimsNotNull(readResponse, APPLICATION_TYPE, SCOPE);
        assertNotNull(readResponse.getResponseTypes());
        assertTrue(readResponse.getResponseTypes().containsAll(expectedResponseTypes));
        assertNotNull(readResponse.getGrantTypes());
        assertTrue(readResponse.getGrantTypes().containsAll(expectedGrantTypes));

        // 3. Request authorization
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(expectedResponseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        if (expectedResponseTypes.size() == 0) {
            AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
            authorizeClient.setRequest(authorizationRequest);
            AuthorizationResponse authorizationResponse = authorizeClient.exec();

            showClient(authorizeClient);
            assertEquals(authorizationResponse.getStatus(), 302);
            assertNotNull(authorizationResponse.getLocation());
            assertNotNull(authorizationResponse.getErrorType());
            assertNotNull(authorizationResponse.getErrorDescription());
            assertNotNull(authorizationResponse.getState());

            return;
        }

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        String scope = authorizationResponse.getScope();
        String authorizationCode = null;
        String accessToken = null;
        String idToken = null;
        String refreshToken = null;

        assertNotNull(authorizationResponse.getLocation());
        assertNotNull(authorizationResponse.getState());
        assertNotNull(authorizationResponse.getScope());
        if (expectedResponseTypes.contains(ResponseType.CODE)) {
            assertNotNull(authorizationResponse.getCode());

            authorizationCode = authorizationResponse.getCode();
        }
        if (expectedResponseTypes.contains(ResponseType.TOKEN)) {
            assertNotNull(authorizationResponse.getAccessToken());

            accessToken = authorizationResponse.getAccessToken();
        }
        if (expectedResponseTypes.contains(ResponseType.ID_TOKEN)) {
            assertNotNull(authorizationResponse.getIdToken());

            idToken = authorizationResponse.getIdToken();

            // 4. Validate id_token
            Jwt jwt = Jwt.parse(idToken);
            AssertBuilder.jwt(jwt)
                    .validateSignatureRSA(jwksUri, SignatureAlgorithm.RS256)
                    .notNullAuthenticationTime()
                    .check();

            RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                    jwksUri,
                    jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
            RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

            if (expectedResponseTypes.contains(ResponseType.CODE)) {
                assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
                assertTrue(rsaSigner.validateAuthorizationCode(authorizationCode, jwt));
            }
            if (expectedResponseTypes.contains(ResponseType.TOKEN)) {
                assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ACCESS_TOKEN_HASH));
                assertTrue(rsaSigner.validateAccessToken(accessToken, jwt));
            }
        }

        if (expectedResponseTypes.contains(ResponseType.CODE)) {
            // 5. Request access token using the authorization code.
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
                    .check();

            if (expectedGrantTypes.contains(GrantType.REFRESH_TOKEN)) {
                assertNotNull(tokenResponse.getRefreshToken());

                refreshToken = tokenResponse.getRefreshToken();

                // 6. Request new access token using the refresh token.
                TokenClient refreshTokenClient = new TokenClient(tokenEndpoint);
                TokenResponse refreshTokenResponse = refreshTokenClient.execRefreshToken(scope, refreshToken, clientId, clientSecret);

                showClient(refreshTokenClient);
                AssertBuilder.tokenResponse(refreshTokenResponse)
                        .notNullRefreshToken()
                        .check();

                accessToken = refreshTokenResponse.getAccessToken();
            } else {
                assertNull(tokenResponse.getRefreshToken());
            }
        }

        if (accessToken != null) {
            // 7. Request user info
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

            showClient(userInfoClient);
            assertEquals(userInfoResponse.getStatus(), 200);
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.SUBJECT_IDENTIFIER));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.FAMILY_NAME));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.EMAIL));
            assertNotNull(userInfoResponse.getClaim(JwtClaimName.ADDRESS));

            if (idToken != null) {
                // 8. End session
                String endSessionId = UUID.randomUUID().toString();
                String sid = Jwt.parseOrThrow(idToken).getClaims().getClaimAsString("sid");
                EndSessionRequest endSessionRequest = new EndSessionRequest(idToken, postLogoutRedirectUri, endSessionId);
                endSessionRequest.setSid(sid);

                EndSessionClient endSessionClient = new EndSessionClient(endSessionEndpoint);
                endSessionClient.setRequest(endSessionRequest);

                EndSessionResponse endSessionResponse = endSessionClient.exec();

                showClient(endSessionClient);
                assertEquals(endSessionResponse.getStatus(), 200);
                assertNotNull(endSessionResponse.getHtmlPage());

                // silly validation of html content returned by server but at least it verifies that logout_uri and post_logout_uri are present
                assertTrue(endSessionResponse.getHtmlPage().contains("<html>"));
                assertTrue(endSessionResponse.getHtmlPage().contains(logoutUri));
                assertTrue(endSessionResponse.getHtmlPage().contains(postLogoutRedirectUri));
                // assertEquals(endSessionResponse.getState(), endSessionId); // commented out, for http-based logout we get html page
            }
        }
    }

    @DataProvider(name = "grantTypesRestrictionDataProvider")
    public Object[][] omittedResponseTypesFailDataProvider(ITestContext context) {
        String userId = context.getCurrentXmlTest().getParameter("userId");
        String userSecret = context.getCurrentXmlTest().getParameter("userSecret");
        String redirectUris = context.getCurrentXmlTest().getParameter("redirectUris");
        String redirectUri = context.getCurrentXmlTest().getParameter("redirectUri");
        String sectorIdentifierUri = context.getCurrentXmlTest().getParameter("sectorIdentifierUri");
        String postLogoutRedirectUri = context.getCurrentXmlTest().getParameter("postLogoutRedirectUri");
        String logoutUri = context.getCurrentXmlTest().getParameter("logoutUri");

        return new Object[][]{
                {
                        Arrays.asList(),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(),
                        Arrays.asList(),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(ResponseType.CODE),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(ResponseType.TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                //
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.REFRESH_TOKEN),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT, GrantType.REFRESH_TOKEN),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.CLIENT_CREDENTIALS),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.CLIENT_CREDENTIALS),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
                {
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(ResponseType.CODE, ResponseType.TOKEN, ResponseType.ID_TOKEN),
                        Arrays.asList(GrantType.OXAUTH_UMA_TICKET),
                        Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN, GrantType.IMPLICIT, GrantType.OXAUTH_UMA_TICKET),
                        userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri, postLogoutRedirectUri, logoutUri
                },
        };
    }
}
