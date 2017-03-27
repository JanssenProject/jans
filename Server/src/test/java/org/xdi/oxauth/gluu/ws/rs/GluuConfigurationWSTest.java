package org.xdi.oxauth.gluu.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.slf4j.Logger;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.gluu.GluuConfiguration;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationWSTest extends BaseTest {

	@RunAsClient
    @Parameters({"gluuConfigurationPath", "webTarget"})
	@Consumes(MediaType.APPLICATION_JSON)
    @Test
    public void getConfigurationTest(String gluuConfigurationPath, @Optional @ArquillianResteasyResource("/.well-known/gluu-configuration") final WebTarget webTarget) throws Exception {
        Response response = webTarget./*path(gluuConfigurationPath).*/request().get();
        BaseTest.showResponse("UMA : TConfiguration.configuration", response);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        try {
        	System.err.println(response.readEntity(String.class));
        	GluuConfiguration appConfiguration = ServerUtil.createJsonMapper().readValue(response.readEntity(String.class), GluuConfiguration.class);
        	System.err.println(appConfiguration.getIdGenerationEndpoint());
            assertNotNull(appConfiguration, "Meta data configuration is null");
            assertNotNull(appConfiguration.getIdGenerationEndpoint());
            assertNotNull(appConfiguration.getIntrospectionEndpoint());
            assertNotNull(appConfiguration.getAuthLevelMapping());
            assertNotNull(appConfiguration.getScopeToClaimsMapping());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}