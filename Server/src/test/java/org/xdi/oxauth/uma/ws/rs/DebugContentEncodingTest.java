/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

/**
 * ATTENTION : This test is for debug purpose ONLY. Do not use asserts here!!!
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/03/2013
 */

public class DebugContentEncodingTest extends BaseTest {

	@ArquillianResource
	private URI url;

	public static final ObjectMapper MAPPER = ServerUtil.createJsonMapper();

	private static Token pat;
	private static String umaRegisterResourcePath;

	@Test
	@Parameters({ "authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
			"umaRedirectUri", "umaRegisterResourcePath" })
	public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
			String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
		pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
				umaPatClientSecret, umaRedirectUri);
		this.umaRegisterResourcePath = umaRegisterResourcePath;
	}

	@Test(dependsOnMethods = { "init" })
	public void t1() throws Exception {
		final ResourceSet set = UmaTestUtil.createResourceSet();
		final String json = ServerUtil.createJsonMapper().writeValueAsString(set);
		run(json);
	}
	//
	// @Test(dependsOnMethods = "t1")
	// public void t2() throws Exception {
	// final String json =
	// ServerUtil.createJsonMapper().writeValueAsString(UmaTestUtil.createResourceSet());
	// run(json);
	// }

	public void run(final String p_json) {
		try {
			final String rsid = String.valueOf(System.currentTimeMillis());
			String path = umaRegisterResourcePath + "/" + rsid;
			System.out.println("Path: " + path);

			System.out.println("PAT: " + pat.getAccessToken());

			Builder request = ResteasyClientBuilder.newClient().target(url.toString() + path).request();
			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + pat.getAccessToken());

			try {
				// final String json = "{\"resourceSet\":{\"name\":\"Server
				// Photo
				// Album22\",\"iconUri\":\"http://www.example.com/icons/flower.png\",\"scopes\":[\"http://photoz.example.com/dev/scopes/view\",\"http://photoz.example.com/dev/scopes/all\"]}}";
				// final String json =
				// ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
				System.out.println("Json: " + p_json);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}

			Response response = request.post(Entity.json(p_json));
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : CheckContentEncodingTest.run() : ", response, entity);

			if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
				System.out.println("Success.");
			} else {
				System.out.println("ERROR: Unexpected response code.");
			}
			try {
				final ResourceSetResponse status = ServerUtil.createJsonMapper().readValue(entity,
						ResourceSetResponse.class);
				System.out.println("Status: " + status);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
