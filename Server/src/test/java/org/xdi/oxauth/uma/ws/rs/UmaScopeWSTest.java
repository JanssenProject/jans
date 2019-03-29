/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.testng.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaScopeDescription;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class UmaScopeWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	// private MetadataConfiguration m_configuration;
	//
	// @Parameters({"umaConfigurationPath"})
	// @Test
	// public void init(final String umaConfigurationPath) {
	// m_configuration = TUma.requestConfiguration(this, umaConfigurationPath);
	// UmaTestUtil.assert_(m_configuration);
	// }

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

		UmaTestUtil.assert_(scope);
	}
}
