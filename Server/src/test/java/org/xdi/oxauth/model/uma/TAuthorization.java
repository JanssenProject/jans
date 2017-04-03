/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.common.Holder;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

class TAuthorization {

	private final URI baseUri;

	public TAuthorization(URI baseUri) {
		assertNotNull(baseUri); // must not be null
		this.baseUri = baseUri;
	}

	public RptAuthorizationResponse requestAuthorization(String p_umaPermissionAuthorizationPath,
			final String p_umaAmHost, final Token p_aat, final RptAuthorizationRequest p_request) {
		final Holder<RptAuthorizationResponse> h = new Holder<RptAuthorizationResponse>();

		try {
			Builder request = ResteasyClientBuilder.newClient()
					.target(baseUri.toString() + p_umaPermissionAuthorizationPath).request();

			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + p_aat.getAccessToken());
			request.header("Host", p_umaAmHost);

			final String json = ServerUtil.createJsonMapper().writeValueAsString(p_request);
			Response response = request.post(Entity.json(json));
			String entity = response.readEntity(String.class);
			BaseTest.showResponse("UMA : TAuthorization.requestAuthorization() : ", response, entity);

			assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
			try {
				RptAuthorizationResponse result = ServerUtil.createJsonMapper().readValue(entity,
						RptAuthorizationResponse.class);
				// UmaTestUtil.assert_(result);

				h.setT(result);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return h.getT();
	}
}
