package io.jans.as.client.ws.rs;

import io.jans.as.client.*;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;

/**
 * Integration tests to validate redirect uris regex behavior
 *
 */
public class AuthorizationAcrValuesTest extends BaseTest {

    /**
     * This method is used to test when acr_values is not send in Authentication URL
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAcrValues_NoAcrsValues_NotNull(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {
        showTitle("requestAuthorizationAcrValues_NoAcrsValues_NotNull");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 3. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest1.setState(state);
        authorizationRequest1.setNonce(nonce);

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertNotNull(authorizationResponse1.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope must be null");
    }

    /**
     * This method is used to test when acr_values is sent in Authentication URL
     */
    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void requestAuthorizationAcrValues_withBasic_NotNull(
            final String userId, final String userSecret, final String redirectUris, final String redirectUri,
            final String sectorIdentifierUri) {

        showTitle("requestAuthorizationAcrValues_withBasic_NotNull");

        List<ResponseType> responseTypes = Arrays.asList(
                ResponseType.CODE,
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request authorization
        responseTypes = Arrays.asList(
                ResponseType.TOKEN,
                ResponseType.ID_TOKEN);

        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String nonce = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest1 = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, nonce);
        authorizationRequest1.setState(state);
        authorizationRequest1.setNonce(nonce);
        authorizationRequest1.setAcrValues(Arrays.asList("basic") );

        AuthorizationResponse authorizationResponse1 = authenticateResourceOwnerAndGrantAccess(
                authorizationEndpoint, authorizationRequest1, userId, userSecret);

        assertNotNull(authorizationResponse1.getLocation(), "The location is null");
        assertNotNull(authorizationResponse1.getAccessToken(), "The access token is null");
        assertNotNull(authorizationResponse1.getState(), "The state is null");
        assertNotNull(authorizationResponse1.getTokenType(), "The token type is null");
        assertNotNull(authorizationResponse1.getExpiresIn(), "The expires in value is null");
        assertNotNull(authorizationResponse1.getScope(), "The scope must be null");
    }
}
