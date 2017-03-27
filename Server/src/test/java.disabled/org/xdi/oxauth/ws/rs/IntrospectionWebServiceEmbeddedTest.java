/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;
import org.xdi.oxauth.util.ServerUtil;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */

public class IntrospectionWebServiceEmbeddedTest extends BaseTest {

    private Token m_authorization;
    private Token m_tokenToIntrospect;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri"})
    public void requestAuthorization(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        m_authorization = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_authorization);
    }

    @Test(dependsOnMethods = "requestAuthorization")
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret", "umaRedirectUri"})
    public void requestTokenToIntrospect(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                                         String umaAatClientId, String umaAatClientSecret, String umaRedirectUri) {
        m_tokenToIntrospect = TUma.requestAat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_tokenToIntrospect);
    }

    @Test(dependsOnMethods = "requestTokenToIntrospect")
    @Parameters({"introspectionPath"})
    public void introspection(final String introspectionPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.POST, introspectionPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", "Bearer " + m_authorization.getAccessToken());
                request.addParameter("token", m_tokenToIntrospect.getAccessToken());
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                showResponse("introspection", response);

                assertEquals(response.getStatus(), 200);
                try {
                    final IntrospectionResponse t = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), IntrospectionResponse.class);
                    assertTrue(t != null && t.isActive());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.run();
    }

}
