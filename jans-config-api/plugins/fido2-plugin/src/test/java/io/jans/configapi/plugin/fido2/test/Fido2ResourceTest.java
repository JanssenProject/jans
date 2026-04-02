/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.test;

import io.jans.configapi.plugin.fido2.Fido2BaseTest;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Parameters;

public class Fido2ResourceTest extends Fido2BaseTest {

    @Parameters({ "test.issuer", "fido2ConfigUrl" })
    @Test
    public void getFido2Configuration(final String issuer, final String fido2ConfigUrl) {
        log.error("\n\n getFido2Configuration() - issuer:{}, fido2ConfigUrl:{}", issuer, fido2ConfigUrl);

        Builder request = getResteasyService().getClientBuilder(issuer + fido2ConfigUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        log.info("getFido2Configuration() - Response for getDefaultAuthenticationMethod -  response:{}, response.getStatus():{}", response, response.getStatus());
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        
    }

}
