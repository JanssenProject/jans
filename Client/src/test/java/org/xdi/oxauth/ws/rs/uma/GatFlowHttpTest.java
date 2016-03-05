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

    protected RegisterResourceSetFlowHttpTest umaRegisterResourceSetFlowHttpTest;
    protected RegisterResourceSetPermissionFlowHttpTest umaRegisterResourceSetPermissionFlowHttpTest;

    protected RptStatusService rptStatusService;
    protected RptAuthorizationRequestService authorizationService;

    protected Token aat;
    protected Token pat;
    protected String gat;

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl, clientExecutor()).getMetadataConfiguration();
        UmaTestUtil.assert_(this.metadataConfiguration);

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

        gat = gatService.createGAT("Bearer " + aat.getAccessToken(), umaAmHost, gatRequest).getRpt();
    }

    /**
     * Host determines GAT status
     */
    @Test(dependsOnMethods = {"testRequesterObtainsRpt"})
    public void testHostDetermineRptStatus1() throws Exception {
        showTitle("testHostDetermineRptStatus1");

        // Determine GAT status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + pat.getAccessToken(),
                    gat, "");
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

        // Authorize GAT to access permission ticket
        RptAuthorizationResponse authorizationResponse = null;
        try {
            RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(gat, umaRegisterResourceSetPermissionFlowHttpTest.ticketForFullAccess);

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
     * Host determines GAT status
     */
    @Test(dependsOnMethods = {"testRequesterAccessProtectedResourceWithEnoughPermissionsRpt"})
    public void testHostDetermineRptStatus2() throws Exception {
        showTitle("testHostDetermineRptStatus2");

        // Determine GAT status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            tokenStatusResponse = this.rptStatusService.requestRptStatus(
                    "Bearer " + pat.getAccessToken(),
                    gat, "");
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(tokenStatusResponse);
 }
}