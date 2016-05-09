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

import static org.junit.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TRegisterPermission {

    private final BaseTest baseTest;

    public TRegisterPermission(BaseTest p_baseTest) {
        assertNotNull(p_baseTest); // must not be null
        baseTest = p_baseTest;
    }

    public PermissionTicket registerPermission(final Token p_pat, final String p_umaAmHost, String p_umaHost,
                                     final UmaPermission p_request, String path) {
        final Holder<PermissionTicket> ticketH = new Holder<PermissionTicket>();
        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.POST, path) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + p_pat.getAccessToken());
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
                    BaseTest.showResponse("UMA : TRegisterPermission.registerPermission() : ", response);

                    assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode(), "Unexpected response code.");
                    try {
                        final PermissionTicket t = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), PermissionTicket.class);
                        UmaTestUtil.assert_(t);

                        ticketH.setT(t);
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
        return ticketH.getT();
    }
}
