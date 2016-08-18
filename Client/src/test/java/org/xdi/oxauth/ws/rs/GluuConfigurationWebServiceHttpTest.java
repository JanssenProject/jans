package org.xdi.oxauth.ws.rs;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.GluuConfigurationClient;
import org.xdi.oxauth.client.GluuConfigurationResponse;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

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
