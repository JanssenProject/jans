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
import org.xdi.oxauth.client.uma.ResourceSetRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.ResourceSet;
import org.xdi.oxauth.model.uma.ResourceSetWithId;
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
public class RegisterResourceSetFlowHttpTest extends BaseTest {

    protected UmaConfiguration metadataConfiguration;
    protected Token m_pat;

    protected String resourceSetId;

    public RegisterResourceSetFlowHttpTest() {
    }

    public RegisterResourceSetFlowHttpTest(UmaConfiguration metadataConfiguration) {
        this.metadataConfiguration = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        if (this.metadataConfiguration == null) {
            this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
            UmaTestUtil.assert_(this.metadataConfiguration);
        }

        m_pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(m_pat);
    }

    /**
     * Test for the registering UMA resource set description
     */
    @Test
    public void testRegisterResourceSet() throws Exception {
        showTitle("testRegisterResourceSet");
        registerResourceSet(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
    }

    public String registerResourceSet(List<String> scopes) throws Exception {
        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Add resource set description
        ResourceSetResponse resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(scopes);

            resourceSetStatus = resourceSetRegistrationService.addResourceSet("Bearer " + m_pat.getAccessToken(), resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(resourceSetStatus);

        this.resourceSetId = resourceSetStatus.getId();
        return this.resourceSetId;
    }

    /**
     * Test UMA resource set description modification
     */
    @Test(dependsOnMethods = {"testRegisterResourceSet"})
    public void testModifyResourceSet() throws Exception {
        showTitle("testModifyResourceSet");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Modify resource set description
        ResourceSetResponse resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 2");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceSetId, resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceSetStatus, "Resource set status is null");
        this.resourceSetId = resourceSetStatus.getId();
        assertNotNull(this.resourceSetId, "Resource set description id is null");
    }

    /**
     * Test non existing UMA resource set description modification
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testModifyNotExistingResourceSet() throws Exception {
        showTitle("testModifyNotExistingResourceSet");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Modify resource set description with non existing Id
        ResourceSetResponse resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 3");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceSetId, resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceSetStatus, "Resource set status is not null");
    }

    /**
     * Test UMA resource set description modification with invalid PAT
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testModifyResourceSetWithInvalidPat() throws Exception {
        showTitle("testModifyResourceSetWithInvalidPat");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Modify resource set description with invalid PAT
        ResourceSetResponse resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 4");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken() + "_invalid", this.resourceSetId + "_invalid", resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.UNAUTHORIZED.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceSetStatus, "Resource set status is not null");
    }

    /**
     * Test for getting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testGetOneResourceSet() throws Exception {
        showTitle("testGetResourceSets");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Get list of resource set descriptions
        ResourceSetWithId resourceSets = null;
        try {
            resourceSets = resourceSetRegistrationService.getResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceSetId);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceSets, "Resource set descriptions is null");
    }

    /**
     * Test for getting UMA resource set description
     */
    @Test(dependsOnMethods = {"testGetOneResourceSet"})
    public void testGetResourceSets() throws Exception {
        showTitle("testGetResourceSets");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Get list of resource set descriptions
        List<String> resourceSets = null;
        try {
            resourceSets = resourceSetRegistrationService.getResourceSetList("Bearer " + m_pat.getAccessToken(), "");
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceSets, "Resource set descriptions is null");
        assertTrue(resourceSets.contains(this.resourceSetId), "Resource set descriptions list doesn't contain added resource set description");
    }

    /**
     * Test for deleting UMA resource set descriptions
     */
    @Test(dependsOnMethods = {"testGetResourceSets"})
    public void testDeleteResourceSet() throws Exception {
        showTitle("testDeleteResourceSet");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Delete resource set description
        boolean deleted = false;
        try {
            resourceSetRegistrationService.deleteResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceSetId);
            deleted = true;
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertTrue(deleted, "Failed to delete resource set description");
    }

}