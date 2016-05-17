/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.xdi.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
/**
 * Functional tests for JWK Web Services (embedded)
 *
 * @author Javier Rojas Blum Date: 11.15.2011
 */
public class JwkRestWebServiceEmbeddedTest extends BaseTest {

    @Parameters({"jwksPath"})
    @Test
    public void requestJwks(final String jwksPath) throws Exception {

        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this),
                ResourceRequestEnvironment.Method.GET, jwksPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Accept", MediaType.APPLICATION_JSON);
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("requestJwks", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");

                try {
                    JSONObject jsonObj = new JSONObject(response.getContentAsString());
                    assertTrue(jsonObj.has(JSON_WEB_KEY_SET), "Unexpected result: keys not found");
                    JSONArray keys = jsonObj.getJSONArray(JSON_WEB_KEY_SET);
                    assertNotNull(keys, "Unexpected result: keys is null");
                    assertTrue(keys.length() > 0, "Unexpected result: keys is empty");
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }.run();
    }
}