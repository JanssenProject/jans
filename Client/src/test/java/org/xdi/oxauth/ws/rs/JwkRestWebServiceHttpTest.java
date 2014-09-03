package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.JwkClient;
import org.xdi.oxauth.client.JwkResponse;
import org.xdi.oxauth.model.jwk.JSONWebKey;

/**
 * Functional tests for JWK Web Services (HTTP)
 *
 * @author Javier Rojas Blum Date: 11.15.2011
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
        assertNotNull(response.getKeys(), "Unexpected result: keys is null");
        assertTrue(response.getKeys().size() > 0, "Unexpected result: keys is empty");

        for (JSONWebKey JSONWebKey : response.getKeys()) {
            assertNotNull(JSONWebKey.getKeyId(), "Unexpected result: keyId is null");
            assertNotNull(JSONWebKey.getUse(), "Unexpected result: use is null");
        }
    }
}