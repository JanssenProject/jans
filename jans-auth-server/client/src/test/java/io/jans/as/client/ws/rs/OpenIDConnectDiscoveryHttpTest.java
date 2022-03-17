/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryRequest;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URISyntaxException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Functional tests for SWD Web Services (HTTP)
 *
 * @author Javier Rojas Blum Date: 12.7.2011
 */
public class OpenIDConnectDiscoveryHttpTest extends BaseTest {

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
        assertRequest(resource, expectedHost, null);
    }

    @Test
    public void hostNormalization2() throws Exception {
        String resource = "example.com:8080";
        String expectedHost = "example.com:8080";
        assertRequest(resource, expectedHost, null);
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

    @Parameters({"swdResource"})
    @Test
    public void requestOpenIdConnectDiscovery(final String resource) throws Exception {
        showTitle("requestOpenIdConnectDiscovery");

        OpenIdConnectDiscoveryClient client = new OpenIdConnectDiscoveryClient(resource);
        OpenIdConnectDiscoveryResponse response = client.exec();

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code");
        assertNotNull(response.getSubject());
        assertTrue(response.getLinks().size() > 0);
    }
}