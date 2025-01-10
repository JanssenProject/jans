/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test;

import io.jans.configapi.saml.KcSAMLBaseTest;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;

public class JansIdpResourceTest extends KcSAMLBaseTest {

    @Parameters({ "test.issuer", "samlIdpUrl" })
    @Test
    public void getKcSAMLIdp(final String issuer, final String samlIdpUrl) {
        log.info("getKcSAMLIdp() - accessToken:{}, issuer:{}, samlIdpUrl:{}", accessToken, issuer, samlIdpUrl);
       
        before();
        Builder request = getResteasyService().getClientBuilder(issuer + samlIdpUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        Response response = request.get();
        log.info("Response for getKcSAMLIdp -  response:{}", response);
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

}
