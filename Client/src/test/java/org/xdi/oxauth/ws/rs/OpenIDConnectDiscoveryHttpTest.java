/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.gluu.oxauth.client.OpenIdConnectDiscoveryClient;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryRequest;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryResponse;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

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