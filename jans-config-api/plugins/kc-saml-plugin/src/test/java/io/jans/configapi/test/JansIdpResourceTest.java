/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.core.test.BaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.lang.reflect.Method;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public class JansIdpResourceTest extends BaseTest {

    // Execute before each test is run
    @BeforeMethod
    public void before(Method methodName) {
        boolean isServiceDeployed = isServiceDeployed("io.jans.configapi.plugin.saml.rest.ApiApplication");
        log.info("\n\n\n *** JansIdpResourceTest - isServiceDeployed:{}", isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isServiceDeployed) {
            throw new SkipException("KC-SAML Plugin not deployed");
        }
    }

    @Parameters({ "test.issuer", "samlIdpUrl" })
    @Test
    public void getKcSAMLIdp(final String issuer, final String samlIdpUrl) {
        log.info("getKcSAMLIdp() - accessToken:{}, issuer:{}, samlIdpUrl:{}", accessToken, issuer, samlIdpUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + samlIdpUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getKcSAMLIdp -  response:{}", response);
    }

}
