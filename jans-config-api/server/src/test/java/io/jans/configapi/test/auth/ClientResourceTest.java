/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.test.auth;

import io.jans.configapi.ConfigServerBaseTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import static org.testng.Assert.*;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class ClientResourceTest extends ConfigServerBaseTest {

 
    @Parameters({ "test.issuer", "openidClientsUrl" })
    @Test
    public void getAllClient(final String issuer, final String openidClientsUrl) {
        log.info("getAllClient() - accessToken:{}, issuer:{}, openidClientsUrl:{}", accessToken, issuer,
                openidClientsUrl);
        Builder request = getResteasyService().getClientBuilder(issuer + openidClientsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = request.get();
        assertEquals(response.getStatus(), Status.OK.getStatusCode());
        log.info("Response for getAllClient -  response:{}", response);
    }

    @Parameters({ "test.issuer", "openidClientsUrl", "openid_client_1" })
    @Test
    public void postClient(final String issuer, final String openidClientsUrl, final String json) {
        log.info("\n\n\n postClient2 - accessToken:{}, issuer:{}, openidClientsUrl:{}, json:{}", accessToken, issuer,
                openidClientsUrl, json);

        Builder request = getResteasyService().getClientBuilder(issuer + openidClientsUrl);
        request.header(AUTHORIZATION, AUTHORIZATION_TYPE + " " + accessToken);
        request.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        Response response = request.post(Entity.entity(json, MediaType.APPLICATION_JSON));
        log.info("post client -  response:{}", response);

        if (response.getStatus() == 201) {
            log.trace("Response for postClient -  response.getEntity():{}, response.getClass():{}",
                    response.getEntity(), response.getClass());
        }

        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());
    }
}
