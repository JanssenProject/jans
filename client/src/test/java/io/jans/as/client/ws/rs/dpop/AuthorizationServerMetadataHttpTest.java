/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.client.ws.rs.dpop;

import io.jans.as.client.*;
import io.jans.as.client.dev.HostnameVerifierType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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
            openIdConnectDiscoveryResponse = openIdConnectDiscoveryClient.exec(new ApacheHttpClient4Executor(httpClient));
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
