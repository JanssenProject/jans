/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.uma;

import io.jans.as.model.common.Holder;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TRegisterPermission {

    private final URI baseUri;

    public TRegisterPermission(URI baseUri) {
        assertNotNull(baseUri); // must not be null
        this.baseUri = baseUri;
    }

    public PermissionTicket registerPermission(final Token pat, final UmaPermission permission, String path) {
        final Holder<PermissionTicket> ticketH = new Holder<>();
        Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
        request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        request.header("Authorization", "Bearer " + pat.getAccessToken());

        String json = null;
        try {
            json = ServerUtil.createJsonMapper().writeValueAsString(permission);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        Response response = request.post(Entity.json(json));
        String entity = response.readEntity(String.class);

        BaseTest.showResponse("UMA : TRegisterPermission.registerPermission() : ", response, entity);

        assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");
        try {
            final PermissionTicket t = ServerUtil.createJsonMapper().readValue(entity, PermissionTicket.class);
            UmaTestUtil.assertIt(t);

            ticketH.setT(t);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        return ticketH.getT();
    }
}
