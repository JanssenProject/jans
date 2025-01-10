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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public class JansLinkConfigResourceTest extends BaseTest {

    @BeforeMethod
    public void before(Method methodName) {
        boolean isServiceDeployed = isDeployed();
        log.error("\n\n\n *** JANS-LINK Plugin isServiceDeployed{}", isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isServiceDeployed) {
            throw new SkipException("JANS-LINK Plugin not deployed");
        }
    }

    private boolean isDeployed() {
        return isServiceDeployed("io.jans.configapi.plugin.link.rest.ApiApplication");
    }

    @Parameters({ "test.issuer", "linkConfigUrl" })
    @Test
    public void fetchLinkConfiguration(final String issuer, final String linkConfigUrl) {
        log.error("\n\n\n Get Link Configuration isDeployed():{} {}", isDeployed(), "\n\n\n");
        if (isDeployed()) {
            log.info("fetchLinkConfiguration() - accessToken:{}, issuer:{}, linkConfigUrl:{}", accessToken, issuer,
                    linkConfigUrl);
            Builder request = getResteasyService().getClientBuilder(issuer + linkConfigUrl);
            request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            Response response = request.get();
            assertEquals(response.getStatus(), Status.OK.getStatusCode());
            log.info("Response for fetchLinkConfiguration -  response:{}", response);
        } else {
            assertTrue(true);
        }
    }

}
