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
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.UmaRptIntrospectionService;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * Test flow for the accessing protected resource (HTTP)
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */
public class AccessProtectedResourceFlowHttpTest extends BaseTest {

    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected UmaRegisterPermissionFlowHttpTest permissionFlowTest;

    protected UmaRptIntrospectionService rptStatusService;

    protected Token pat;

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientExecutor(true)).getMetadata();
        UmaTestUtil.assert_(this.metadata);

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientExecutor(true));
        UmaTestUtil.assert_(pat);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.pat = this.pat;

        this.permissionFlowTest = new UmaRegisterPermissionFlowHttpTest(this.metadata);
        this.permissionFlowTest.registerResourceTest = this.registerResourceTest;

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadata, clientExecutor(true));
    }

    /**
     * Register resource
     */
    @Test
    public void registerResource() throws Exception {
        showTitle("registerResource");
        this.registerResourceTest.addResource();
    }

    /**
     * RS registers permissions for specific resource.
     */
    @Test(dependsOnMethods = {"registerResource"})
    public void rsRegisterPermissions() throws Exception {
        showTitle("rsRegisterPermissions");
        permissionFlowTest.testRegisterPermission();
    }

    /**
     * RP requests RPT with ticket
     */
    @Test(dependsOnMethods = {"rsRegisterPermissions"})
    public void requestRpt() throws Exception {
        showTitle("requestRpt");
        String ticket = permissionFlowTest.ticket;
        System.out.println(ticket);
        // Return permissionFlowTest.ticket in format specified in 3.1.2
    }

    //** 4 ******************************************************************************

    /**
     * Authorize requester to access resource set
     */
    @Test(dependsOnMethods = {"testHostReturnTicketToRequester"})
    public void testRequesterAsksForAuthorization() throws Exception {
        showTitle("testRequesterAsksForAuthorization");

        // Authorize RPT token to access permission ticket


//        UmaTestUtil.assertAuthorizationRequest(authorizationResponse);
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
    @Parameters()
    public void testHostDetermineRptStatus2() throws Exception {
        showTitle("testHostDetermineRptStatus2");

        // Determine RPT token to status
        RptIntrospectionResponse tokenStatusResponse = null;
        try {
            //tokenStatusResponse = this.rptStatusService.requestRptStatus(
            //        "Bearer " + pat.getAccessToken(),
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