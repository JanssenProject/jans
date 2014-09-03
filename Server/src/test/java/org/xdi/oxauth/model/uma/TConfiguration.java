package org.xdi.oxauth.model.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.IOException;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TConfiguration {

    private final BaseTest m_baseTest;
    private MetadataConfiguration m_configuration = null;

    public TConfiguration(BaseTest p_baseTest) {
        assertNotNull(p_baseTest); // must not be null
        m_baseTest = p_baseTest;
    }

    public MetadataConfiguration getConfiguration(final String umaConfigurationPath) {
        if (m_configuration == null) {
            try {
                configuration(umaConfigurationPath);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        UmaTestUtil.assert_(m_configuration);
        return m_configuration;
    }

    private void configuration(final String umaConfigurationPath) throws Exception {
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(m_baseTest), ResourceRequestEnvironment.Method.GET, umaConfigurationPath) {

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
                    m_configuration = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), MetadataConfiguration.class);
                    UmaTestUtil.assert_(m_configuration);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.run();
    }
}
