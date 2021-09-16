/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.jarm;

import io.jans.as.client.BaseTest;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import io.jans.as.client.dev.HostnameVerifierType;
import io.jans.as.model.common.ResponseMode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version July 28, 2021
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
        assertTrue(response.getResponseModesSupported().size() > 0, "The responseModesSupported is empty");
        assertTrue(response.getResponseModesSupported().contains(ResponseMode.QUERY_JWT.toString()));
        assertTrue(response.getResponseModesSupported().contains(ResponseMode.FRAGMENT_JWT.toString()));
        assertTrue(response.getResponseModesSupported().contains(ResponseMode.FORM_POST_JWT.toString()));
        assertTrue(response.getResponseModesSupported().contains(ResponseMode.JWT.toString()));
        assertTrue(response.getAuthorizationSigningAlgValuesSupported().size() > 0, "The authorizationSigningAlgValuesSupported is empty");
        assertTrue(response.getAuthorizationEncryptionAlgValuesSupported().size() > 0, "The authorizationEncryptionAlgValuesSupported is empty");
        assertTrue(response.getAuthorizationEncryptionEncValuesSupported().size() > 0, "The authorizationEncryptionEncValuesSupported is empty");
    }
}
