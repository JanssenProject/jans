/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.jans.ws.rs;

import io.jans.as.model.gluu.GluuConfiguration;
import io.jans.as.server.BaseTest;
import io.jans.as.server.util.ServerUtil;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;

import static org.testng.Assert.*;

/**
 * Created by eugeniuparvan on 8/12/16.
 */
public class GluuConfigurationWSTest extends BaseTest {

    @ArquillianResource
    private URI url;

    @Parameters({"gluuConfigurationPath"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Test
    public void getConfigurationTest(String gluuConfigurationPath) throws Exception {
        Invocation.Builder request = ResteasyClientBuilder.newClient().target(getApiTagetURL(url) + gluuConfigurationPath).request();
        Response response = request.get();
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