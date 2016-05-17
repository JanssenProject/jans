/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.common.Holder;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

class TAuthorization {

    private final BaseTest baseTest;

    public TAuthorization(BaseTest p_baseTest) {
        assertNotNull(p_baseTest); // must not be null
        baseTest = p_baseTest;
    }

    public RptAuthorizationResponse requestAuthorization(String p_umaPermissionAuthorizationPath, final String p_umaAmHost, final Token p_aat, final RptAuthorizationRequest p_request) {
        final Holder<RptAuthorizationResponse> h = new Holder<RptAuthorizationResponse>();

        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.POST, p_umaPermissionAuthorizationPath) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + p_aat.getAccessToken());
                    request.addHeader("Host", p_umaAmHost);

                    try {
                        final String json = ServerUtil.createJsonMapper().writeValueAsString(p_request);
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
                    BaseTest.showResponse("UMA : TAuthorization.requestAuthorization() : ", response);

                    assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");
                    try {
                        RptAuthorizationResponse result = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), RptAuthorizationResponse.class);
//                        UmaTestUtil.assert_(result);

                        h.setT(result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            }.run();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return h.getT();
    }
}
