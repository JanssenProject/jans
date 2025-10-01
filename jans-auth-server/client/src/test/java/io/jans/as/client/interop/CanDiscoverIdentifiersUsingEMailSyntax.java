/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.interop;

import io.jans.as.client.OpenIdConnectDiscoveryRequest;
import org.testng.annotations.Test;

import java.net.URISyntaxException;

import static org.testng.Assert.assertEquals;

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
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void emailNormalization2() throws Exception {
        String resource = "joe@example.com";
        String expectedHost = "example.com";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void emailNormalization3() throws Exception {
        String resource = "acct:joe@example.com:8080";
        String expectedHost = "example.com:8080";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void emailNormalization4() throws Exception {
        String resource = "joe@example.com:8080";
        String expectedHost = "example.com:8080";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void emailNormalization5() throws Exception {
        String resource = "joe@localhost";
        String expectedHost = "localhost";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void emailNormalization6() throws Exception {
        String resource = "joe@localhost:8080";
        String expectedHost = "localhost:8080";
        assertRequest(resource, expectedHost, null);
    }

    private void assertRequest(String resource, String expectedHost, String expectedPath) throws URISyntaxException {
        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

}