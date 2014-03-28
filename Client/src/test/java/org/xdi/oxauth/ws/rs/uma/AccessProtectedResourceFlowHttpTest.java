package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.AuthorizationRequestService;
import org.xdi.oxauth.client.uma.RptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.*;
import org.xdi.oxauth.model.uma.wrapper.Token;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test flow for the accessing protected resource (HTTP)
 *
 * @author Yuriy Movchan Date: 10/22/2012
 */
public class AccessProtectedResourceFlowHttpTest extends BaseTest {

    protected MetadataConfiguration metadataConfiguration;

    protected ObtainRptTokenFlowHttpTest umaObtainRptTokenFlowHttpTest;

    protected RegisterResourceSetFlowHttpTest umaRegisterResourceSetFlowHttpTest;
    protected RegisterResourceSetPermissionFlowHttpTest umaRegisterResourceSetPermissionFlowHttpTest;

    protected RptStatusService rptStatusService;
    protected AuthorizationRequestService rptPermissionAuthorizationService;

    protected Token m_aat;
    protected Token m_pat;

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        UmaTestUtil.assert_(this.metadataConfiguration);

        this.umaObtainRptTokenFlowHttpTest = new ObtainRptTokenFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceSetFlowHttpTest = new RegisterResourceSetFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceSetPermissionFlowHttpTest = new RegisterResourceSetPermissionFlowHttpTest(this.metadataConfiguration);

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadataConfiguration);
        this.rptPermissionAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(metadataConfiguration);
    }

    //** 1 ******************************************************************************

    /**
     * Host obtains PAT
     */
    @Test
    @Parameters({"umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri"})
    public void testHostObtainPat(final String umaUserId, final String umaUserSecret, final String umaPatClientId,
                                  final String umaPatClientSecret, final String umaRedirectUri) throws Exception {
        showTitle("testHostObtainPat");
        m_pat = UmaClient.requestPat(authorizationEndpoint, tokenEndpoint, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_pat);

        // Init UmaPatTokenAwareHttpTest test
        this.umaRegisterResourceSetFlowHttpTest.m_pat = this.m_pat;

        // Init UmaRegisterResourceSetPermissionFlowHttpTest test
        this.umaRegisterResourceSetPermissionFlowHttpTest.umaRegisterResourceSetFlowHttpTest = this.umaRegisterResourceSetFlowHttpTest;
    }

    /**
     * Host registers resource set description
     */
    @Test(dependsOnMethods = {"testHostObtainPat"})
    public void testHostRegisterResourceSet() throws Exception {
        showTitle("testHostRegisterResourceSet");
        this.umaRegisterResourceSetFlowHttpTest.testRegisterResourceSet();
    }

    //** 2 ******************************************************************************

    /**
     * Requester obtains AAT token
     */
    @Test(dependsOnMethods = {"testHostRegisterResourceSet"})
    @Parameters({"umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret", "umaRedirectUri"})
    public void testRequesterObtainAat(final String umaUserId, final String umaUserSecret,
                                       final String umaAatClientId, final String umaAatClientSecret,
                                       final String umaRedirectUri) throws Exception {
        showTitle("testRequesterObtainAat");
        m_aat = UmaClient.requestAat(authorizationEndpoint, tokenEndpoint, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_aat);

        // Init UmaPatTokenAwareHttpTest test
        this.umaObtainRptTokenFlowHttpTest.m_aat = this.m_aat;
    }

    /**
     * Requester obtains RPT token
     */
    @Test(dependsOnMethods = {"testRequesterObtainAat"})
    @Parameters({"umaAmHost"})
    public void testRequesterObtainsRpt(final String umaAmHost) throws Exception {
        showTitle("testRequesterObtainsRpt");
        this.umaObtainRptTokenFlowHttpTest.testObtainRptTokenFlow(umaAmHost);
    }

    //** 3 ******************************************************************************

    /**
     * Requesting party access protected resource at host via requester
     */
    @Test(dependsOnMethods = {"testRequesterObtainsRpt"})
    public void testRequesterAccessProtectedResourceWithNotEnoughPermissionsRpt() throws Exception {
        showTitle("testRequesterAccessProtectedResourceWithNotEnoughPermissionsRpt");
        // Scenario for case when there is no valid RPT in request or not enough permissions
        // In this case we have RPT without permissions
    }

    /**
     * Host determines RPT status
     */
    @Test(dependsOnMethods = {"testRequesterAccessProtectedResourceWithNotEnoughPermissionsRpt"})
    @Parameters({"umaAmHost"})
    public void testHostDetermineRptStatus1(final String umaAmHost) throws Exception {
        showTitle("testHostDetermineRptStatus1");

        String resourceSetId = umaRegisterResourceSetFlowHttpTest.resourceSetId;

        // Determine RPT token to status
        RptStatusResponse tokenStatusResponse = null;
        try {
            RptStatusRequest tokenStatusRequest = new RptStatusRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, resourceSetId);
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + m_pat.getAccessToken(),
                    tokenStatusRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
//			assertEquals(ex.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode(), "Unexpected response status");
            throw ex;
        }

        assertNotNull(tokenStatusResponse, "Token response status is not invalid");
        assertTrue(tokenStatusResponse.getActive(), "Token response status is not active");
        assertTrue(tokenStatusResponse.getPermissions() == null || tokenStatusResponse.getPermissions().isEmpty());
    }

    /**
     * As result host register permissions for specific resource set and requester.
     * Scenario for case when there is valid RPT but it has not enough permissions.
     */
    @Test(dependsOnMethods = {"testHostDetermineRptStatus1"})
    @Parameters({"umaAmHost"})
    public void testHostRegisterPermissions(final String umaAmHost) throws Exception {
        showTitle("testHostRegisterPermissions");
        umaRegisterResourceSetPermissionFlowHttpTest.testRegisterResourceSetPermission(umaAmHost);
    }

    /**
     * Host return ticket to requester
     */
    @Test(dependsOnMethods = {"testHostRegisterPermissions"})
    public void testHostReturnTicketToRequester() throws Exception {
        showTitle("testHostReturnTicketToRequester");
        // Return umaRegisterResourceSetPermissionFlowHttpTest.ticketForFullAccess in format specified in 3.1.2
    }

    //** 4 ******************************************************************************

    /**
     * Authorize requester to access resource set
     */
    @Test(dependsOnMethods = {"testHostReturnTicketToRequester"})
    @Parameters({"umaAmHost"})
    public void testRequesterAsksForAuthorization(final String umaAmHost) throws Exception {
        showTitle("testRequesterAsksForAuthorization");

        // Authorize RPT token to access permission ticket
        ClientResponse<AuthorizationResponse> authorizationResponse = null;
        try {
            RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, umaRegisterResourceSetPermissionFlowHttpTest.ticketForFullAccess);
            authorizationResponse = this.rptPermissionAuthorizationService.requestRptPermissionAuthorization(
                    "Bearer " + m_aat.getAccessToken(),
                    umaAmHost,
                    rptAuthorizationRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assertAuthorizationRequest(authorizationResponse);
    }

    //** 5 ******************************************************************************

    /**
     * Requesting party access protected resource at host via requester
     */
    @Test(dependsOnMethods = {"testRequesterAsksForAuthorization"})
    public void testRequesterAccessProtectedResourceWithEnoughPermissionsRpt() throws Exception {
        showTitle("testRequesterAccessProtectedResourceWithEonughPermissionsRpt");
        // Scenario for case when there is valid RPT in request with enough permissions
    }

    /**
     * Host determines RPT status
     */
    @Test(dependsOnMethods = {"testRequesterAccessProtectedResourceWithEnoughPermissionsRpt"})
    @Parameters({"umaAmHost"})
    public void testHostDetermineRptStatus2(final String umaAmHost) throws Exception {
        showTitle("testHostDetermineRptStatus2");

        String resourceSetId = umaRegisterResourceSetFlowHttpTest.resourceSetId;

        // Determine RPT token to status
        RptStatusResponse tokenStatusResponse = null;
        try {
            RptStatusRequest tokenStatusRequest = new RptStatusRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, resourceSetId);
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + m_pat.getAccessToken(),
                    tokenStatusRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(tokenStatusResponse);

        // Requester RPT has permission to access this resource set with scope http://photoz.example.com/dev/scopes/view. Hence host should allow him to download this resource.
    }

    /**
     * "
     * Host send protected resource to requester
     */
    @Test(dependsOnMethods = {"testHostDetermineRptStatus2"})
    public void testReturnProtectedResource() throws Exception {
        showTitle("testReturnProtectedResource");
        // RPT has enough permissions. Hence host should returns resource
    }
}