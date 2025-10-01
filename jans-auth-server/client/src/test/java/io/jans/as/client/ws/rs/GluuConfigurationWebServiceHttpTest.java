/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs;

import io.jans.as.client.BaseTest;
import io.jans.as.client.GluuConfigurationClient;
import io.jans.as.client.GluuConfigurationResponse;
import org.testng.annotations.Test;

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
