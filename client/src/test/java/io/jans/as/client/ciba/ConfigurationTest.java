/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba;

import io.jans.as.client.BaseTest;
import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import io.jans.as.client.dev.HostnameVerifierType;
import io.jans.as.model.common.GrantType;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class ConfigurationTest extends BaseTest {

    @Test
    @Parameters({"swdResource"})
    public void requestOpenIdConfiguration(final String resource) throws Exception {
        showTitle("OpenID Connect Discovery");

        OpenIdConnectDiscoveryClient openIdConnectDiscoveryClient = new OpenIdConnectDiscoveryClient(resource);
        OpenIdConnectDiscoveryResponse openIdConnectDiscoveryResponse = openIdConnectDiscoveryClient.exec(
                new ApacheHttpClient4Executor(createHttpClient(HostnameVerifierType.ALLOW_ALL)));

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
        assertNotNull(response.getAuthorizationEndpoint(), "The authorizationEndpoint is null");
        assertNotNull(response.getTokenEndpoint(), "The tokenEndpoint is null");
        assertNotNull(response.getRevocationEndpoint(), "The tokenRevocationEndpoint is null");
        assertNotNull(response.getUserInfoEndpoint(), "The userInfoEndPoint is null");
        assertNotNull(response.getEndSessionEndpoint(), "The endSessionEndpoint is null");
        assertNotNull(response.getJwksUri(), "The jwksUri is null");
        assertNotNull(response.getRegistrationEndpoint(), "The registrationEndpoint is null");

        assertTrue(response.getGrantTypesSupported().size() > 0, "The grantTypesSupported is empty");
        assertTrue(response.getGrantTypesSupported().contains(GrantType.CIBA.getParamName()), "The grantTypes urn:openid:params:grant-type:ciba is null");

        assertNotNull(response.getBackchannelAuthenticationEndpoint(), "The backchannelAuthenticationEndpoint is null");
        assertTrue(response.getBackchannelTokenDeliveryModesSupported().size() > 0, "The backchannelTokenDeliveryModesSupported is empty");
        assertTrue(response.getBackchannelAuthenticationRequestSigningAlgValuesSupported().size() > 0, "The backchannelAuthenticationRequestSigningAlgValuesSupported is empty");
        assertNotNull(response.getBackchannelUserCodeParameterSupported(), "The backchannelUserCodeParameterSupported is null");
    }
}
