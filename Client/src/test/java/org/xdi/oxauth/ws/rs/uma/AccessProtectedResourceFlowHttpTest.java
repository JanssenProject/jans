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
import org.xdi.oxauth.client.uma.UmaRptAuthorizationService;
import org.xdi.oxauth.client.uma.UmaRptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import static org.testng.Assert.*;

/**
 * Test flow for the accessing protected resource (HTTP)
 *
 * @author Yuriy Movchan Date: 10/22/2012
 */
public class AccessProtectedResourceFlowHttpTest extends BaseTest {

    protected UmaConfiguration metadataConfiguration;

    //protected ObtainRptTokenFlowHttpTest umaObtainRptTokenFlowHttpTest;

    protected RegisterResourceFlowHttpTest umaRegisterResourceFlowHttpTest;
    protected UmaRegisterPermissionFlowHttpTest permissionFlowHttpTest;

    protected UmaRptStatusService rptStatusService;
    protected UmaRptAuthorizationService rptPermissionAuthorizationService;

    protected Token m_aat;
    protected Token m_pat;

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        UmaTestUtil.assert_(this.metadataConfiguration);

        //this.umaObtainRptTokenFlowHttpTest = new ObtainRptTokenFlowHttpTest(this.metadataConfiguration);
        this.umaRegisterResourceFlowHttpTest = new RegisterResourceFlowHttpTest(this.metadataConfiguration);
        this.permissionFlowHttpTest = new UmaRegisterPermissionFlowHttpTest(this.metadataConfiguration);

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadataConfiguration);
        this.rptPermissionAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(metadataConfiguration);
    }

    //** 1 ******************************************************************************

    /**
     * Host obtains PAT
     */
    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void testHostObtainPat(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        showTitle("testHostObtainPat");
        m_pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(m_pat);

        // Init UmaPatTokenAwareHttpTest test
        this.umaRegisterResourceFlowHttpTest.m_pat = this.m_pat;

        // Init UmaRegisterResourcePermissionFlowHttpTest test
        this.permissionFlowHttpTest.registerResourceTest = this.umaRegisterResourceFlowHttpTest;
    }

    /**
     * Host registers resource set description
     */
    @Test(dependsOnMethods = {"testHostObtainPat"})
    public void testHostRegisterResource() throws Exception {
        showTitle("testHostRegisterResource");
        this.umaRegisterResourceFlowHttpTest.testRegisterResource();
    }

    //** 2 ******************************************************************************

    /**
     * Requester obtains AAT token
     */
    @Test(dependsOnMethods = {"testHostRegisterResource"})
    @Parameters({"umaAatClientId", "umaAatClientSecret"})
    public void testRequesterObtainAat(final String umaAatClientId, final String umaAatClientSecret) throws Exception {
        showTitle("testRequesterObtainAat");
        m_aat = UmaClient.requestAat(tokenEndpoint, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(m_aat);

        // Init UmaPatTokenAwareHttpTest test
        //this.umaObtainRptTokenFlowHttpTest.m_aat = this.m_aat;
    }

    /**
     * Requester obtains RPT token
     */
    @Test(dependsOnMethods = {"testRequesterObtainAat"})
    @Parameters({"umaAmHost"})
    public void testRequesterObtainsRpt(final String umaAmHost) throws Exception {
        showTitle("testRequesterObtainsRpt");
        //this.umaObtainRptTokenFlowHttpTest.testObtainRptTokenFlow(umaAmHost);
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

        String resourceId = umaRegisterResourceFlowHttpTest.resourceId;

        // Determine RPT token to status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            //tokenStatusResponse = this.rptStatusService.requestRptStatus(
            //        "Bearer " + m_pat.getAccessToken(),
            //        this.umaObtainRptTokenFlowHttpTest.rptToken, "");
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
        permissionFlowHttpTest.testRegisterPermission(umaAmHost);
    }

    /**
     * Host return ticket to requester
     */
    @Test(dependsOnMethods = {"testHostRegisterPermissions"})
    public void testHostReturnTicketToRequester() throws Exception {
        showTitle("testHostReturnTicketToRequester");
        // Return permissionFlowHttpTest.ticketForFullAccess in format specified in 3.1.2
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
            RptAuthorizationRequest rptAuthorizationRequest = null;// new RptAuthorizationRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, permissionFlowHttpTest.ticketForFullAccess);

            authorizationResponse = this.rptPermissionAuthorizationService.requestRptAuthorization(
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

        // Determine RPT token to status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            //tokenStatusResponse = this.rptStatusService.requestRptStatus(
            //        "Bearer " + m_pat.getAccessToken(),
            //        this.umaObtainRptTokenFlowHttpTest.rptToken, "");
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