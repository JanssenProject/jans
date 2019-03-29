/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interop;

import static org.testng.Assert.assertEquals;

import org.gluu.oxauth.client.OpenIdConnectDiscoveryRequest;
import org.testng.annotations.Test;

/**
 * OC5:FeatureTest-Can Discover Identifiers using E-Mail Syntax
 *
 * @author Javier Rojas Blum Date: 09.03.2013
 */
public class CanDiscoverIdentifiersUsingEMailSyntax {

    @Test
    public void emailNormalization1() throws Exception {
        String resource = "acct:joe@example.com";
        String expectedHost = "example.com";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void emailNormalization2() throws Exception {
        String resource = "joe@example.com";
        String expectedHost = "example.com";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void emailNormalization3() throws Exception {
        String resource = "acct:joe@example.com:8080";
        String expectedHost = "example.com:8080";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void emailNormalization4() throws Exception {
        String resource = "joe@example.com:8080";
        String expectedHost = "example.com:8080";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void emailNormalization5() throws Exception {
        String resource = "joe@localhost";
        String expectedHost = "localhost";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void emailNormalization6() throws Exception {
        String resource = "joe@localhost:8080";
        String expectedHost = "localhost:8080";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }
}