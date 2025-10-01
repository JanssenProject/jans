package io.jans.as.client.ws.rs.revoke;

import com.google.common.collect.Lists;
import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.SubjectType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * @author Yuriy Z
 */
public class GlobalTokenRevocationHttpTest extends BaseTest {

    @Parameters({"redirectUris", "userId", "userSecret", "redirectUri"})
    @Test
    public void globalTokenRevocation(
            final String redirectUris, final String userId, final String userSecret, final String redirectUri) {
        showTitle("globalTokenRevocation");

        final AuthenticationMethod authnMethod = AuthenticationMethod.CLIENT_SECRET_BASIC;

        // 1. Register client
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        registerRequest.setTokenEndpointAuthMethod(authnMethod);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setScope(Lists.newArrayList("global_token_revocation", "openid"));
        registerRequest.setSubjectType(SubjectType.PUBLIC);

        RegisterClient registerClient = newRegisterClient(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        // 3. Request authorization
        List<String> scopes = Arrays.asList(
                "openid",
                "profile",
                "address",
                "email");
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, registerResponse.getClientId(), scopes, redirectUri, nonce);
        authorizationRequest.setState(state);

        AuthorizationResponse authorizationResponse = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest, userId, userSecret);

        assertNotNull(authorizationResponse.getLocation(), "The location is null");
        assertNotNull(authorizationResponse.getCode(), "The authorization code is null");

        // 4. Request access token using the authorization code.
        TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
        tokenRequest.setCode(authorizationResponse.getCode());
        tokenRequest.setRedirectUri(redirectUri);
        tokenRequest.setAuthUsername(registerResponse.getClientId());
        tokenRequest.setAuthPassword(registerResponse.getClientSecret());
        tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        TokenClient tokenClient = newTokenClient(tokenRequest);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();
        showClient(tokenClient);

        // 5. request User Info with access token
        UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
        UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(tokenResponse.getAccessToken());
        showClient(userInfoClient);

        assertNull(userInfoResponse.getErrorType()); // no error

        // 6. revoke token by user uid=userId
        GlobalTokenRevocationClientRequest revocationRequest = new GlobalTokenRevocationClientRequest();
        revocationRequest.setFormat("uid");
        revocationRequest.setId(userId);
        revocationRequest.setAuthUsername(registerResponse.getClientId());
        revocationRequest.setAuthPassword(registerResponse.getClientSecret());
        revocationRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);

        GlobalTokenRevocationClient globalTokenRevocationClient = new GlobalTokenRevocationClient(globalTokenRevocationEndpoint);
        globalTokenRevocationClient.exec(revocationRequest);
        showClient(globalTokenRevocationClient);

        // 7. request User Info with access token which is revoked -> error type is not null
        userInfoClient = new UserInfoClient(userInfoEndpoint);
        userInfoResponse = userInfoClient.execUserInfo(tokenResponse.getAccessToken());
        showClient(userInfoClient);

        assertNotNull(userInfoResponse.getErrorType()); // no error
    }
}
