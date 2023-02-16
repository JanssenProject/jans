package io.jans.as.client.ws.rs.token;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.util.Pair;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class NativeSsoHttpTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void nativeSso(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) throws Exception {
        final Pair<String, String> fromApp1 = app1Flow(userId, userSecret, redirectUris, redirectUri, sectorIdentifierUri);

        final String deviceToken = fromApp1.getFirst();
        final String idToken = fromApp1.getSecond();

        app2Flow(deviceToken, idToken, redirectUris, sectorIdentifierUri);
    }

    private Pair<String, String> app1Flow(String userId, String userSecret, String redirectUris, String redirectUri, String sectorIdentifierUri) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, InvalidJwtException {
        showTitle("APP 1 - Perform Authorization Code Flow");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE);
        List<String> scopes = Arrays.asList("openid", "profile", "device_sso", "email");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Request authorization and receive the authorization code.
        String nonce = UUID.randomUUID().toString();
        AuthorizationResponse authorizationResponse = requestAuthorization(userId, userSecret, redirectUri, responseTypes, scopes, clientId, nonce);

        String scope = authorizationResponse.getScope();
        String authorizationCode = authorizationResponse.getCode();

        assertTrue(scope.contains("device_sso")); // make sure device_sso scope is present -> pre-requisite to get device secret

        // 3. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationCode);
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setScope("");
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .notBlankDeviceToken()
                .check();

        final String deviceToken = tokenResponse.getDeviceToken();

        // 4. Validate id_token - ds_hash must be present
        final String idToken = tokenResponse.getIdToken();
        AssertBuilder.jwtParse(idToken)
                .validateSignatureRSAClientEngine(jwksUri, SignatureAlgorithm.RS256)
                .claimsPresence(JwtClaimName.CODE_HASH)
                .notNullAuthenticationTime()
                .notNullJansOpenIDConnectVersion()
                .notNullAuthenticationContextClassReference()
                .notNullAuthenticationMethodReferences()
                .notBlankDsHash()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 6. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.BIRTHDATE, JwtClaimName.GENDER, JwtClaimName.MIDDLE_NAME)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE)
                .check();

        return new Pair<>(deviceToken, idToken);
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

    private void app2Flow(String deviceToken, String idToken, String redirectUris, String sectorIdentifierUri) throws InvalidJwtException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        showTitle("APP 2 - Token Exchange with device_token and id_token");
        showTitle("APP 2 gets device_token and id_token via shared secured storage (here we just emulate it.)");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.ID_TOKEN);
        List<GrantType> grantTypes = Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.TOKEN_EXCHANGE);
        List<String> scopes = Arrays.asList("openid", "profile", "device_sso", "email");

        // 1. Register client
        RegisterResponse registerResponse = registerClient(redirectUris, responseTypes, grantTypes, scopes, sectorIdentifierUri);

        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // 2. Get access_token by sending id_token and deviceToken
        TokenRequest tokenRequest = new TokenRequest(GrantType.TOKEN_EXCHANGE);
        tokenRequest.setAudience(issuer);
        tokenRequest.setScope("openid profile email");
        tokenRequest.setSubjectToken(idToken);
        tokenRequest.setSubjectTokenType("urn:ietf:params:oauth:token-type:id_token");
        tokenRequest.setActorToken(deviceToken);
        tokenRequest.setActorTokenType("urn:x-oath:params:oauth:token-type:device-secret");
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        tokenRequest.setAuthUsername(clientId);
        tokenRequest.setAuthPassword(clientSecret);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showTitle("APP 2:");
        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse)
                .notNullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request user info
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoClient.setExecutor(clientEngine(true));
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);

        showClient(userInfoClient);
        AssertBuilder.userInfoResponse(userInfoResponse)
                .notNullClaimsPersonalData()
                .claimsPresence(JwtClaimName.EMAIL, JwtClaimName.BIRTHDATE, JwtClaimName.GENDER, JwtClaimName.MIDDLE_NAME)
                .claimsPresence(JwtClaimName.NICKNAME, JwtClaimName.PREFERRED_USERNAME, JwtClaimName.PROFILE)
                .check();
    }
}
