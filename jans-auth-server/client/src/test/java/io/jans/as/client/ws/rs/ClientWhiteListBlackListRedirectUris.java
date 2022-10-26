/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.util.StringUtils;
import io.jans.as.model.util.URLPatternList;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.List;


import static io.jans.as.model.register.RegisterRequestParam.SCOPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version November 29, 2017
 */
public class ClientWhiteListBlackListRedirectUris extends BaseTest {

    private String registrationAccessToken1;
    private String registrationClientUri1;

    @Test
    public void testUrlPatterList() {
        showTitle("testUrlPatterList");

        List<String> urlPatterns = Arrays.asList(
                "*.gluu.org/foo*bar",
                "https://example.org/foo/bar.html",
                "*.attacker.com/*");

        URLPatternList urlPatternList = new URLPatternList(urlPatterns);
        assertFalse(urlPatternList.isUrlListed("gluu.org"));
        assertFalse(urlPatternList.isUrlListed("www.gluu.org"));
        assertTrue(urlPatternList.isUrlListed("http://gluu.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("https://mail.gluu.org/foo/bar"));
        assertTrue(urlPatternList.isUrlListed("http://www.gluu.org/foobar"));
        assertTrue(urlPatternList.isUrlListed("https://www.gluu.org/foo/baz/bar"));
        assertFalse(urlPatternList.isUrlListed("http://example.org"));
        assertFalse(urlPatternList.isUrlListed("http://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("https://example.org/foo/bar.html"));
        assertTrue(urlPatternList.isUrlListed("http://attacker.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.attacker.com"));
        assertTrue(urlPatternList.isUrlListed("https://www.attacker.com/foo/bar"));
    }

    @Test
    public void requestClientAssociateInBlackList() throws Exception {
        showTitle("requestClientAssociateInBlackList");

        final String redirectUris = "https://www.attacker.com";

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        RegisterResponse response = registerClient.execRegister(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));

        showClient(registerClient);
        AssertBuilder.registerResponse(response).bad().check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void requestClientAssociate(final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("requestClientAssociate");

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientEngine(true));
        RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).created().check();
        assertNotNull(response.getClaims().get(SCOPE.toString()));

        registrationAccessToken1 = response.getRegistrationAccessToken();
        registrationClientUri1 = response.getRegistrationClientUri();
    }

    @Test(dependsOnMethods = "requestClientAssociate")
    public void requestClientUpdate() throws Exception {
        showTitle("requestClientUpdate");

        final String redirectUris = "https://www.attacker.com";

        final RegisterRequest registerRequest = new RegisterRequest(registrationAccessToken1);
        registerRequest.setHttpMethod(HttpMethod.PUT);
        registerRequest.setRedirectUris(StringUtils.spaceSeparatedToList(redirectUris));

        final RegisterClient registerClient = new RegisterClient(registrationClientUri1);
        registerClient.setRequest(registerRequest);
        registerClient.setExecutor(clientEngine(true));
        final RegisterResponse response = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(response).bad().check();
    }
}