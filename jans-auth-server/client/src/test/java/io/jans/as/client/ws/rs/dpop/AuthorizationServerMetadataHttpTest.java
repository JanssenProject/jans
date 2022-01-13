/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.dpop;

import io.jans.as.client.BaseTest;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import io.jans.as.client.dev.HostnameVerifierType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class AuthorizationServerMetadataHttpTest extends BaseTest {

    @Test
    @Parameters({"swdResource"})
    public void requestOpenIdConfiguration(final String resource) throws Exception {
        showTitle("OpenID Connect Discovery");

        OpenIdConnectDiscoveryClient openIdConnectDiscoveryClient = new OpenIdConnectDiscoveryClient(resource);

        CloseableHttpClient httpClient = createHttpClient(HostnameVerifierType.ALLOW_ALL);
        OpenIdConnectDiscoveryResponse openIdConnectDiscoveryResponse;
        try {
            openIdConnectDiscoveryResponse = openIdConnectDiscoveryClient.exec(new ApacheHttpClient43Engine(httpClient));
        } finally {
            httpClient.close();
        }

        showClient(openIdConnectDiscoveryClient);
        assertEquals(openIdConnectDiscoveryResponse.getStatus(), 200, "Unexpected response code");
        assertNotNull(openIdConnectDiscoveryResponse.getSubject());
        assertTrue(openIdConnectDiscoveryResponse.getLinks().size() > 0);

        String configurationEndpoint = openIdConnectDiscoveryResponse.getLinks().get(0).getHref() +
                "/.well-known/openid-configuration";

        showTitle("OpenID Connect Configuration");

        OpenIdConfigurationClient client = new OpenIdConfigurationClient(configurationEndpoint);
        OpenIdConfigurationResponse response = client.execOpenIdConfiguration();

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code");
        assertNotNull(response.getIssuer(), "The issuer is null");
        assertNotNull(response.getDpopSigningAlgValuesSupported());
        assertTrue(response.getDpopSigningAlgValuesSupported().size() > 0, "The dpop_signing_alg_values_supported is empty");
    }
}
