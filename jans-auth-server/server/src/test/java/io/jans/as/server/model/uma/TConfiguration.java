/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.uma;

import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

class TConfiguration {

    private final URI baseUri;
    private UmaMetadata configuration = null;

    public TConfiguration(URI baseUri) {
        assertNotNull(baseUri); // must not be null
        this.baseUri = baseUri;
    }

    public UmaMetadata getConfiguration(final String umaConfigurationPath) {
        if (configuration == null) {
            try {
                configuration(umaConfigurationPath);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        UmaTestUtil.assertIt(configuration);
        return configuration;
    }

    private void configuration(final String umaConfigurationPath) throws Exception {
        Builder request = ResteasyClientBuilder.newClient().target(baseUri.toString() + umaConfigurationPath).request();
        request.header("Accept", UmaConstants.JSON_MEDIA_TYPE);
        Response response = request.get();
        String entity = response.readEntity(String.class);

        BaseTest.showResponse("UMA : TConfiguration.configuration", response, entity);

        assertEquals(response.getStatus(), 200, "Unexpected response code.");
        try {
            configuration = ServerUtil.createJsonMapper().readValue(entity, UmaMetadata.class);
            UmaTestUtil.assertIt(configuration);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
