/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaResource;
import org.gluu.oxauth.model.uma.UmaResourceResponse;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.gluu.oxauth.util.ServerUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.gluu.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TRegisterResource {

	private final URI baseUri;
	private UmaResourceResponse registerStatus;
	private UmaResourceResponse modifyStatus;

	public TRegisterResource(URI baseUri) {
		assertNotNull(baseUri); // must not be null
		this.baseUri = baseUri;
	}

	public UmaResourceResponse registerResource(final Token pat, String umaRegisterResourcePath,
												UmaResource resource) {
		try {
			registerStatus = registerResourceInternal(pat, umaRegisterResourcePath, resource);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		UmaTestUtil.assert_(registerStatus);
		return registerStatus;
	}

	public UmaResourceResponse modifyResource(final Token p_pat, String umaRegisterResourcePath, final String p_rsId,
											  UmaResource resource) {
		try {
			modifyStatus = modifyResourceInternal(p_pat, umaRegisterResourcePath, p_rsId, resource);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		UmaTestUtil.assert_(modifyStatus);
		return modifyStatus;
	}

	private UmaResourceResponse registerResourceInternal(final Token pat, String umaRegisterResourcePath,
														 final UmaResource resource) throws Exception {
		String path = umaRegisterResourcePath;
		System.out.println("Path: " + path);

		System.out.println("PAT: " + pat.getAccessToken());
		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		request.header("Authorization", "Bearer " + pat.getAccessToken());

		String json = null;
		try {
			// final String json = "{\"resource\":{\"name\":\"Server Photo
			// Album22\",\"iconUri\":\"http://www.example.com/icons/flower.png\",\"scopes\":[\"http://photoz.example.com/dev/scopes/view\",\"http://photoz.example.com/dev/scopes/all\"]}}";
			// final String json =
			// ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(resource);
			json = ServerUtil.createJsonMapper().writeValueAsString(resource);
			System.out.println("Json: " + json);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		Response response = request.post(Entity.json(json));
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : TRegisterResource.registerResourceInternal() : ", response, entity);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");

		registerStatus = TUma.readJsonValue(entity, UmaResourceResponse.class);

		UmaTestUtil.assert_(registerStatus);
		return registerStatus;
	}

	private UmaResourceResponse modifyResourceInternal(final Token p_pat, String umaRegisterResourcePath,
													   final String p_rsId, final UmaResource resource) throws Exception {
		String path = umaRegisterResourcePath + "/" + p_rsId + "/";

		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		request.header("Authorization", "Bearer " + p_pat.getAccessToken());

		String json = null;
		try {
			// final String json =
			// ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(resource);
			json = ServerUtil.createJsonMapper().writeValueAsString(resource);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		Response response = request.put(Entity.json(json));
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : TRegisterResource.modifyResourceInternal() : ", response, entity);

		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
		modifyStatus = TUma.readJsonValue(entity, UmaResourceResponse.class);

		UmaTestUtil.assert_(modifyStatus);
		return modifyStatus;
	}

	public List<String> getResourceList(final Token p_pat, String p_umaRegisterResourcePath) {
		final List<String> result = new ArrayList<String>();

		try {
			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + p_umaRegisterResourcePath)
					.request();
			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + p_pat.getAccessToken());
			Response response = request.get();
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : TRegisterResource.getResourceList() : ", response, entity);

			assertEquals(response.getStatus(), 200, "Unexpected response code.");

			List<String> list = TUma.readJsonValue(entity, List.class);
			if (list != null) {
				result.addAll(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		return result;
	}

	public void deleteResource(final Token p_pat, String p_umaRegisterResourcePath, String p_id) {
		String path = p_umaRegisterResourcePath + "/" + p_id + "/";
		try {

			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
			// request.addHeader("Accept",
			// UmaConstants.RESOURCE_SET_STATUS_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + p_pat.getAccessToken());

			Response response = request.delete();
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : TRegisterResource.deleteResource() : ", response, entity);

			assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode(), "Unexpected response code.");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
