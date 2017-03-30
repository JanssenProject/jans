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

import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;

/**
 * Functional tests for JWK Web Services (embedded)
 *
 * @author Javier Rojas Blum Date: 11.15.2011
 */
public class JwkRestWebServiceEmbeddedTest extends BaseTest {

	@ArquillianResource
	private URI url;

	@Parameters({ "jwksPath" })
	@Test
	public void requestJwks(final String jwksPath) throws Exception {

		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + jwksPath).request();
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

}
