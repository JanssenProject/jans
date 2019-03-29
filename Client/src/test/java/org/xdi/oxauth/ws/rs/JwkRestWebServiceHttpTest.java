/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.JwkResponse;

import static org.testng.Assert.*;

import org.gluu.oxauth.model.jwk.JSONWebKey;

/**
 * Functional tests for JWK Web Services (HTTP)
 *
 * @author Javier Rojas Blum
 * @version June 25, 2016
 */
public class JwkRestWebServiceHttpTest extends BaseTest {

    @Test
    public void requestJwks() throws Exception {
        showTitle("requestJwks");

        JwkClient jwkClient = new JwkClient(jwksUri);
        JwkResponse response = jwkClient.exec();

        showClient(jwkClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "Unexpected result: entity is null");
        assertNotNull(response.getJwks(), "Unexpected result: jwks is null");
        assertNotNull(response.getJwks().getKeys(), "Unexpected result: keys is null");
        assertTrue(response.getJwks().getKeys().size() > 0, "Unexpected result: keys is empty");

        for (JSONWebKey JSONWebKey : response.getJwks().getKeys()) {
            assertNotNull(JSONWebKey.getKid(), "Unexpected result: kid is null");
            assertNotNull(JSONWebKey.getUse(), "Unexpected result: use is null");
        }
    }

    @Parameters({"clientJwksUri"})
    @Test
    public void requestClientJwks(final String clientJwksUri) throws Exception {
        showTitle("requestJwks");

        JwkClient jwkClient = new JwkClient(clientJwksUri);
        JwkResponse response = jwkClient.exec();

        showClient(jwkClient);
        assertEquals(response.getStatus(), 200, "Unexpected response code: " + response.getEntity());
        assertNotNull(response.getEntity(), "Unexpected result: entity is null");
        assertNotNull(response.getJwks(), "Unexpected result: jwks is null");
        assertNotNull(response.getJwks().getKeys(), "Unexpected result: keys is null");
        assertTrue(response.getJwks().getKeys().size() > 0, "Unexpected result: keys is empty");

        for (JSONWebKey JSONWebKey : response.getJwks().getKeys()) {
            assertNotNull(JSONWebKey.getKid(), "Unexpected result: kid is null");
            assertNotNull(JSONWebKey.getUse(), "Unexpected result: use is null");
        }
    }
}