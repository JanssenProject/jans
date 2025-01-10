/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.lock.test;

import io.jans.configapi.lock.LockBaseTest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class LockAuditResourceTest extends LockBaseTest {

    @Parameters({ "test.issuer", "lockAuditHealthPostUrl", "audit_health_post_1" })
    @Test
    public void getLockAuditData(final String issuer, final String lockAuditHealthPostUrl, final String json) {
        log.info("getLockAuditData() - accessToken:{}, issuer:{}, lockAuditHealthPostUrl:{}, json:{}", accessToken,
                issuer, lockAuditHealthPostUrl, json);
        before();
        Builder request = getResteasyService().getClientBuilder(issuer + lockAuditHealthPostUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.post(Entity.entity(json, MediaType.APPLICATION_JSON));
        log.info("post lock audit -  response:{}", response);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());

    }

    @Parameters({ "test.issuer", "lockAuditHealthSearchUrl" })
    // @Test
    public void getLockAuditData(final String issuer, final String lockAuditHealthSearchUrl) {
        log.info("getLockAuditData() - accessToken:{}, issuer:{}, lockAuditHealthSearchUrl:{}", accessToken, issuer,
                lockAuditHealthSearchUrl);
        before();
        Builder request = getResteasyService().getClientBuilder(issuer + lockAuditHealthSearchUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("Response for getLockAuditData -  response:{}", response);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

}
