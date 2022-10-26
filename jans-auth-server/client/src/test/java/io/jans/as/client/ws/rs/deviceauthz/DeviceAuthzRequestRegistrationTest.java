/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.deviceauthz;

import io.jans.as.client.BaseTest;
import io.jans.as.client.DeviceAuthzClient;
import io.jans.as.client.DeviceAuthzRequest;
import io.jans.as.client.DeviceAuthzResponse;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;

import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.authorize.DeviceAuthzErrorResponseType;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static io.jans.as.model.util.StringUtils.EASY_TO_READ_CHARACTERS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for WS used to register device authz requests.
 */
public class DeviceAuthzRequestRegistrationTest extends BaseTest {

    protected static RegisterResponse registerClientForDeviceAuthz(AuthenticationMethod authenticationMethod,
                                                                   List<GrantType> grantTypes, String redirectUris,
                                                                   String sectorIdentifierUri, String registrationEndpoint) {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email", "phone", "user_name");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setTokenEndpointAuthMethod(authenticationMethod);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);
        registerRequest.setScope(scopes);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        return registerResponse;
    }

    protected static void validateSuccessfulResponse(DeviceAuthzResponse response) {
        final String regex = "[" + EASY_TO_READ_CHARACTERS + "]{4}-[" + EASY_TO_READ_CHARACTERS + "]{4}";
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getUserCode(), "User code is null");
        assertNotNull(response.getDeviceCode(), "Device code is null");
        assertNotNull(response.getInterval(), "Interval is null");
        assertTrue(response.getInterval() > 0, "Interval is null");
        assertNotNull(response.getVerificationUri(), "Verification Uri is null");
        assertNotNull(response.getVerificationUriComplete(), "Verification Uri complete is null");
        assertTrue(response.getVerificationUri().length() > 10, "Invalid verification_uri");
        assertTrue(response.getVerificationUriComplete().length() > 10, "Invalid verification_uri_complete");
        assertNotNull(response.getExpiresIn(), "expires_in is null");
        assertTrue(response.getExpiresIn() > 0, "expires_in contains an invalid value");
        assertTrue(response.getUserCode().matches(regex));
    }

    protected static void validateErrorResponse(DeviceAuthzResponse response, int status, DeviceAuthzErrorResponseType errorType) {
        assertEquals(response.getStatus(), status, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getErrorType(), "Error expected, however no error was found");
        assertNotNull(response.getErrorDescription(), "Error description expected, however no error was found");
        assertEquals(response.getErrorType(), errorType, "Unexpected error");
        assertNull(response.getUserCode(), "User code must not be null");
        assertNull(response.getDeviceCode(), "Device code must not be null");
    }

    /**
     * Verifies normal flow with different scopes, AS should generate user_code, device_code and other data.
     * It uses normal client_secret_basic authentication method.
     */
    @Test
    public void deviceAuthzHappyFlow() {
        showTitle("deviceAuthzHappyFlow");

        // Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz(AuthenticationMethod.CLIENT_SECRET_BASIC,
                Collections.singletonList(GrantType.DEVICE_CODE), null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();

        // 1. OpenId, profile, address and email scopes
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);

        // 2. Only openid scope
        scopes = Collections.singletonList("openid");
        authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);
    }

    /**
     * Verifies normal flow with different scopes, AS should generate user_code, device_code and other data.
     * It uses normal none authentication method, therefore no client authentication is required.
     */
    @Test
    public void deviceAuthzHappyFlowPublicClient() {
        showTitle("deviceAuthzHappyFlowPublicClient");

        // Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz(AuthenticationMethod.NONE,
                Collections.singletonList(GrantType.DEVICE_CODE), null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();

        // 1. OpenId, profile, address and email scopes
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthenticationMethod(AuthenticationMethod.NONE);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);

        // 2. Only openid scope
        scopes = Collections.singletonList("openid");
        authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);
    }

    /**
     * Tests that the device authz request is rejected, since client doesnt support that grant type.
     */
    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void deviceAuthzGrantTypeDoesntSupported(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("deviceAuthzGrantTypeDoesntSupported");

        // Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz(AuthenticationMethod.CLIENT_SECRET_BASIC,
                Collections.singletonList(GrantType.AUTHORIZATION_CODE), redirectUris, sectorIdentifierUri, registrationEndpoint);
        String clientId = registerResponse.getClientId();

        // Device authz request registration
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateErrorResponse(response, 400, DeviceAuthzErrorResponseType.INVALID_GRANT);
    }

    /**
     * AS should authenticate client requests, however these tests are trying to pass device authz requests with
     * wrong client authn data.
     */
    @Test
    public void deviceAuthzNoPublicClientHoweverIncorrectAuthSent() {
        showTitle("deviceAuthzNoPublicClientHoweverIncorrectAuthSent");

        // Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz(AuthenticationMethod.CLIENT_SECRET_BASIC,
                Collections.singletonList(GrantType.DEVICE_CODE), null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();

        // 1. No authentication data sent
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthenticationMethod(AuthenticationMethod.NONE);

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateErrorResponse(response, 401, DeviceAuthzErrorResponseType.INVALID_CLIENT);

        // 2. Invalid authentication
        scopes = Arrays.asList("openid", "profile", "address", "email");
        authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword("invalid-client-id-" + System.currentTimeMillis());

        deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateErrorResponse(response, 401, DeviceAuthzErrorResponseType.INVALID_CLIENT);
    }

    /**
     * Client that doesnt require authn accept device authz requests even client sends authn data.
     */
    @Test
    public void deviceAuthzPublicClientAndAuthSent() {
        showTitle("deviceAuthzPublicClientAndAuthSent");

        // Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz(AuthenticationMethod.NONE,
                Collections.singletonList(GrantType.DEVICE_CODE), null, null, registrationEndpoint);
        String clientId = registerResponse.getClientId();

        // Device authz request
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);
        validateSuccessfulResponse(response);
    }

}