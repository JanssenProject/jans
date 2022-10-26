/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs;

import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.BaseTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.python.core.util.StringUtil;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Functional tests for JWK Web Services (embedded)
 *
 * @author Javier Rojas Blum
 * @version January 3, 2018
 */
public class JwkRestWebServiceEmbeddedTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"jwksPath"})
    @Test
    public void requestJwks(final String jwksPath) throws Exception {

        Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + jwksPath).request();
        request.header("Accept", MediaType.APPLICATION_JSON);

        Response response = request.get();
        String entity = response.readEntity(String.class);

        showResponse("requestJwks", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");

        try {
            JSONObject jsonObj = new JSONObject(entity);
            assertTrue(jsonObj.has(JSON_WEB_KEY_SET), "Unexpected result: keys not found");
            JSONArray keys = jsonObj.getJSONArray(JSON_WEB_KEY_SET);
            assertNotNull(keys, "Unexpected result: keys is null");
            assertTrue(keys.length() > 0, "Unexpected result: keys is empty");
        } catch (JSONException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void setClaimTestJsonObj() {
        try {
            String stringJson = StringUtil.fromBytes(Base64Util.base64urldecode("eyJzYWx0IjoibWFjbmgiLCJwcm92aWRlciI6ImlkcDEifQ=="));
            JSONObject jobj = new JSONObject(stringJson);

            JwtClaims claims = new JwtClaims();
            claims.setClaim("test_claim", jobj);
            assertEquals(jobj, claims.toJsonObject().get("test_claim"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void setClaimTestInt() {
        try {
            JwtClaims claims = new JwtClaims();
            claims.setClaim("test_claim", 123);
            assertEquals("{\"test_claim\":123}", claims.toJsonObject().toString());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void setClaimTestIntList() {
        try {
            JwtClaims claims = new JwtClaims();
            claims.setClaim("test_claim", Arrays.asList(123, 456, 789));
            assertEquals("{\"test_claim\":[123,456,789]}", claims.toJsonObject().toString());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void setClaimTestStringList() {
        try {
            JwtClaims claims = new JwtClaims();
            claims.setClaim("test_claim", Arrays.asList("qwe", "asd", "zxc"));
            assertEquals("{\"test_claim\":[\"qwe\",\"asd\",\"zxc\"]}", claims.toJsonObject().toString());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
