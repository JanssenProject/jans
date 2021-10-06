/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import static org.testng.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaScopeDescription;
import io.jans.as.model.uma.UmaTestUtil;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.uma.TUma;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class UmaScopeWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	@Parameters({ "umaScopePath" })
	@Test
	public void scopePresence(final String umaScopePath) throws Exception {
		String path = umaScopePath + "/" + "modify";
		System.out.println("Path: " + path);

		Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		Response response = request.get();
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : UmaScopeWSTest.scopePresence() : ", response, entity);

		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");

		final UmaScopeDescription scope = TUma.readJsonValue(entity, UmaScopeDescription.class);

		UmaTestUtil.assertIt(scope);
	}
}
