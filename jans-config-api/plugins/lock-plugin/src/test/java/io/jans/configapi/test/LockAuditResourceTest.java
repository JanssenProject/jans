/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.core.test.BaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public class LockAuditResourceTest extends BaseTest {

    // Execute before each test is run
    @BeforeMethod
    public void before(Method methodName) {
        boolean isServiceDeployed = isServiceDeployed("io.jans.configapi.plugin.lock.rest.ApiApplication");
        log.info("\n\n\n *** LockAuditResourceTest - isServiceDeployed:{}", isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be
        // skipped as well
        if (!isServiceDeployed) {
            throw new SkipException("Lock Plugin not deployed");
        }

    }

    @Parameters({ "test.issuer", "lockAuditHealthPostUrl", "audit_health_post_1" })
    @Test
    public void getLockAuditData(final String issuer, final String lockAuditHealthPostUrl, final String json) {
        log.info("getLockAuditData() - accessToken:{}, issuer:{}, lockAuditHealthPostUrl:{}, json:{}", accessToken,
                issuer, lockAuditHealthPostUrl, json);
        Builder request = getResteasyService().getClientBuilder(issuer + lockAuditHealthPostUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.post(Entity.entity(json, MediaType.APPLICATION_JSON));
        log.info("post lock audit -  response:{}", response);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getLockAuditData -  response:{}", response);
    }

    @Parameters({ "test.issuer", "lockAuditHealthSearchUrl" })
    // @Test
    public void getLockAuditData(final String issuer, final String lockAuditHealthSearchUrl) {
        log.info("getLockAuditData() - accessToken:{}, issuer:{}, lockAuditHealthSearchUrl:{}", accessToken, issuer,
                lockAuditHealthSearchUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + lockAuditHealthSearchUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getLockAuditData -  response:{}", response);
    }

}
