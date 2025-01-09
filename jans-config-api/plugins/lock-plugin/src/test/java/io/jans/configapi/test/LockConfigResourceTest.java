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

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public class LockConfigResourceTest extends BaseTest {

    // Execute before each test is run
    @BeforeMethod
    public void before(Method methodName) {
        boolean isServiceDeployed = isServiceDeployed("io.jans.configapi.plugin.lock.rest.ApiApplication");
        log.info("\n\n\n *** LockConfigResourceTest - isServiceDeployed:{}", isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isServiceDeployed) {
            throw new SkipException("Lock Plugin not deployed");
        }

    }

    @Parameters({ "test.issuer", "lockConfigUrl" })
    @Test
    public void getLockConfigUrlData(final String issuer, final String lockConfigUrl) {
        log.info("getLockConfigUrlData() - accessToken:{}, issuer:{}, lockConfigUrl:{}", accessToken, issuer,
                lockConfigUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + lockConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getLockConfigUrlData -  response:{}", response);
    }

}
