package org.xdi.oxauth.gluu.ws.rs;

import org.jboss.seam.mock.EnhancedMockHttpServletRequest;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.ResourceRequestEnvironment;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.gluu.GluuConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.util.ServerUtil;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationWSTest extends BaseTest {

    @Parameters({"gluuConfigurationPath"})
    @Test
    public void getConfigurationTest(String gluuConfigurationPath) throws Exception {
        final BaseTest baseTest = this;
        new ResourceRequestEnvironment.ResourceRequest(new ResourceRequestEnvironment(baseTest), ResourceRequestEnvironment.Method.GET, gluuConfigurationPath) {
            private GluuConfiguration configuration;

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
                    configuration = ServerUtil.createJsonMapper().readValue(response.getContentAsString(), GluuConfiguration.class);
                    assertNotNull(configuration, "Meta data configuration is null");
                    assertNotNull(configuration.getFederationMetadataEndpoint());
                    assertNotNull(configuration.getFederationEndpoint());
                    assertNotNull(configuration.getIdGenerationEndpoint());
                    assertNotNull(configuration.getIntrospectionEndpoint());
                    assertNotNull(configuration.getAuthLevelMapping());
                    assertNotNull(configuration.getScopeToClaimsMapping());
                    assertEquals(configuration.getHttpLogoutSupported(), "true");
                    assertEquals(configuration.getLogoutSessionSupported(), "true");

                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.run();
    }
}