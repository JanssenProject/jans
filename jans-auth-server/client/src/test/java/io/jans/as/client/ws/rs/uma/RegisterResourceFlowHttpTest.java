/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import io.jans.as.client.BaseTest;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaResourceService;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaResource;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.model.uma.UmaResourceWithId;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for the registering UMA resources
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */
public class RegisterResourceFlowHttpTest extends BaseTest {

    private static final String START_SCOPE_EXPRESSION = "{\"rule\": {\"and\": [{\"or\": [{\"var\": 0},{\"var\": 1}]},{\"var\": 2}]}," +
            "  \"data\": [\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\",\"http://photoz.example.com/dev/actions/internalClient\"]}";
    private static final String MODIFY_SCOPE_EXPRESSION = "{\"rule\": {\"or\": [{\"or\": [{\"var\": 0},{\"var\": 1}]},{\"var\": 2}]}," +
            "  \"data\": [\"http://photoz.example.com/dev/actions/all\",\"http://photoz.example.com/dev/actions/add\",\"http://photoz.example.com/dev/actions/internalClient\"]}";

    protected UmaMetadata metadata;
    protected Token pat;

    protected String resourceId;
    protected String resourceIdWithScopeExpression;
    protected UmaResourceService resourceService;

    public RegisterResourceFlowHttpTest() {
    }

    public RegisterResourceFlowHttpTest(UmaMetadata metadataConfiguration) {
        this.metadata = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        if (this.metadata == null) {
            this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true)).getMetadata();
            UmaTestUtil.assertIt(this.metadata);
        }

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientEngine(true));
        UmaTestUtil.assertIt(pat);
    }

    public UmaResourceService getResourceService() throws Exception {
        if (resourceService == null) {
            resourceService = UmaClientFactory.instance().createResourceService(this.metadata, clientEngine(true));
        }
        return resourceService;
    }

    /**
     * Add resource
     */
    @Test
    public void addResource() throws Exception {
        showTitle("addResource");
        registerResource(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
        registerResourceWithScopeExpression(START_SCOPE_EXPRESSION);
    }

    public String registerResource(List<String> scopes) throws Exception {
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(scopes);
            resource.setType("myType");

            UmaResourceResponse resourceStatus = getResourceService().addResource("Bearer " + pat.getAccessToken(), resource);
            UmaTestUtil.assertIt(resourceStatus);

            this.resourceId = resourceStatus.getId();
            return this.resourceId;
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }
    }

    public String registerResourceWithScopeExpression(String scopeExpression) throws Exception {
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopeExpression(scopeExpression);
            resource.setType("myType");

            UmaResourceResponse resourceStatus = getResourceService().addResource("Bearer " + pat.getAccessToken(), resource);
            UmaTestUtil.assertIt(resourceStatus);

            this.resourceIdWithScopeExpression = resourceStatus.getId();
            return this.resourceIdWithScopeExpression;
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }
    }

    /**
     * Resource modification
     */
    @Test(dependsOnMethods = {"addResource"})
    public void modifyResource() throws Exception {
        showTitle("modifyResource");

        // Modify resource description
        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 2");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
            resource.setType("myType");

            resourceStatus = getResourceService().updateResource("Bearer " + pat.getAccessToken(), this.resourceId, resource);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }

        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 2");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopeExpression(MODIFY_SCOPE_EXPRESSION);
            resource.setType("myType");

            resourceStatus = getResourceService().updateResource("Bearer " + pat.getAccessToken(), this.resourceIdWithScopeExpression, resource);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceStatus, "Resource status is null");
        assertNotNull(this.resourceId, "Resource description id is null");
    }

    /**
     * Test non existing UMA resource description modification
     */
    @Test(dependsOnMethods = {"modifyResource"})
    public void modifyNotExistingResource() throws Exception {
        showTitle("modifyNotExistingResource");

        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 3");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            getResourceService().updateResource("Bearer " + pat.getAccessToken(), "fake_resource_id", resource);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            int status = ex.getResponse().getStatus();
            assertTrue(status != Response.Status.OK.getStatusCode(), "Unexpected response status");
        }
    }

    /**
     * Test UMA resource description modification with invalid PAT
     */
    @Test(dependsOnMethods = {"modifyResource"})
    public void testModifyResourceWithInvalidPat() throws Exception {
        showTitle("testModifyResourceWithInvalidPat");

        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 4");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceStatus = getResourceService().updateResource("Bearer " + pat.getAccessToken() + "_invalid", this.resourceId + "_invalid", resource);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.UNAUTHORIZED.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceStatus, "Resource status is not null");
    }

    /**
     * Get resource
     */
    @Test(dependsOnMethods = {"modifyResource"})
    public void getOneResource() throws Exception {
        showTitle("getOneResource");

        try {
            UmaResourceWithId resource = getResourceService().getResource("Bearer " + pat.getAccessToken(), this.resourceId);
            assertEquals(resource.getType(), "myType");

            UmaResourceWithId resourceWithExpression = getResourceService().getResource("Bearer " + pat.getAccessToken(), this.resourceIdWithScopeExpression);
            assertEquals(resourceWithExpression.getScopeExpression(), MODIFY_SCOPE_EXPRESSION);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }
    }

    /**
     * Get resources
     */
    @Test(dependsOnMethods = {"getOneResource"})
    public void getResources() throws Exception {
        showTitle("getResources");

        List<String> resources = null;
        try {
            resources = getResourceService().getResourceList("Bearer " + pat.getAccessToken(), "");
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }

        assertNotNull(resources, "Resources is null");
        assertTrue(resources.contains(this.resourceId), "Resource list doesn't contain added resource");
    }

    /**
     * Delete resource
     */
    @Test(dependsOnMethods = {"getResources"})
    public void deleteResource() throws Exception {
        showTitle("testDeleteResource");

        try {
            getResourceService().deleteResource("Bearer " + pat.getAccessToken(), this.resourceId);
        } catch (ClientErrorException ex) {
            System.err.println(ex.getResponse().readEntity(String.class));
            throw ex;
        }
    }
}