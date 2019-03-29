/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.oxauth.model.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * OC5:FeatureTest-Displays Logo in Login Page
 *
 * @author Javier Rojas Blum
 * @version November 3, 2016
 */
public class DisplaysLogoInLoginPage extends BaseTest {

    @Parameters({"redirectUris", "redirectUri", "sectorIdentifierUri"})
    @Test
    public void displaysLogoInLoginPage(final String redirectUris, final String redirectUri, final String sectorIdentifierUri) throws Exception {
        showTitle("OC5:FeatureTest-Displays Logo in Login Page");

        List<ResponseType> responseTypes = Arrays.asList(ResponseType.CODE);
        String logoUri = "http://www.gluu.org/wp-content/themes/gluursn/images/logo.png";

        // 1. Register client
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "oxAuth test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setResponseTypes(responseTypes);
        registerRequest.setLogoUri(logoUri);
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

            WebElement logo = driver.findElement(By.xpath("//img[@src='" + logoUri + "']"));
            assertNotNull(logo);
        } catch (NoSuchElementException ex) {
            fail("Logo not found");
        } finally {
            stopSelenium();
        }
    }
}