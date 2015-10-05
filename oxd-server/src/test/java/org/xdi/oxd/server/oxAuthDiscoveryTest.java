/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import junit.framework.Assert;
import org.testng.annotations.Test;
import org.xdi.oxauth.client.OpenIdConfigurationClient;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class oxAuthDiscoveryTest {

    @Test
    public void discoveryCallByOxAuthClient() {
        String url = "https://ce-dev.gluu.org/.well-known/openid-configuration";
        OpenIdConfigurationClient client = new OpenIdConfigurationClient(url);
        OpenIdConfigurationResponse response = client.execOpenIdConfiguration();
        System.out.println(response.getEntity());
        Assert.assertNotNull(response);
    }
}
