/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server.manual;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class oxAuthDiscoveryTest {

    @Test
    public void discoveryCallByOxAuthClient() throws IOException {
        String url = "https://ce-dev.gluu.org/.well-known/openid-configuration";
        OpenIdConfigurationClient client = new OpenIdConfigurationClient(url);
        OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
        System.out.println(response.getEntity());
        assertNotNull(response);
    }
}
