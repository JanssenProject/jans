package org.xdi.oxauth.gluu.ws.rs;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.model.gluu.GluuConfiguration;
import org.gluu.oxauth.util.ServerUtil;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationWSTest extends BaseTest {

	@ArquillianResource
	private URI url;

	@RunAsClient
	@Parameters({ "gluuConfigurationPath", "webTarget" })
	@Consumes(MediaType.APPLICATION_JSON)
	@Test
	public void getConfigurationTest(String gluuConfigurationPath,
			@Optional @ArquillianResteasyResource("") final WebTarget webTarget) throws Exception {
		Response response = webTarget.path(gluuConfigurationPath).request().get();
		String entity = response.readEntity(String.class);
		BaseTest.showResponse("UMA : TConfiguration.configuration", response, entity);

		assertEquals(response.getStatus(), 200, "Unexpected response code.");
		try {
			GluuConfiguration appConfiguration = ServerUtil.createJsonMapper().readValue(entity,
					GluuConfiguration.class);
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