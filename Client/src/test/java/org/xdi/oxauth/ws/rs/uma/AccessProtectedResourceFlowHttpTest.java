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
import org.xdi.oxauth.client.uma.UmaTokenService;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.UmaTokenResponse;
import org.xdi.oxauth.model.uma.wrapper.Token;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

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
    protected UmaTokenService tokenService;

    protected Token pat;
    protected String rpt;

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
        this.tokenService = UmaClientFactory.instance().createTokenService(metadata, clientExecutor(true));
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
     * RP requests RPT with ticket and gets needs_info error (not all claims are provided, so redirect to claims-gathering endpoint)
     */
    @Test(dependsOnMethods = {"rsRegisterPermissions"})
    public void requestRptAndGetNeedsInfo() throws Exception {
        showTitle("requestRptAndGetNeedsInfo");

        try {
            tokenService.requestRpt(
                    "Bearer" + pat.getAccessToken(),
                    GrantType.OXAUTH_UMA_TICKET.getValue(),
                    permissionFlowTest.ticket,
                    null, null, null, null, null);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.BAD_REQUEST.getStatusCode(), "Unexpected response status");

            // todo for Gene : parse needs_info
        }
    }

    // todo for Gene : claims-gathering method - interaction with Selenium (emulate user behavior)

    /**
     * Request RPT with all claims provided
     */
    @Test(dependsOnMethods = {"requestRptAndGetNeedsInfo"})
    public void successfulRptRequest() throws Exception {
        showTitle("successfulRptRequest");

        UmaTokenResponse response = tokenService.requestRpt("Bearer" + pat.getAccessToken(),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                permissionFlowTest.ticket,
                null, null, null, null, null);
        UmaTestUtil.assert_(response);

        this.rpt = response.getAccessToken();
    }

    /**
     * RPT status request
     */
    @Test(dependsOnMethods = {"successfulRptRequest"})
    @Parameters()
    public void rptStatus() throws Exception {
        showTitle("rptStatus");
        UmaTestUtil.assert_(this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(), rpt, ""));
    }
}