/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.manual;

import junit.framework.Assert;
import org.testng.annotations.Test;
import org.gluu.oxauth.client.OpenIdConfigurationClient;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;

import java.io.IOException;

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
        Assert.assertNotNull(response);
    }
}
