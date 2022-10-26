/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

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

import static org.testng.Assert.fail;

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
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setPolicyUri(policyUri);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

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
            navigateToAuhorizationUrl(driver, authorizationRequestUrl);

//            WebElement policy = driver.findElement(By.xpath("//a[@href='" + policyUri + "']"));
//            assertNotNull(policy);
        } catch (Exception ex) {
            fail("Policy not found");
        } finally {
            stopSelenium();
        }
    }
}