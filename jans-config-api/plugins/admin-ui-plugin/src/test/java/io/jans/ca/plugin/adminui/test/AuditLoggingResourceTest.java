/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.plugin.adminui.test;

import io.jans.as.common.model.registration.Client;

import static io.restassured.RestAssured.given;
import io.jans.ca.plugin.adminui.AdminUIBaseTest;
import io.jans.model.net.HttpServiceResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.Map;

import org.apache.http.entity.ContentType;
import static org.testng.Assert.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class AuditLoggingResourceTest extends AdminUIBaseTest{

   
    /**
     *  Testing Audit Logging endpoint
     */	 
    @Parameters({"issuer", "auditLoggingURL", "audit_post_1"})
    @Test
    public void postAuditLoggingData(final String issuer, final String auditLoggingURL, final String json) {
        log.error("postAuditLoggingData() - accessToken:{}, issuer:{}, auditLoggingURL:{}, json:{}", accessToken, issuer, auditLoggingURL, json);
        Builder request = getResteasyService().getClientBuilder(issuer+auditLoggingURL);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        
        Response response = request.post(Entity.entity(json, MediaType.APPLICATION_JSON));
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.error("Response for assertEquals(response.getStatus(), Status.OK.getStatusCode()); -  response:{}", response);
    }
	

  
}
