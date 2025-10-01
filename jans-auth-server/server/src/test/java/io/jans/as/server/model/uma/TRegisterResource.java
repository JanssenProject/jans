/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.uma;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaResource;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

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
        UmaTestUtil.assertIt(registerStatus);
        return registerStatus;
    }

    public UmaResourceResponse modifyResource(final Token pat, String umaRegisterResourcePath, final String rsId, UmaResource resource) {
        try {
            modifyStatus = modifyResourceInternal(pat, umaRegisterResourcePath, rsId, resource);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        UmaTestUtil.assertIt(modifyStatus);
        return modifyStatus;
    }

    private UmaResourceResponse registerResourceInternal(final Token pat, String umaRegisterResourcePath, final UmaResource resource) {
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

        UmaTestUtil.assertIt(registerStatus);
        return registerStatus;
    }

    private UmaResourceResponse modifyResourceInternal(final Token pat, String umaRegisterResourcePath, final String rsId, final UmaResource resource) {
        String path = umaRegisterResourcePath + "/" + rsId + "/";

        Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
        request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        request.header("Authorization", "Bearer " + pat.getAccessToken());

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

        UmaTestUtil.assertIt(modifyStatus);
        return modifyStatus;
    }

    public List<String> getResourceList(final Token pat, String umaRegisterResourcePath) {
        final List<String> result = new ArrayList<>();

        try {
            Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + umaRegisterResourcePath)
                    .request();
            request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
            request.header("Authorization", "Bearer " + pat.getAccessToken());
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

    public void deleteResource(final Token pat, String p_umaRegisterResourcePath, String id) {
        String path = p_umaRegisterResourcePath + "/" + id + "/";
        try {

            Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + path).request();
            // request.addHeader("Accept",
            // UmaConstants.RESOURCE_SET_STATUS_MEDIA_TYPE);
            request.header("Authorization", "Bearer " + pat.getAccessToken());

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
