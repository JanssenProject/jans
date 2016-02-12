package org.xdi.oxauth.ws.rs.uma;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.CreateGatService;
import org.xdi.oxauth.client.uma.RptAuthorizationRequestService;
import org.xdi.oxauth.client.uma.RptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.GatRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/01/2016
 */

public class GatFlowHttpTest extends BaseTest {

    protected UmaConfiguration metadataConfiguration;

    protected ObtainRptTokenFlowHttpTest umaObtainRptTokenFlowHttpTest;

    protected RegisterResourceSetFlowHttpTest umaRegisterResourceSetFlowHttpTest;
    protected RegisterResourceSetPermissionFlowHttpTest umaRegisterResourceSetPermissionFlowHttpTest;

    protected RptStatusService rptStatusService;
    protected RptAuthorizationRequestService authorizationService;

    protected Token aat;
    protected Token pat;

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl, clientExecutor()).getMetadataConfiguration();
        UmaTestUtil.assert_(this.metadataConfiguration);

        this.umaObtainRptTokenFlowHttpTest = new ObtainRptTokenFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceSetFlowHttpTest = new RegisterResourceSetFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceSetPermissionFlowHttpTest = new RegisterResourceSetPermissionFlowHttpTest(this.metadataConfiguration);

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadataConfiguration);
        this.authorizationService = UmaClientFactory.instance().createAuthorizationRequestService(metadataConfiguration);
    }

    //** 1 ******************************************************************************

    /**
     * Host obtains PAT
     */
    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void testHostObtainPat(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        showTitle("testHostObtainPat");
        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(pat);

        // Init UmaPatTokenAwareHttpTest test
        this.umaRegisterResourceSetFlowHttpTest.m_pat = this.pat;

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
    @Parameters({"umaAatClientId", "umaAatClientSecret"})
    public void testRequesterObtainAat(final String umaAatClientId, final String umaAatClientSecret) throws Exception {
        showTitle("testRequesterObtainAat");
        aat = UmaClient.requestAat(tokenEndpoint, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(aat);

        // Init UmaPatTokenAwareHttpTest test
        this.umaObtainRptTokenFlowHttpTest.m_aat = this.aat;
    }

    /**
     * Requester obtains GAT token
     */
    @Test(dependsOnMethods = {"testRequesterObtainAat"})
    @Parameters({"umaAmHost"})
    public void testRequesterObtainsRpt(final String umaAmHost) throws Exception {
        showTitle("testRequesterObtainsRpt");

        CreateGatService gatService = UmaClientFactory.instance().createGatService(this.metadataConfiguration, clientExecutor(true));

        GatRequest gatRequest = new GatRequest();

        gatService.createGAT("Bearer " + aat.getAccessToken(), umaAmHost, gatRequest);

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
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + pat.getAccessToken(),
                    this.umaObtainRptTokenFlowHttpTest.rptToken, "");
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
        RptAuthorizationResponse authorizationResponse = null;
        try {
            RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, umaRegisterResourceSetPermissionFlowHttpTest.ticketForFullAccess);

            authorizationResponse = this.authorizationService.requestRptPermissionAuthorization(
                    "Bearer " + aat.getAccessToken(),
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

        // Determine RPT token to status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + pat.getAccessToken(),
                    this.umaObtainRptTokenFlowHttpTest.rptToken, "");
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