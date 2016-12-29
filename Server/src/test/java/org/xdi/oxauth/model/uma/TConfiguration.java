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
import org.xdi.oxauth.util.ServerUtil;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TConfiguration {

    private final BaseTest baseTest;
    private UmaConfiguration configuration = null;

    public TConfiguration(BaseTest p_baseTest) {
        assertNotNull(p_baseTest); // must not be null
        baseTest = p_baseTest;
    }

    public UmaConfiguration getConfiguration(final String umaConfigurationPath) {
        if (configuration == null) {
            try {
                configuration(umaConfigurationPath);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        UmaTestUtil.assert_(configuration);
        return configuration;
    }

    private void configuration(final String umaConfigurationPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.GET, umaConfigurationPath) {

            @Override
            protected void prepareRequest(EnhancedMockHttpServletRequest request) {
                super.prepareRequest(request);
                request.addHeader("Accept", UmaConstants.JSON_MEDIA_TYPE);
            }

            @Override
            protected void onResponse(EnhancedMockHttpServletResponse response) {
                super.onResponse(response);
                BaseTest.showResponse("UMA : TConfiguration.configuration", response);

                assertEquals(response.getStatus(), 200, "Unexpected response code.");
                try {
                    configuration = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), UmaConfiguration.class);
                    UmaTestUtil.assert_(configuration);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.run();
    }
}
