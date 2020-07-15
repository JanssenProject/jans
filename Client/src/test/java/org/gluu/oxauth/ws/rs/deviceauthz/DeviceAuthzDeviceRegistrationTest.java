/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ws.rs.deviceauthz;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests for WS used to register device requests.
 */
public class DeviceAuthzDeviceRegistrationTest extends BaseTest {

    @Parameters({"userId", "userSecret", "redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void deviceAuthzHappyFlow() {
        showTitle("deviceAuthzHappyFlow");

        // 1. Register client
        RegisterResponse registerResponse = registerClientForDeviceAuthz();
        String clientId = registerResponse.getClientId();

        // 2. Request device authz
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");

        DeviceAuthzRequest authorizationRequest = new DeviceAuthzRequest(clientId, scopes);
        authorizationRequest.setAuthUsername(clientId);
        authorizationRequest.setAuthPassword(registerResponse.getClientSecret());

        DeviceAuthzClient deviceAuthzClient = new DeviceAuthzClient(deviceAuthzEndpoint);
        deviceAuthzClient.setRequest(authorizationRequest);

        DeviceAuthzResponse response = deviceAuthzClient.exec();

        showClient(deviceAuthzClient);

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
    }

    private RegisterResponse registerClientForDeviceAuthz() {
        List<ResponseType> responseTypes = Collections.singletonList(ResponseType.CODE);

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app", null);
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setGrantTypes(Collections.singletonList(GrantType.DEVICE_CODE));

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

}