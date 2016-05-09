/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ScopeDescription;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */

public class UmaScopeWSTest extends BaseTest {

//    private MetadataConfiguration m_configuration;
//
//    @Parameters({"umaConfigurationPath"})
//    @Test
//    public void init(final String umaConfigurationPath) {
//        m_configuration = TUma.requestConfiguration(this, umaConfigurationPath);
//        UmaTestUtil.assert_(m_configuration);
//    }

    @Parameters({"umaScopePath"})
    @Test
    public void scopePresence(final String umaScopePath) throws Exception {
        String path = umaScopePath + "/" + "modify";
        System.out.println("Path: " + path);
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(this), ResourceRequestEnvironment.Method.GET, path) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                BaseTest.showResponse("UMA : UmaScopeWSTest.scopePresence() : ", response);

                assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(), "Unexpected response code.");

                final ScopeDescription scope = TUma.readJsonValue(response.getContentAsString(), ScopeDescription.class);

                UmaTestUtil.assert_(scope);
            }
        }.run();
    }
}
