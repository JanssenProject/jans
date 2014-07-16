package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.ResourceSetRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.*;
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

    protected MetadataConfiguration metadataConfiguration;
    protected Token m_pat;

    protected String resourceSetId;
    protected String resourceVersion;

    public RegisterResourceSetFlowHttpTest() {
    }

    public RegisterResourceSetFlowHttpTest(MetadataConfiguration metadataConfiguration) {
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

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Add resource set description
        ResourceSetStatus resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            String id = String.valueOf(System.currentTimeMillis());
            resourceSetStatus = resourceSetRegistrationService.addResourceSet("Bearer " + m_pat.getAccessToken(), id, resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(resourceSetStatus);

        this.resourceSetId = resourceSetStatus.getId();
        this.resourceVersion = resourceSetStatus.getRev();
        assertEquals(this.resourceVersion, "1", "Resource set description revision is not 1");
    }

    /**
     * Test UMA resource set description modification
     */
    @Test(dependsOnMethods = {"testRegisterResourceSet"})
    public void testModifyResourceSet() throws Exception {
        showTitle("testModifyResourceSet");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Modify resource set description
        ResourceSetStatus resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 2");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceVersion, this.resourceSetId, resourceSet);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertNotNull(resourceSetStatus, "Resource set status is null");

        this.resourceSetId = resourceSetStatus.getId();
        this.resourceVersion = resourceSetStatus.getRev();
        assertNotNull(this.resourceSetId, "Resource set description id is null");
        assertNotNull(this.resourceVersion, "Resource set description revision is null");
        assertEquals(this.resourceVersion, "2", "Resource set description revision is not 2");
    }

    /**
     * Test non existing UMA resource set description modification
     */
    @Test(dependsOnMethods = {"testModifyResourceSet"})
    public void testModifyNotExistingResourceSet() throws Exception {
        showTitle("testModifyNotExistingResourceSet");

        ResourceSetRegistrationService resourceSetRegistrationService = UmaClientFactory.instance().createResourceSetRegistrationService(this.metadataConfiguration);

        // Modify resource set description with non existing Id
        ResourceSetStatus resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 3");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceVersion, this.resourceSetId + "1", resourceSet);
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
        ResourceSetStatus resourceSetStatus = null;
        try {
            ResourceSet resourceSet = new ResourceSet();
            resourceSet.setName("Photo Album 4");
            resourceSet.setIconUri("http://www.example.com/icons/flower.png");
            resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));

            resourceSetStatus = resourceSetRegistrationService.updateResourceSet("Bearer " + m_pat.getAccessToken() + "_invalid", this.resourceVersion, this.resourceSetId + "_invalid", resourceSet);
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
        VersionedResourceSet resourceSets = null;
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
            resourceSetRegistrationService.deleteResourceSet("Bearer " + m_pat.getAccessToken(), this.resourceVersion, this.resourceSetId);
            deleted = true;
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        assertTrue(deleted, "Failed to delete resource set description");
    }

}