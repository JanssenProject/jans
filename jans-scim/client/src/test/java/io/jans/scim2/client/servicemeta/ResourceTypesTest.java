/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.servicemeta;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-21.
 */
public class ResourceTypesTest extends BaseTest {

    private ListResponse listResponse;

    @BeforeTest
    public void init() throws Exception{
        Response response=client.getResourceTypes();
        listResponse=response.readEntity(ListResponse.class);
    }

    @Test
    public void checkResourcesExistence(){
        assertTrue(listResponse.getTotalResults()>0);

        listResponse.getResources().forEach(
                res -> assertTrue(res.getSchemas().contains("urn:ietf:params:scim:schemas:core:2.0:ResourceType")));

    }
}
