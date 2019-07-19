/*
  All rights reserved -- Copyright 2015 Gluu Inc.
*/
package io.swagger.client.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.GetJwksParams;
import io.swagger.client.model.GetJwksResponse;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static io.swagger.client.api.Tester.api;
import static org.testng.Assert.*;

/**
 * Test for checking JSON Web Key Set functionality
 *
 * @author Shoeb
 * @version 12/02/2018
 */
public class GetJwksTest {

    @Test
    @Parameters({"opHost", "opDiscoveryPath"})
    public void test(String opHost, @Optional String opDiscoveryPath) throws Exception {

        final DevelopersApi client = api();

        final GetJwksParams params = new GetJwksParams();
        params.setOpHost(opHost);
        params.setOpDiscoveryPath(opDiscoveryPath);

        final GetJwksResponse response = client.getJsonWebKeySet(Tester.getAuthorization(), params);
        assertNotNull(response);
        assertNotNull(response.getKeys());
        assertFalse(response.getKeys().isEmpty());

    }

    @Test
    @Parameters({"opDiscoveryPath"})
    public void testWithNoOP(@Optional String opDiscoveryPath) throws Exception {

        final DevelopersApi client = api();

        final GetJwksParams params = new GetJwksParams();
        params.setOpDiscoveryPath(opDiscoveryPath);

        try {
            client.getJsonWebKeySetWithHttpInfo(Tester.getAuthorization(), params);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), 400);
        }

    }

}
