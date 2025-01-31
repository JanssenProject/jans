/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.test;

import io.jans.configapi.plugin.saml.KcSAMLBaseTest;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class JansKcSAMLTrustRelationshipTest extends KcSAMLBaseTest {

    @Parameters({ "test.issuer", "samlTrustRelationshipUrl" })
    @Test
    public void getKcSAMLTrustRelationship(final String issuer, final String samlTrustRelationshipUrl) {
        log.info("getKcSAMLTrustRelationship() - accessToken:{}, issuer:{}, samlTrustRelationshipUrl:{}", accessToken,
                issuer, samlTrustRelationshipUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + samlTrustRelationshipUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("Response for getKcSAMLTrustRelationship -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
    }

}
