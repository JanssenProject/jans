/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import java.lang.reflect.Method;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

public class LockAuditResourceTest extends ConfigServerBaseTest{
    
    // Execute before each test is run
    @BeforeMethod
    public void before(Method methodName){
        boolean isServiceDeployed = isServiceDeployed("io.jans.configapi.plugin.lock.rest.ApiApplication");
          log.error("\n\n\n *** LockAuditResourceTest - isServiceDeployed:{}",isServiceDeployed);
        // check condition, note once you condition is met the rest of the tests will be skipped as well
        if(!isServiceDeployed) {
            throw new SkipException("Lock Plugin not deployed");
        }
    
    }   
    

    @Parameters({"issuer", "lockAuditUrl"})
    @Test
    public void getLockAuditData(final String issuer, final String lockAuditUrl) {
        log.error("getLockAuditData() - accessToken:{}, issuer:{}, lockAuditUrl:{}", accessToken, issuer, lockAuditUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + lockAuditUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for getLockAuditData -  response:{}", response);
    }


}
