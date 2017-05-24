/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.UmaResourceService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.UmaResourceResponse;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaResource;
import org.xdi.oxauth.model.uma.UmaResourceWithId;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test cases for the registering UMA resource set description flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/10/2012
 */
public class RegisterResourceFlowHttpTest extends BaseTest {

    protected UmaMetadata metadata;
    protected Token pat;

    protected String resourceId;

    public RegisterResourceFlowHttpTest() {
    }

    public RegisterResourceFlowHttpTest(UmaMetadata metadataConfiguration) {
        this.metadata = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        if (this.metadata == null) {
            this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl).getMetadata();
            UmaTestUtil.assert_(this.metadata);
        }

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(pat);
    }

    /**
     * Test for the registering UMA resource description
     */
    @Test
    public void testRegisterResource() throws Exception {
        showTitle("testRegisterResource");
        registerResource(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
    }

    public String registerResource(List<String> scopes) throws Exception {
        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Add resource description
        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(scopes);

            resourceStatus = resourceService.addResource("Bearer " + pat.getAccessToken(), resource);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(resourceStatus);

        this.resourceId = resourceStatus.getId();
        return this.resourceId;
    }

    /**
     * Test UMA resource description modification
     */
    @Test(dependsOnMethods = {"testRegisterResource"})
    public void testModifyResource() throws Exception {
        showTitle("testModifyResource");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Modify resource description
        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 2");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceStatus = resourceService.updateResource("Bearer " + pat.getAccessToken(), this.resourceId, resource);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceStatus, "Resource status is null");
        this.resourceId = resourceStatus.getId();
        assertNotNull(this.resourceId, "Resource description id is null");
    }

    /**
     * Test non existing UMA resource description modification
     */
    @Test(dependsOnMethods = {"testModifyResource"})
    public void testModifyNotExistingResource() throws Exception {
        showTitle("testModifyNotExistingResource");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Modify resource description with non existing Id
        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 3");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceStatus = resourceService.updateResource("Bearer " + pat.getAccessToken(), this.resourceId, resource);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceStatus, "Resource status is not null");
    }

    /**
     * Test UMA resource description modification with invalid PAT
     */
    @Test(dependsOnMethods = {"testModifyResource"})
    public void testModifyResourceWithInvalidPat() throws Exception {
        showTitle("testModifyResourceWithInvalidPat");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Modify resource description with invalid PAT
        UmaResourceResponse resourceStatus = null;
        try {
            UmaResource resource = new UmaResource();
            resource.setName("Photo Album 4");
            resource.setIconUri("http://www.example.com/icons/flower.png");
            resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceStatus = resourceService.updateResource("Bearer " + pat.getAccessToken() + "_invalid", this.resourceId + "_invalid", resource);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.UNAUTHORIZED.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceStatus, "Resource status is not null");
    }

    /**
     * Test for getting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testModifyResource"})
    public void testGetOneResource() throws Exception {
        showTitle("testGetOneResource");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Get list of resource set descriptions
        UmaResourceWithId resources = null;
        try {
            resources = resourceService.getResource("Bearer " + pat.getAccessToken(), this.resourceId);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resources, "Resource descriptions is null");
    }

    /**
     * Test for getting UMA resource description
     */
    @Test(dependsOnMethods = {"testGetOneResource"})
    public void testGetResources() throws Exception {
        showTitle("testGetResources");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Get list of resource set descriptions
        List<String> resources = null;
        try {
            resources = resourceService.getResourceList("Bearer " + pat.getAccessToken(), "");
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resources, "Resource descriptions is null");
        assertTrue(resources.contains(this.resourceId), "Resource list doesn't contain added resource");
    }

    /**
     * Test for deleting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testGetResources"})
    public void testDeleteResource() throws Exception {
        showTitle("testDeleteResource");

        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(this.metadata);

        // Delete resource set description
        boolean deleted = false;
        try {
            resourceService.deleteResource("Bearer " + pat.getAccessToken(), this.resourceId);
            deleted = true;
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertTrue(deleted, "Failed to delete resource set description");
    }

}