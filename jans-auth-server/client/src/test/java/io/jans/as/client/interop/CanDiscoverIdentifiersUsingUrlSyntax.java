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
 * OC5:FeatureTest-Can Discover Identifiers using URL Syntax
 *
 * @author Javier Rojas Blum Date: 09.03.2013
 */
public class CanDiscoverIdentifiersUsingUrlSyntax {

    @Test
    public void urlNormalization1() throws Exception {
        String resource = "https://example.com";
        String expectedHost = "example.com";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void urlNormalization2() throws Exception {
        String resource = "https://example.com/joe";
        String expectedHost = "example.com";
        String expectedPath = "/joe";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void urlNormalization3() throws Exception {
        String resource = "https://example.com:8080/";
        String expectedHost = "example.com:8080";
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void urlNormalization4() throws Exception {
        String resource = "https://example.com:8080/joe";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void urlNormalization5() throws Exception {
        String resource = "https://example.com:8080/joe#fragment";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void urlNormalization6() throws Exception {
        String resource = "https://example.com:8080/joe?param=value";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void urlNormalization7() throws Exception {
        String resource = "https://example.com:8080/joe?param1=foo&param2=bar#fragment";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void hostNormalization1() throws Exception {
        String resource = "example.com";
        String expectedHost = "example.com";
        String expectedPath = null;
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void hostNormalization2() throws Exception {
        String resource = "example.com:8080";
        String expectedHost = "example.com:8080";
        String expectedPath = null;
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void hostNormalization3() throws Exception {
        String resource = "example.com/path";
        String expectedHost = "example.com";
        String expectedPath = "/path";
        assertRequest(resource, expectedHost, expectedPath);
    }

    @Test
    public void hostNormalization4() throws Exception {
        String resource = "example.com:8080/path";
        String expectedHost = "example.com:8080";
        String expectedPath = "/path";
        assertRequest(resource, expectedHost, expectedPath);
    }

    private void assertRequest(String resource, String expectedHost, String expectedPath) throws URISyntaxException {
        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }
}