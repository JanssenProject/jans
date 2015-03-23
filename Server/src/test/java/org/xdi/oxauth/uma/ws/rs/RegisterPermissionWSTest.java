/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.ResourceSetStatus;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterPermissionWSTest extends BaseTest {

    private Token m_pat;
    private ResourceSetStatus m_resourceSet;
    private String m_umaRegisterResourcePath;
    private String m_umaPermissionPath;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri",
            "umaRegisterResourcePath", "umaPermissionPath"})
    public void init_(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                      String umaPatClientId, String umaPatClientSecret, String umaRedirectUri,
                      String umaRegisterResourcePath, String p_umaPermissionPath) {
        m_umaRegisterResourcePath = umaRegisterResourcePath;
        m_umaPermissionPath = p_umaPermissionPath;

        m_pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_pat);
    }

    @Test(dependsOnMethods = {"init_"})
    public void init() {
        m_resourceSet = TUma.registerResourceSet(this, m_pat, m_umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(m_resourceSet);
    }

    @Test(dependsOnMethods = {"init"})
    @Parameters({"umaAmHost", "umaHost"})
    public void testRegisterPermission(final String umaAmHost, String umaHost) throws Exception {
        final RegisterPermissionRequest r = new RegisterPermissionRequest();
        r.setResourceSetId(m_resourceSet.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        final ResourceSetPermissionTicket ticket = TUma.registerPermission(this, m_pat, umaAmHost, umaHost, r, m_umaPermissionPath);
        UmaTestUtil.assert_(ticket);
    }

    @Test(dependsOnMethods = {"testRegisterPermission"})
    @Parameters({"umaAmHost", "umaHost"})
    public void testRegisterPermissionWithInvalidResourceSet(final String umaAmHost, String umaHost) {
        final RegisterPermissionRequest r = new RegisterPermissionRequest();
        r.setResourceSetId(m_resourceSet.getId() + "x");

        final String path = m_umaPermissionPath + "/" + umaHost + "/";
        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.PUT, path) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + m_pat.getAccessToken());
                    request.addHeader("Host", umaAmHost);

                    try {
                        final String json = ServerUtil.createJsonMapper().writeValueAsString(r);
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
                    BaseTest.showResponse("UMA : RegisterPermissionWSTest.testRegisterPermissionWithInvalidResourceSet() : ", response);

                    assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode(), "Unexpected response code.");
                    try {
                        final ResourceSetPermissionTicket t = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), ResourceSetPermissionTicket.class);
                        Assert.assertNull(t);
                    } catch (Exception e) {
                        // it's ok if it fails here, we expect ticket as null.
                    }
                }
            }.run();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    // use normal test instead of @AfterClass because it will not work with ResourceRequestEnvironment seam class which is used
    // behind TUma wrapper.
    @Test(dependsOnMethods = {"testRegisterPermissionWithInvalidResourceSet"})
    public void cleanUp() {
        if (m_resourceSet != null) {
            TUma.deleteResourceSet(this, m_pat, m_umaRegisterResourcePath, m_resourceSet.getId());
        }
    }
}
