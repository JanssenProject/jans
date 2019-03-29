/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.uma.PermissionTicket;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaPermission;
import org.gluu.oxauth.model.uma.UmaResourceResponse;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterPermissionWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	private static Token pat;
	private static UmaResourceResponse resource;
	private static String umaRegisterResourcePath;
	private static String umaPermissionPath;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaRedirectUri", "umaRegisterResourcePath", "umaPermissionPath" })
	public void init_(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath,
			String p_umaPermissionPath) {
		this.umaRegisterResourcePath = umaRegisterResourcePath;
		umaPermissionPath = p_umaPermissionPath;

		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);
		UmaTestUtil.assert_(pat);
	}

	@Test(dependsOnMethods = { "init_" })
	public void init() {
		resource = TUma.registerResource(url, pat, umaRegisterResourcePath, UmaTestUtil.createResource());
		UmaTestUtil.assert_(resource);
	}

	@Test(dependsOnMethods = { "init" })
	public void testRegisterPermission() throws Exception {
		final UmaPermission r = new UmaPermission();
		r.setResourceId(resource.getId());
		r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

		final PermissionTicket ticket = TUma.registerPermission(url, pat, r, umaPermissionPath);
		UmaTestUtil.assert_(ticket);
	}

	@Test(dependsOnMethods = { "testRegisterPermission" })
	public void testRegisterPermissionWithInvalidResource() {
		final String path = umaPermissionPath;
		try {
			Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + pat.getAccessToken());

			String json = null;
			try {
				final UmaPermission r = new UmaPermission();
				r.setResourceId(resource.getId() + "x");

				json = ServerUtil.createJsonMapper().writeValueAsString(r);
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}

			Response response = request.post(Entity.json(json));
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : RegisterPermissionWSTest.testRegisterPermissionWithInvalidResource() : ",
					response, entity);

			assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode(),
					"Unexpected response code.");
			try {
				final PermissionTicket t = ServerUtil.createJsonMapper().readValue(entity, PermissionTicket.class);
				Assert.assertNull(t);
			} catch (Exception e) {
				// it's ok if it fails here, we expect ticket as null.
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	// use normal test instead of @AfterClass because it will not work with
	// ResourceRequestEnvironment seam class which is used
	// behind TUma wrapper.
	@Test(dependsOnMethods = {"testRegisterPermissionWithInvalidResource"})
	public void cleanUp() {
		if (resource != null) {
			TUma.deleteResource(url, pat, umaRegisterResourcePath, resource.getId());
		}
	}
}
