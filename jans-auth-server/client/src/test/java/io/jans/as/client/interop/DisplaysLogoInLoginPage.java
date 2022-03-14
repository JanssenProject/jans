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
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * OC5:FeatureTest-Displays Logo in Login Page
 *
 * @author Javier Rojas Blum
 * @version November 3, 2016
 */
public class DisplaysLogoInLoginPage extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void displaysLogoInLoginPage(final String redirectUris, final String redirectUri,
                                        final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Displays Logo in Login Page");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        String logoUri = "http://www.gluu.org/wp-content/themes/gluursn/images/logo.png";

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setLogoUri(logoUri);
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

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(responseTypes, clientId, scopes,
                redirectUri, null);
        authorizationRequest.setState(state);

        String authorizationRequestUrl = getAuthorizationEndpoint() + "?" + authorizationRequest.getQueryString();

        AuthorizeClient authorizeClient = new AuthorizeClient(getAuthorizationEndpoint());
        authorizeClient.setRequest(authorizationRequest);

        try {
            startSelenium();
            navigateToAuhorizationUrl(driver, authorizationRequestUrl);
            WebElement logo = driver.findElement(By.id("AppLogo"));
            assertNotNull(logo);
        } catch (NoSuchElementException ex) {
            fail("Logo not found");
        } finally {
            stopSelenium();
        }
    }
}