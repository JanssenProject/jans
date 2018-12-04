/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package org.xdi.oxd.server;

import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.common.params.GetJwksParams;
import org.xdi.oxd.common.response.GetJwksResponse;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * Test for checking JSON Web Key Set functionality
 *
 * @author Shoeb
 * @version 12/01/2018
 */

public class GetJwksTest {


    @Test(enabled = false)
    @Parameters({"host", "opHost", "opDiscoveryPath"})
    public void test(String host, String opHost, @Optional String opDiscoveryPath) {

        final ClientInterface client = Tester.newClient(host);

        final GetJwksParams params = new GetJwksParams();
        params.setOpHost(opHost);
        params.setOpDiscoveryPath(opDiscoveryPath);

        final GetJwksResponse response = client.getJwks(Tester.getAuthorization(), params);
        assertNotNull(response);
        assertNotNull(response.getKeys());
        assertFalse(response.getKeys().isEmpty());

    }
}
