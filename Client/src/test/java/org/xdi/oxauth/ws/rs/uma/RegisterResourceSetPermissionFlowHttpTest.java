/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.ResourceSetPermissionRegistrationService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.ResourceSetPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * Test cases for the registering UMA resource set description permissions flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/19/2012
 */
public class RegisterResourceSetPermissionFlowHttpTest extends BaseTest {

    protected MetadataConfiguration metadataConfiguration;

    protected RegisterResourceSetFlowHttpTest umaRegisterResourceSetFlowHttpTest;
    protected String ticketForFullAccess;

    public RegisterResourceSetPermissionFlowHttpTest() {
    }

    public RegisterResourceSetPermissionFlowHttpTest(MetadataConfiguration metadataConfiguration) {
        this.metadataConfiguration = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaUserId, final String umaUserSecret,
                     final String umaPatClientId, final String umaPatClientSecret, final String umaRedirectUri) throws Exception {
        if (this.metadataConfiguration == null) {
            this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
            UmaTestUtil.assert_(this.metadataConfiguration);
        }

        this.umaRegisterResourceSetFlowHttpTest = new RegisterResourceSetFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceSetFlowHttpTest.setAuthorizationEndpoint(authorizationEndpoint);
        this.umaRegisterResourceSetFlowHttpTest.setTokenEndpoint(tokenEndpoint);
        this.umaRegisterResourceSetFlowHttpTest.init(umaMetaDataUrl, umaPatClientId, umaPatClientSecret);

        this.umaRegisterResourceSetFlowHttpTest.testRegisterResourceSet();
    }

    @AfterClass
    public void clean() throws Exception {
        this.umaRegisterResourceSetFlowHttpTest.testDeleteResourceSet();
    }

    /**
     * Test for registering permissions for resource set
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testRegisterResourceSetPermission(final String umaAmHost) throws Exception {
        showTitle("testRegisterResourceSetPermission");

        ResourceSetPermissionRegistrationService resourceSetPermissionRegistrationService = UmaClientFactory.instance().createResourceSetPermissionRegistrationService(this.metadataConfiguration);

        // Register permissions for resource set
        ResourceSetPermissionRequest resourceSetPermissionRequest = new ResourceSetPermissionRequest();
        resourceSetPermissionRequest.setResourceSetId(this.umaRegisterResourceSetFlowHttpTest.resourceSetId);
        resourceSetPermissionRequest.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        ResourceSetPermissionTicket t = null;
        try {
            t = resourceSetPermissionRegistrationService.registerResourceSetPermission(
                    "Bearer " + this.umaRegisterResourceSetFlowHttpTest.m_pat.getAccessToken(), umaAmHost, "photoz.example.com", resourceSetPermissionRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(t);
        this.ticketForFullAccess = t.getTicket();
    }

    /**
     * Test for registering permissions for resource set
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testRegisterResourceSetPermissionForInvalidResource(final String umaAmHost) throws Exception {
        showTitle("testRegisterResourceSetPermissionForInvalidResource");

        ResourceSetPermissionRegistrationService resourceSetPermissionRegistrationService = UmaClientFactory.instance().createResourceSetPermissionRegistrationService(this.metadataConfiguration);

        // Register permissions for resource set
        ResourceSetPermissionRequest resourceSetPermissionRequest = new ResourceSetPermissionRequest();
        resourceSetPermissionRequest.setResourceSetId(this.umaRegisterResourceSetFlowHttpTest.resourceSetId + "1");
        resourceSetPermissionRequest.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));


        ResourceSetPermissionTicket resourceSetPermissionTiket = null;
        try {
            resourceSetPermissionTiket = resourceSetPermissionRegistrationService.registerResourceSetPermission(
                    "Bearer " + this.umaRegisterResourceSetFlowHttpTest.m_pat.getAccessToken(), umaAmHost, "photoz.example.com", resourceSetPermissionRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode(), "Unexpected response status");
        }

        assertNull(resourceSetPermissionTiket, "Resource set permission is not null");
    }

    /**
     * Test get registering permissions ticket by configuration code
     */
    /*
	@Test
    @Parameters({"umaAmHost"})
    public void testGetResourceSetPermission(final String umaAmHost) throws Exception {
        showTitle("testGetResourceSetPermission");

        ResourceSetPermissionRegistrationService resourceSetPermissionRegistrationService = UmaClientFactory.instance().createResourceSetPermissionRegistrationService(this.metadataConfiguration.getPermissionRegistrationEndpoint());
    	
    	ResourceSetPermissionTiket resourceSetPermissionTiket = null;
		try {
			resourceSetPermissionTiket = resourceSetPermissionRegistrationService.getResourceSetPermission(
					"Bearer " + this.umaRegisterResourceSetFlowHttpTest.umaPatTokenAwareHttpTest.patToken, umaAmHost, "photoz.example.com", "5287.8C53.4BAB.5992.439D.7818.D36D.44CC.1350670758102");
		} catch (ClientResponseFailure ex) {
			System.err.println(ex.getResponse().getEntity(String.class));
			throw ex;
		}

		assertNotNull(resourceSetPermissionTiket, "Resource set permission is null");
		assertNotNull(resourceSetPermissionTiket.getTicket(), "Resource set permission  token is null");
    }
*/
}