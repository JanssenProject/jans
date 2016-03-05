/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TRegisterResourceSet {

    private final BaseTest baseTest;
    private ResourceSetResponse registerStatus;
    private ResourceSetResponse modifyStatus;

    public TRegisterResourceSet(BaseTest p_baseTest) {
        assertNotNull(p_baseTest); // must not be null
        baseTest = p_baseTest;
    }

    public ResourceSetResponse registerResourceSet(final Token p_pat, String umaRegisterResourcePath, ResourceSet p_resourceSet) {
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

    private ResourceSetResponse registerResourceSetInternal(final Token p_pat, String umaRegisterResourcePath, final ResourceSet p_resourceSet) throws Exception {
        String path = umaRegisterResourcePath;
        System.out.println("Path: " + path);
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.POST, path) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                System.out.println("PAT: " + p_pat.getAccessToken());
                request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                request.addHeader("Authorization", "Bearer " + p_pat.getAccessToken());

                try {
//                    final String json = "{\"resourceSet\":{\"name\":\"Server Photo Album22\",\"iconUri\":\"http://www.example.com/icons/flower.png\",\"scopes\":[\"http://photoz.example.com/dev/scopes/view\",\"http://photoz.example.com/dev/scopes/all\"]}}";
//                    final String json = ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(p_resourceSet);
                    final String json = ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
                    System.out.println("Json: " + json);
                    request.setContent(Util.getBytes(json));
                    request.setContentType(UmaConstants.JSON_MEDIA_TYPE);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                BaseTest.showResponse("UMA : TRegisterResourceSet.registerResourceSetInternal() : ", response);

                assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");

                registerStatus = TUma.readJsonValue(response.getContentAsString(), ResourceSetResponse.class);

                UmaTestUtil.assert_(registerStatus);
            }
        }.run();
        return registerStatus;
    }

    private ResourceSetResponse modifyResourceSetInternal(final Token p_pat, String umaRegisterResourcePath,
                                                        final String p_rsId, final ResourceSet p_resourceSet) throws Exception {
        String path = umaRegisterResourcePath + "/" + p_rsId + "/";
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.PUT, path) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);

                request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                request.addHeader("Authorization", "Bearer " + p_pat.getAccessToken());

                try {
//                    final String json = ServerUtil.jsonMapperWithWrapRoot().writeValueAsString(p_resourceSet);
                    final String json = ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
                    request.setContent(Util.getBytes(json));
                    request.setContentType(UmaConstants.JSON_MEDIA_TYPE);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                BaseTest.showResponse("UMA : TRegisterResourceSet.modifyResourceSetInternal() : ", response);

                assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
                modifyStatus = TUma.readJsonValue(response.getContentAsString(), ResourceSetResponse.class);

                UmaTestUtil.assert_(modifyStatus);
            }
        }.run();
        return modifyStatus;
    }

    public List<String> getResourceSetList(final Token p_pat, String p_umaRegisterResourcePath) {
        final List<String> result = new ArrayList<String>();
        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.GET, p_umaRegisterResourcePath) {
                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + p_pat.getAccessToken());
                }

                @Override
                protected void onResponse(EnhancedMockHttpServletResponse response) {
                    super.onResponse(response);
                    BaseTest.showResponse("UMA : TRegisterResourceSet.getResourceSetList() : ", response);

                    assertEquals(response.getStatus(), 200, "Unexpected response code.");

                    List<String> list = TUma.readJsonValue(response.getContentAsString(), List.class);
                    if (list != null) {
                        result.addAll(list);
                    }

                }
            }.run();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return result;
    }

    public void deleteResourceSet(final Token p_pat, String p_umaRegisterResourcePath, String p_id) {
        String path = p_umaRegisterResourcePath + "/" + p_id + "/";
        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.DELETE, path) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    //request.addHeader("Accept", UmaConstants.RESOURCE_SET_STATUS_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + p_pat.getAccessToken());
                }

                @Override
                protected void onResponse(EnhancedMockHttpServletResponse response) {
                    super.onResponse(response);
                    BaseTest.showResponse("UMA : TRegisterResourceSet.deleteResourceSet() : ", response);

                    assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode(), "Unexpected response code.");
                }
            }.run();
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
//        final String j = "{\"_id\":1364301527462,\"_rev\":1,\"status\":\"created\"}";
        final ResourceSetResponse newR = TUma.readJsonValue(j, ResourceSetResponse.class);
        System.out.println();
    }
}
