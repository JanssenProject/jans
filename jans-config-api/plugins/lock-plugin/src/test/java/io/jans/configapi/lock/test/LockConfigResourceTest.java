/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.lock.test;

import io.jans.configapi.core.test.BaseTest;
import io.jans.configapi.lock.LockBaseTest;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class LockConfigResourceTest extends LockBaseTest {

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
