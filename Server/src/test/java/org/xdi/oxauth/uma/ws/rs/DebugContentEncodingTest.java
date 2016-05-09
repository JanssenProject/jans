/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.jboss.seam.resteasy.SeamResteasyProviderFactory;
import org.testng.annotations.BeforeSuite;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * ATTENTION : This test is for debug purpose ONLY. Do not use asserts here!!!
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/03/2013
 */

public class DebugContentEncodingTest extends BaseTest {

    public static final ObjectMapper MAPPER = ServerUtil.createJsonMapper();

    private Token pat;

    private String umaRegisterResourcePath;

    @Override
    @BeforeSuite
    public void startSeam() throws Exception {
        super.startSeam();

        final ResteasyProviderFactory seamInstance = SeamResteasyProviderFactory.peekInstance();
        if (seamInstance != null) {
            System.out.println("SeamResteasyProviderFactory : got resteasy factory");

            // yuriyz : here the funny history begins:
            // first ResteasyJacksonProvider is created by reader (ResteasyProviderFactory.addMessageBodyReader())
            // and is placed into providers map (ResteasyProviderFactory.providers)
            // but later there is created writer provider which overrides it in map (ResteasyProviderFactory.addMessageBodyWriter())
            // as result reader provider is not in map anymore. The only possible way is try to get it via ResteasyProviderFactory.getMessageBodyReader()

            final MessageBodyReader reader = seamInstance.getMessageBodyReader(ResourceSet.class, ResourceSet.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE);
            if (reader instanceof ResteasyJacksonProvider) {
                ResteasyJacksonProvider p = (ResteasyJacksonProvider) reader;
                System.out.println("SeamResteasyProviderFactory : set mapper reader provider");
                p.setMapper(MAPPER);
            } else {
                System.out.println("Unable to find reader jackson provider. Reader: " + reader);
            }

            final MessageBodyWriter writer = seamInstance.getMessageBodyWriter(ResourceSet.class, ResourceSet.class, new Annotation[0], MediaType.APPLICATION_JSON_TYPE);
            if (writer instanceof ResteasyJacksonProvider) {
                ResteasyJacksonProvider p = (ResteasyJacksonProvider) reader;
                System.out.println("SeamResteasyProviderFactory : set mapper writer provider");
                p.setMapper(MAPPER);
            } else {
                System.out.println("Unable to find writer jackson provider. Writer: " + writer);
            }

        }
    }


    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri", "umaRegisterResourcePath"})
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
        pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        this.umaRegisterResourcePath = umaRegisterResourcePath;
    }

    @Test(dependsOnMethods = {"init"})
    public void t1() throws Exception {
        final ResourceSet set = UmaTestUtil.createResourceSet();
        final String json = ServerUtil.createJsonMapper().writeValueAsString(set);
        run(json);
    }
//
//    @Test(dependsOnMethods = "t1")
//    public void t2() throws Exception {
//        final String json = ServerUtil.createJsonMapper().writeValueAsString(UmaTestUtil.createResourceSet());
//        run(json);
//    }

    public void run(final String p_json) {
        try {
            final String rsid = String.valueOf(System.currentTimeMillis());
            String path = umaRegisterResourcePath + "/" + rsid;
            System.out.println("Path: " + path);
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.PUT, path) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    System.out.println("PAT: " + pat.getAccessToken());
                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + pat.getAccessToken());

                    try {
//                        final String json = "{\"resourceSet\":{\"name\":\"Server Photo Album22\",\"iconUri\":\"http://www.example.com/icons/flower.png\",\"scopes\":[\"http://photoz.example.com/dev/scopes/view\",\"http://photoz.example.com/dev/scopes/all\"]}}";
//                    final String json = ServerUtil.createJsonMapper().writeValueAsString(p_resourceSet);
                        System.out.println("Json: " + p_json);
                        request.setContent(Util.getBytes(p_json));
                        request.setContentType(UmaConstants.JSON_MEDIA_TYPE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                protected void onResponse(EnhancedMockHttpServletResponse response) {
                    super.onResponse(response);
                    BaseTest.showResponse("UMA : CheckContentEncodingTest.run() : ", response);

                    if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                        System.out.println("Success.");
                    } else {
                        System.out.println("ERROR: Unexpected response code.");
                    }
                    try {
                        final ResourceSetResponse status = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), ResourceSetResponse.class);
                        System.out.println("Status: " + status);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
