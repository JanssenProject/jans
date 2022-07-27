/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import io.jans.as.client.client.Asserter;
import io.jans.as.client.dev.HostnameVerifierType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Functional tests for OpenId Configuration Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version July 10, 2019
 */
public class ConfigurationRestWebServiceHttpTest extends BaseTest {

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
        Asserter.assertOpenIdConfigurationResponse(response);

        assertTrue(response.getResponseModesSupported().size() > 0, "The responseModesSupported is empty");
        assertTrue(response.getGrantTypesSupported().size() > 0, "The grantTypesSupported is empty");
        assertTrue(response.getSubjectTypesSupported().size() > 0, "The subjectTypesSupported is empty");
        assertTrue(response.getUserInfoSigningAlgValuesSupported().size() > 0, "The userInfoSigningAlgValuesSupported is empty");
        assertTrue(response.getUserInfoEncryptionAlgValuesSupported().size() > 0, "The userInfoEncryptionAlgValuesSupported is empty");
        assertTrue(response.getUserInfoEncryptionEncValuesSupported().size() > 0, "The userInfoEncryptionEncValuesSupported is empty");
        assertTrue(response.getIdTokenSigningAlgValuesSupported().size() > 0, "The idTokenSigningAlgValuesSupported is empty");
        assertTrue(response.getIdTokenEncryptionAlgValuesSupported().size() > 0, "The idTokenEncryptionAlgValuesSupported is empty");
        assertTrue(response.getIdTokenEncryptionEncValuesSupported().size() > 0, "The idTokenEncryptionEncValuesSupported is empty");
        assertTrue(response.getRequestObjectSigningAlgValuesSupported().size() > 0, "The requestObjectSigningAlgValuesSupported is empty");
        assertTrue(response.getRequestObjectEncryptionAlgValuesSupported().size() > 0, "The requestObjectEncryptionAlgValuesSupported is empty");
        assertTrue(response.getRequestObjectEncryptionEncValuesSupported().size() > 0, "The requestObjectEncryptionEncValuesSupported is empty");

        assertTrue(response.getDisplayValuesSupported().size() > 0, "The displayValuesSupported is empty");
        assertTrue(response.getClaimTypesSupported().size() > 0, "The claimTypesSupported is empty");
        assertNotNull(response.getServiceDocumentation(), "The serviceDocumentation is null");
        assertTrue(response.getClaimsLocalesSupported().size() > 0, "The claimsLocalesSupported is empty");
        assertTrue(response.getUiLocalesSupported().size() > 0, "The uiLocalesSupported is empty");
        assertTrue(response.getClaimsParameterSupported(), "The claimsParameterSupported is false");
        assertTrue(response.getRequestParameterSupported(), "The requestParameterSupported is false");
        assertTrue(response.getRequestUriParameterSupported(), "The requestUriParameterSupported is false");
        assertFalse(response.getRequireRequestUriRegistration(), "The requireRequestUriRegistration is true");

        // Jans Auth #917: Add dynamic scopes and claims to discovery
        Map<String, List<String>> scopeToClaims = response.getScopeToClaimsMapping();
        List<String> scopesSupported = response.getScopesSupported();
        List<String> claimsSupported = response.getClaimsSupported();
        for (Map.Entry<String, List<String>> scopeEntry : scopeToClaims.entrySet()) {
            assertTrue(scopesSupported.contains(scopeEntry.getKey()),
                    "The scopes supported list does not contain the scope: " + scopeEntry.getKey());
            for (String claimEntry : scopeEntry.getValue()) {
                assertTrue(claimsSupported.contains(claimEntry),
                        "The claims supported list does not contain the claim: " + claimEntry);
            }
        }
    }
}