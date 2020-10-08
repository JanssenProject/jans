package org.gluu.oxauth.ws.rs;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.GluuConfigurationClient;
import org.gluu.oxauth.client.GluuConfigurationResponse;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationWebServiceHttpTest extends BaseTest {

    @Test
    public void requestGluuConfiguration() throws Exception {
        GluuConfigurationClient client = new GluuConfigurationClient(gluuConfigurationEndpoint);
        GluuConfigurationResponse response = client.execGluuConfiguration();

        showClient(client);
        assertEquals(response.getStatus(), 200, "Unexpected response code. Entity: " + response.getEntity());
        assertNotNull(response.getEntity(), "The entity is null");
    }
}
