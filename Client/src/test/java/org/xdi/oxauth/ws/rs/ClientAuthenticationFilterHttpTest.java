/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.crypto.signature.RSAPublicKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jws.RSASigner;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.jwt.JwtHeaderName;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.StringUtils;

/**
 * @author Javier Rojas Blum Date: 09.06.2012
 */
public class ClientAuthenticationFilterHttpTest extends BaseTest {

    private String customAttrValue1;

    @Parameters({"redirectUris"})
    @Test
    public void requestClientRegistrationWithCustomAttributes(final String redirectUris) throws Exception {
        showTitle("requestClientRegistrationWithCustomAttributes");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        customAttrValue1 = UUID.randomUUID().toString();
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.addCustomAttribute("oxAuthTrustedClient", "true");
        registerRequest.addCustomAttribute("myCustomAttr1", customAttrValue1);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientSecretExpiresAt());
    }

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth1(final String userId, final String userSecret,
                                                    final String redirectUri) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth1");

        // 1. Request authorization and receive the authorization code.
        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = "af0ifjsldkj";

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, customAttrValue1, scopes, redirectUri, null);
        authorizationRequest.setState(state);
        authorizationRequest.setAuthUsername(userId);
        authorizationRequest.setAuthPassword(userSecret);
        authorizationRequest.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(authorizationRequest);
        AuthorizationResponse authorizationResponse = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(authorizationResponse.getStatus(), 302, "Unexpected response code: " + authorizationResponse.getStatus());
        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The code is null");
        assertNotNull(authorizationResponse.getIdToken(), "The idToken is null");
        assertNotNull(authorizationResponse.getState(), "The state is null");

        String authorizationCode = authorizationResponse.getCode();
        String idToken = authorizationResponse.getIdToken();

        // 2. Validate code and id_token
        Jwt jwt = Jwt.parse(idToken);
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.TYPE));
        assertNotNull(jwt.getHeader().getClaimAsString(JwtHeaderName.ALGORITHM));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUDIENCE));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.EXPIRATION_TIME));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUED_AT));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.SUBJECT_IDENTIFIER));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.CODE_HASH));
        assertNotNull(jwt.getClaims().getClaimAsString(JwtClaimName.AUTHENTICATION_TIME));

        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);

        assertTrue(rsaSigner.validate(jwt));
        assertTrue(rsaSigner.validateAuthorizationCode(authorizationCode, jwt));

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(customAttrValue1);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

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

    @Parameters({"userId", "userSecret", "redirectUri"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth2(final String userId, final String userSecret,
                                                    final String redirectUri) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth2");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String nonce = UUID.randomUUID().toString();
        String state = "af0ifjsldkj";

        AuthorizationRequest request = new AuthorizationRequest(responseTypes, customAttrValue1, scopes, redirectUri, nonce);
        request.setState(state);
        request.setAuthUsername(userId);
        request.setAuthPassword(userSecret);
        request.getPrompts().add(Prompt.NONE);

        AuthorizeClient authorizeClient = new AuthorizeClient(authorizationEndpoint);
        authorizeClient.setRequest(request);
        AuthorizationResponse response = authorizeClient.exec();

        showClient(authorizeClient);
        assertEquals(response.getStatus(), 302, "Unexpected response code: " + response.getStatus());
        assertNotNull(response.getLocation(), "The location is null");
        assertNotNull(response.getAccessToken(), "The accessToken is null");
        assertNotNull(response.getTokenType(), "The tokenType is null");
        assertNotNull(response.getIdToken(), "The idToken is null");
        assertNotNull(response.getState(), "The state is null");

        String accessToken = response.getAccessToken();
        String idToken = response.getIdToken();

        Jwt jwt = Jwt.parse(idToken);
        RSAPublicKey publicKey = JwkClient.getRSAPublicKey(
                jwksUri,
                jwt.getHeader().getClaimAsString(JwtHeaderName.KEY_ID));
        RSASigner rsaSigner = new RSASigner(SignatureAlgorithm.RS256, publicKey);
        assertTrue(rsaSigner.validate(jwt));
        assertTrue(rsaSigner.validateAccessToken(accessToken, jwt));
    }

    @Parameters({"userId", "userSecret"})
    @Test(dependsOnMethods = "requestClientRegistrationWithCustomAttributes")
    public void requestAccessTokenCustomClientAuth3(final String userId, final String userSecret) throws Exception {
        showTitle("requestAccessTokenCustomClientAuth3");

        String username = userId;
        String password = userSecret;
        String scope = "openid";

        TokenRequest request = new TokenRequest(GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);
        request.setUsername(username);
        request.setPassword(password);
        request.setScope(scope);
        request.setAuthUsername(customAttrValue1);
        request.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(request);
        TokenResponse response1 = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");
        assertNotNull(response1.getIdToken(), "The id token is null");
    }
}