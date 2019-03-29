/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.interop;

import static org.testng.Assert.assertEquals;

import org.gluu.oxauth.client.OpenIdConnectDiscoveryRequest;
import org.testng.annotations.Test;

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
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization2() throws Exception {
        String resource = "https://example.com/joe";
        String expectedHost = "example.com";
        String expectedPath = "/joe";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization3() throws Exception {
        String resource = "https://example.com:8080/";
        String expectedHost = "example.com:8080";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization4() throws Exception {
        String resource = "https://example.com:8080/joe";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization5() throws Exception {
        String resource = "https://example.com:8080/joe#fragment";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization6() throws Exception {
        String resource = "https://example.com:8080/joe?param=value";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void urlNormalization7() throws Exception {
        String resource = "https://example.com:8080/joe?param1=foo&param2=bar#fragment";
        String expectedHost = "example.com:8080";
        String expectedPath = "/joe";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void hostNormalization1() throws Exception {
        String resource = "example.com";
        String expectedHost = "example.com";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void hostNormalization2() throws Exception {
        String resource = "example.com:8080";
        String expectedHost = "example.com:8080";
        String expectedPath = null;

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void hostNormalization3() throws Exception {
        String resource = "example.com/path";
        String expectedHost = "example.com";
        String expectedPath = "/path";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }

    @Test
    public void hostNormalization4() throws Exception {
        String resource = "example.com:8080/path";
        String expectedHost = "example.com:8080";
        String expectedPath = "/path";

        OpenIdConnectDiscoveryRequest openIdConnectDiscoveryRequest = new OpenIdConnectDiscoveryRequest(resource);
        assertEquals(openIdConnectDiscoveryRequest.getResource(), resource);
        assertEquals(openIdConnectDiscoveryRequest.getHost(), expectedHost);
        assertEquals(openIdConnectDiscoveryRequest.getPath(), expectedPath);
    }
}