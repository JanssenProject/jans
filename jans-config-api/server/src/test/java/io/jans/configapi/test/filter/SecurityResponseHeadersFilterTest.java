package io.jans.configapi.filters;
/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import io.jans.configapi.core.test.BaseTest;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies that SecurityResponseHeadersFilter applies the standard security
 * headers uniformly, on both a successful (200) response and an
 * unauthorized/error response, so the filter is proven to run regardless of
 * outcome and not just on the happy path.
 *
 * Extends the project's shared BaseTest, so it picks up the same
 * test.properties (token endpoint, client id/secret, grant type) that every
 * other config-api integration test already uses - no new configuration
 * required to run it.
 *
 * NOTE: written to match the conventions visible in the existing
 * io.jans.configapi.test.auth.ClientResourceTest (BaseTest +
 * getAccessTokenForGivenScope + a plain JAX-RS Client). It has not been
 * compiled or run against the real module - verify method names on the
 * actual BaseTest in your checkout (getAccessTokenForGivenScope, propertiesMap
 * keys, base URL property name) before relying on it, they may differ
 * slightly by branch/version.
 */
public class SecurityResponseHeadersFilterTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(SecurityResponseHeadersFilterTest.class);

    // Any endpoint works, since the filter is global; this one already exists
    // and is exercised elsewhere in the test suite (config/jans-auth-server).
    private static final String CONFIG_ENDPOINT_PATH = "/jans-config-api/api/v1/jans-auth-server/config";
    private static final String CONFIG_READ_SCOPE = "https://jans.io/oauth/jans-auth-server/config/properties.readonly";

    private Client client;
    private String issuerUrl;

    @BeforeClass
    public void setupClient() {
        client = ClientBuilder.newClient();
        // propertiesMap / issuer base URL wiring follows BaseTest's existing
        // pattern (see getAccessToken()/getAccessTokenForGivenScope() there);
        // adjust the key name if your BaseTest exposes it differently.
        issuerUrl = propertiesMap.get("issuer");
    }

    @AfterClass
    public void teardownClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void securityHeadersPresentOnSuccessfulResponse() {
        String accessToken = getAccessTokenForGivenScope(CONFIG_READ_SCOPE);
        Assert.assertNotNull(accessToken, "Could not obtain access token for scope " + CONFIG_READ_SCOPE);

        Invocation.Builder request = client.target(issuerUrl + CONFIG_ENDPOINT_PATH)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken);

        try (Response response = request.get()) {
            log.info("GET {} -> {}", CONFIG_ENDPOINT_PATH, response.getStatus());
            assertSecurityHeaders(response);
        }
    }

    @Test
    public void securityHeadersPresentOnUnauthorizedResponse() {
        // Deliberately no Authorization header, to confirm the filter also
        // decorates error/4xx responses coming out of the auth filter chain,
        // not just successful resource output.
        Invocation.Builder request = client.target(issuerUrl + CONFIG_ENDPOINT_PATH)
                .request(MediaType.APPLICATION_JSON);

        try (Response response = request.get()) {
            log.info("Unauthenticated GET {} -> {}", CONFIG_ENDPOINT_PATH, response.getStatus());
            Assert.assertEquals(response.getStatus(), 401, "Expected 401 without a bearer token");
            assertSecurityHeaders(response);
        }
    }

    private void assertSecurityHeaders(Response response) {
        assertHeaderEquals(response, "X-Content-Type-Options", "nosniff");
        assertHeaderEquals(response, "X-Frame-Options", "DENY");
        assertHeaderEquals(response, "Referrer-Policy", "no-referrer");

        String csp = response.getHeaderString("Content-Security-Policy");
        Assert.assertNotNull(csp, "Content-Security-Policy header missing");
        Assert.assertTrue(csp.contains("frame-ancestors 'none'"),
                "Content-Security-Policy should contain frame-ancestors 'none', got: " + csp);
    }

    private void assertHeaderEquals(Response response, String headerName, String expectedValue) {
        String actual = response.getHeaderString(headerName);
        Assert.assertNotNull(actual, headerName + " header missing from response");
        Assert.assertEquals(actual, expectedValue,
                headerName + " header had unexpected value");
    }
}