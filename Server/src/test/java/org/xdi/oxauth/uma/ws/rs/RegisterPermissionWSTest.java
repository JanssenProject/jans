/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.junit.Assert;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class RegisterPermissionWSTest extends BaseTest {

    private Token pat;
    private ResourceSetResponse resourceSet;
    private String umaRegisterResourcePath;
    private String umaPermissionPath;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri",
            "umaRegisterResourcePath", "umaPermissionPath"})
    public void init_(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                      String umaPatClientId, String umaPatClientSecret, String umaRedirectUri,
                      String umaRegisterResourcePath, String p_umaPermissionPath) {
        this.umaRegisterResourcePath = umaRegisterResourcePath;
        umaPermissionPath = p_umaPermissionPath;

        pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(pat);
    }

    @Test(dependsOnMethods = {"init_"})
    public void init() {
        resourceSet = TUma.registerResourceSet(this, pat, umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(resourceSet);
    }

    @Test(dependsOnMethods = {"init"})
    @Parameters({"umaAmHost", "umaHost"})
    public void testRegisterPermission(final String umaAmHost, String umaHost) throws Exception {
        final UmaPermission r = new UmaPermission();
        r.setResourceSetId(resourceSet.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        final PermissionTicket ticket = TUma.registerPermission(this, pat, umaAmHost, umaHost, r, umaPermissionPath);
        UmaTestUtil.assert_(ticket);
    }

    @Test(dependsOnMethods = {"testRegisterPermission"})
    @Parameters({"umaAmHost", "umaHost"})
    public void testRegisterPermissionWithInvalidResourceSet(final String umaAmHost, String umaHost) {
        final String path = umaPermissionPath;
        try {
            new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, path) {

                @Override
                protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                    super.prepareRequest(request);

                    request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
                    request.addHeader("Authorization", "Bearer " + pat.getAccessToken());
                    request.addHeader("Host", umaAmHost);

                    try {
                        final UmaPermission r = new UmaPermission();
                        r.setResourceSetId(resourceSet.getId() + "x");

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
                        final PermissionTicket t = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), PermissionTicket.class);
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
        if (resourceSet != null) {
            TUma.deleteResourceSet(this, pat, umaRegisterResourcePath, resourceSet.getId());
        }
    }
}
