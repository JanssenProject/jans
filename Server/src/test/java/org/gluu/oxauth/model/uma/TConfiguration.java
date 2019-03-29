/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaMetadata;
import org.gluu.oxauth.util.ServerUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.gluu.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TConfiguration {

	private final URI baseUri;
	private UmaMetadata configuration = null;

	public TConfiguration(URI baseUri) {
		assertNotNull(baseUri); // must not be null
		this.baseUri = baseUri;
	}

	public UmaMetadata getConfiguration(final String umaConfigurationPath) {
		if (configuration == null) {
			try {
				configuration(umaConfigurationPath);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		}
		UmaTestUtil.assert_(configuration);
		return configuration;
	}

	private void configuration(final String umaConfigurationPath) throws Exception {
		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + umaConfigurationPath).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		Response response = request.get();
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : TConfiguration.configuration", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code.");
		try {
			configuration = ServerUtil.createJsonMapper().readValue(entity, UmaMetadata.class);
			UmaTestUtil.assert_(configuration);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}
}
