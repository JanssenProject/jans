/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import static org.testng.Assert.assertNotNull;

import java.net.URI;
import java.util.Arrays;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.ClaimToken;
import org.xdi.oxauth.model.uma.ClaimTokenList;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.ResourceSetResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class AccessProtectedResourceFlowWSTest extends BaseTest {

	@ArquillianResource
    private URI url;

    private static Token pat;
    private static Token aat;
    private static RPTResponse rpt;
    private static ResourceSetResponse resourceSet;
    private static PermissionTicket ticket;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri"})
    public void init_0(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                       String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        pat = TUma.requestPat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(pat);
    }

    @Test(dependsOnMethods = "init_0")
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri"})
    public void init_1(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                       String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        aat = TUma.requestAat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(aat);
    }

    @Test(dependsOnMethods = "init_1")
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret", "umaRedirectUri"})
    public void init_2(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                       String umaAatClientId, String umaAatClientSecret, String umaRedirectUri) {
        aat = TUma.requestAat(url, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(aat);
    }

    @Test(dependsOnMethods = {"init_2"})
    @Parameters({"umaRptPath", "umaAmHost"})
    public void init(String umaRptPath, String umaAmHost) {
        rpt = TUma.requestRpt(url, aat, umaRptPath, umaAmHost);
        UmaTestUtil.assert_(rpt);
    }

    /* ****************************************************************
       1. Register resource set
     */
    @Test(dependsOnMethods = {"init"})
    @Parameters({"umaRegisterResourcePath"})
    public void _1_registerResourceSet(String umaRegisterResourcePath) throws Exception {
        resourceSet = TUma.registerResourceSet(url, pat, umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(resourceSet);
    }

    /* ****************************************************************
       2. Requesting party access protected resource at host via requester
          RPT has no permissions to access protected resource here...
     */
    @Test(dependsOnMethods = {"_1_registerResourceSet"})
    public void _2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt() throws Exception {
        showTitle("_2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt");
        // do nothing, call must be made from host
    }

    /* ****************************************************************
       3. Host determines RPT status
    */
    @Test(dependsOnMethods = {"_2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt"})
    @Parameters({"umaRptStatusPath", "umaAmHost"})
    public void _3_hostDeterminesRptStatus(String umaRptStatusPath, String umaAmHost) throws Exception {
        final RptIntrospectionResponse status = TUma.requestRptStatus(url, umaRptStatusPath, umaAmHost, pat, rpt.getRpt());
        Assert.assertTrue(status.getActive(), "Token response status is not active");
        Assert.assertTrue(status.getPermissions() == null || status.getPermissions().isEmpty(), "Permissions list is not empty.");
    }

    /* ****************************************************************
       4. Registers permission for RPT
    */
    @Test(dependsOnMethods = {"_3_hostDeterminesRptStatus"})
    @Parameters({"umaAmHost", "umaHost", "umaPermissionPath"})
    public void _4_registerPermissionForRpt(final String umaAmHost, String umaHost, String umaPermissionPath) throws Exception {
        final UmaPermission r = new UmaPermission();
        r.setResourceSetId(resourceSet.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        ticket = TUma.registerPermission(url, pat, umaAmHost, umaHost, r, umaPermissionPath);
        UmaTestUtil.assert_(ticket);
    }

    @Test(dependsOnMethods = {"_4_registerPermissionForRpt"})
    @Parameters({"umaPermissionAuthorizationPath", "umaAmHost"})
    public void _5_authorizePermission(String umaPermissionAuthorizationPath, String umaAmHost) {
        final RptAuthorizationRequest request = new RptAuthorizationRequest();
        request.setRpt(rpt.getRpt());
        request.setTicket(ticket.getTicket());
        request.setClaims(new ClaimTokenList().addToken(new ClaimToken("clientClaim", "clientValue")));

        final RptAuthorizationResponse response = TUma.requestAuthorization(url, umaPermissionAuthorizationPath, umaAmHost, aat, request);
        assertNotNull(response, "Token response status is null");
    }

    /* ****************************************************************
         6. Host determines RPT status
    */
    @Test(dependsOnMethods = {"_5_authorizePermission"})
    @Parameters({"umaRptStatusPath", "umaAmHost"})
    public void _6_hostDeterminesRptStatus(String umaRptStatusPath, String umaAmHost) throws Exception {
        final RptIntrospectionResponse status = TUma.requestRptStatus(url, umaRptStatusPath, umaAmHost, pat, rpt.getRpt());
        UmaTestUtil.assert_(status);

    }

    /** *******************************************************************************
         7 Requesting party access protected resource at host via requester
     */
    @Test(dependsOnMethods = {"_6_hostDeterminesRptStatus"})
    public void _7_requesterAccessProtectedResourceWithEnoughPermissionsRpt() throws Exception {
        showTitle("_7_requesterAccessProtectedResourceWithEnoughPermissionsRpt");
        // Scenario for case when there is valid RPT in request with enough permissions
    }

    // use normal test method instead of @AfterClass because it will not work with ResourceRequestEnvironment seam class which is used
    // behind TUma wrapper.
    @Test(dependsOnMethods = {"_7_requesterAccessProtectedResourceWithEnoughPermissionsRpt"})
    @Parameters({"umaRegisterResourcePath"})
    public void cleanUp(String umaRegisterResourcePath) {
        if (resourceSet != null) {
            TUma.deleteResourceSet(url, pat, umaRegisterResourcePath, resourceSet.getId());
        }
    }


}
