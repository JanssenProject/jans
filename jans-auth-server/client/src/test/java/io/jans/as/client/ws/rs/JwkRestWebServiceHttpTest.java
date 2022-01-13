/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.JwkClient;
import io.jans.as.client.JwkResponse;
import io.jans.as.model.jwk.JSONWebKey;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
            assertNotNull(JSONWebKey.getAlg(), "Unexpected result: alg is null");
        }
        //assertEquals(response.getJwks().getKeys().size(), 11, "The list of keys are not all that could be supported.");
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
            assertNotNull(JSONWebKey.getAlg(), "Unexpected result: alg is null");
        }
        //assertEquals(response.getJwks().getKeys().size(), 11, "The list of keys are not all that could be supported.");
    }
}