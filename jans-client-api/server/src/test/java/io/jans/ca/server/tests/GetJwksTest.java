/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package io.jans.ca.server.tests;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.common.params.GetJwksParams;
import io.jans.ca.common.response.GetJwksResponse;
import io.jans.ca.server.Tester;
import io.jans.ca.server.arquillian.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * Test for checking JSON Web Key Set functionality
 *
 * @author Shoeb
 * @version 12/01/2018
 */

public class GetJwksTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Test(enabled = false)
    @Parameters({"host", "opHost", "opDiscoveryPath"})
    public void test(String host, String opHost, @Optional String opDiscoveryPath) {

        final ClientInterface client = getClientInterface(url);

        final GetJwksParams params = new GetJwksParams();
        params.setOpHost(opHost);
        params.setOpDiscoveryPath(opDiscoveryPath);

        final GetJwksResponse response = client.getJwks(Tester.getAuthorization(client.getApitargetURL()), null, params);
        assertNotNull(response);
        assertNotNull(response.getKeys());
        assertFalse(response.getKeys().isEmpty());
    }

    @Test
    @Parameters({"host", "opConfigurationEndpoint"})
    public void test_withOpConfigurationEndpoint(String host, String opConfigurationEndpoint) {

        final ClientInterface client = getClientInterface(url);

        final GetJwksParams params = new GetJwksParams();
        params.setOpConfigurationEndpoint(opConfigurationEndpoint);

        final GetJwksResponse response = client.getJwks(Tester.getAuthorization(client.getApitargetURL()), null, params);
        assertNotNull(response);
        assertNotNull(response.getKeys());
        assertFalse(response.getKeys().isEmpty());
    }
}
