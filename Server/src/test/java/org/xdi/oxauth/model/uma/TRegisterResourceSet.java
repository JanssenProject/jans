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
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TRegisterResourceSet {

	private final URI baseUri;
	private ResourceSetResponse registerStatus;
	private ResourceSetResponse modifyStatus;

	public TRegisterResourceSet(URI baseUri) {
		assertNotNull(baseUri); // must not be null
		this.baseUri = baseUri;
	}

	public ResourceSetResponse registerResourceSet(final Token p_pat, String umaRegisterResourcePath,
			ResourceSet p_resourceSet) {
		try {
			registerStatus = registerResourceSetInternal(p_pat, umaRegisterResourcePath, p_resourceSet);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		UmaTestUtil.assert_(registerStatus);
		return registerStatus;
	}

	public ResourceSetResponse modifyResourceSet(final Token p_pat, String umaRegisterResourcePath, final String p_rsId,
			ResourceSet p_resourceSet) {
		try {
			modifyStatus = modifyResourceSetInternal(p_pat, umaRegisterResourcePath, p_rsId, p_resourceSet);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		UmaTestUtil.assert_(modifyStatus);
		return modifyStatus;
	}

	private ResourceSetResponse registerResourceSetInternal(final Token p_pat, String umaRegisterResourcePath,
			final ResourceSet p_resourceSet) throws Exception {
		String path = umaRegisterResourcePath;
		System.out.println("Path: " + path);

		System.out.println("PAT: " + p_pat.getAccessToken());
		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		request.header("Authorization", "Bearer " + p_pat.getAccessToken());

		String json = null;
		try {
			// final String json = "{\"resourceSet\":{\"name\":\"Server Photo
			// Album22\",\"iconUri\":\"http://www.example.com/icons/flower.png\",\"scopes\":[\"http://photoz.example.com/dev/scopes/view\",\"http://photoz.example.com/dev/scopes/all\"]}}";
			// final String json =
			// ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(p_resourceSet);
			json = ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
			System.out.println("Json: " + json);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		Response response = request.post(Entity.json(json));
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : TRegisterResourceSet.registerResourceSetInternal() : ", response, entity);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");

		registerStatus = TUma.readJsonValue(entity, ResourceSetResponse.class);

		UmaTestUtil.assert_(registerStatus);
		return registerStatus;
	}

	private ResourceSetResponse modifyResourceSetInternal(final Token p_pat, String umaRegisterResourcePath,
			final String p_rsId, final ResourceSet p_resourceSet) throws Exception {
		String path = umaRegisterResourcePath + "/" + p_rsId + "/";

		Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
		request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
		request.header("Authorization", "Bearer " + p_pat.getAccessToken());

		String json = null;
		try {
			// final String json =
			// ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(p_resourceSet);
			json = ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		Response response = request.put(Entity.json(json));
		String entity = response.readEntity(String.class);

		BaseTest.showResponse("UMA : TRegisterResourceSet.modifyResourceSetInternal() : ", response, entity);

		assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
		modifyStatus = TUma.readJsonValue(entity, ResourceSetResponse.class);

		UmaTestUtil.assert_(modifyStatus);
		return modifyStatus;
	}

	public List<String> getResourceSetList(final Token p_pat, String p_umaRegisterResourcePath) {
		final List<String> result = new ArrayList<String>();

		try {
			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + p_umaRegisterResourcePath)
					.request();
			request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + p_pat.getAccessToken());
			Response response = request.get();
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : TRegisterResourceSet.getResourceSetList() : ", response, entity);

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

	public void deleteResourceSet(final Token p_pat, String p_umaRegisterResourcePath, String p_id) {
		String path = p_umaRegisterResourcePath + "/" + p_id + "/";
		try {

			Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
			// request.addHeader("Accept",
			// UmaConstants.RESOURCE_SET_STATUS_MEDIA_TYPE);
			request.header("Authorization", "Bearer " + p_pat.getAccessToken());

			Response response = request.delete();
			String entity = response.readEntity(String.class);

			BaseTest.showResponse("UMA : TRegisterResourceSet.deleteResourceSet() : ", response, entity);

			assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode(), "Unexpected response code.");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public static void main(String[] args) throws IOException {
		ResourceSet r = new ResourceSet();
		r.setName("test name");
		r.setIconUri("http://icon.com");

		final ObjectMapper mapper = ServerUtil.createJsonMapper();
		mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
		final String json = mapper.writeValueAsString(r);
		System.out.println(json);

		final String j = "{\"resourceSetStatus\":{\"_id\":1364301527462,\"_rev\":1,\"status\":\"created\"}}";
		// final String j =
		// "{\"_id\":1364301527462,\"_rev\":1,\"status\":\"created\"}";
		final ResourceSetResponse newR = TUma.readJsonValue(j, ResourceSetResponse.class);
		System.out.println();
	}
}
