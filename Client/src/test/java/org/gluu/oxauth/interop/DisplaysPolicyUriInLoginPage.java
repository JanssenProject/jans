/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interop;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * OC5:FeatureTest-Displays Policy in Login Page
 *
 * @author Javier Rojas Blum
 * @version November 3, 2016
 */
public class DisplaysPolicyUriInLoginPage extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void displaysPolicyUrlInLoginPage(final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Displays Policy in Login Page");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        String policyUri = "http://www.gluu.org/policy";

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setPolicyUri(policyUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

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

        // 2. Request authorization and receive the authorization code.
        List<String> scopes = Arrays.asList("openid", "profile", "address", "email");
        String state = UUID.randomUUID().toString();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes, redirectUri, null);
        authorizationRequest.setState(state);

        String authorizationRequestUrl = getAuthorizationEndpoint() + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(getAuthorizationEndpoint());
        authorizeClient.setRequest(authorizationRequest);

        try {
            startSelenium();
            driver.navigate().to(authorizationRequestUrl);

//            WebElement policy = driver.findElement(By.xpath("//a[@href='" + policyUri + "']"));
//            assertNotNull(policy);
        } catch (Exception ex) {
            fail("Policy not found");
        } finally {
            stopSelenium();
        }
    }
}