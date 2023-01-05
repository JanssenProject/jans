/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.uma.PermissionTicket;
import io.jans.as.model.uma.RPTResponse;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaPermission;
import io.jans.as.model.uma.UmaResourceResponse;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.uma.TUma;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Arrays;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class AccessProtectedResourceFlowWSTest extends BaseTest {

    @ArquillianResource
    private URI url;

    private static Token pat;
    private static RPTResponse rpt;
    private static UmaResourceResponse resource;
    private static PermissionTicket ticket;

    @Test
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
            "umaRedirectUri"})
    public void init_0(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                       String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        pat = TUma.requestPat(getApiTagetURI(url), authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
                umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assertIt(pat);
    }

    @Test(dependsOnMethods = "init_0")
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
            "umaRedirectUri"})
    public void init_1(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                       String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
    }

    @Test(dependsOnMethods = {"init_1"})
    @Parameters({"umaRptPath"})
    public void init(String umaRptPath) {
        rpt = TUma.requestRpt(getApiTagetURI(url), umaRptPath);
        UmaTestUtil.assertIt(rpt);
    }

    /*
     * **************************************************************** 1.
     * Register resource set
     */
    @Test(dependsOnMethods = {"init"})
    @Parameters({"umaRegisterResourcePath"})
    public void _1_registerResource(String umaRegisterResourcePath) throws Exception {
        resource = TUma.registerResource(getApiTagetURI(url), pat, umaRegisterResourcePath, UmaTestUtil.createResource());
        UmaTestUtil.assertIt(resource);
    }

    /*
     * **************************************************************** 2.
     * Requesting party access protected resource at host via requester RPT has
     * no permissions to access protected resource here...
     */
    @Test(dependsOnMethods = {"_1_registerResource"})
    public void _2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt() throws Exception {
        showTitle("_2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt");
        // do nothing, call must be made from host
    }

    /*
     * **************************************************************** 3. Host
     * determines RPT status
     */
    @Test(dependsOnMethods = {"_2_requesterAccessProtectedResourceWithNotEnoughPermissionsRpt"})
    @Parameters({"umaRptStatusPath"})
    public void _3_hostDeterminesRptStatus(String umaRptStatusPath) throws Exception {
        final RptIntrospectionResponse status = TUma.requestRptStatus(getApiTagetURI(url), umaRptStatusPath, rpt.getRpt());
        Assert.assertTrue(status.getActive(), "Token response status is not active");
        Assert.assertTrue(status.getPermissions() == null || status.getPermissions().isEmpty(),
                "Permissions list is not empty.");
    }

    /*
     * **************************************************************** 4.
     * Registers permission for RPT
     */
    @Test(dependsOnMethods = {"_3_hostDeterminesRptStatus"})
    @Parameters({"umaPermissionPath"})
    public void _4_registerPermissionForRpt(String umaPermissionPath)
            throws Exception {
        final UmaPermission r = new UmaPermission();
        r.setResourceId(resource.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        ticket = TUma.registerPermission(getApiTagetURI(url), pat, r, umaPermissionPath);
        UmaTestUtil.assertIt(ticket);
    }

    @Test(dependsOnMethods = {"_4_registerPermissionForRpt"})
    @Parameters({"umaPermissionAuthorizationPath",})
    public void _5_authorizePermission(String umaPermissionAuthorizationPath) {
        // todo
    }

    /*
     * **************************************************************** 6. Host
     * determines RPT status
     */
    @Test(dependsOnMethods = {"_5_authorizePermission"})
    @Parameters({"umaRptStatusPath"})
    public void _6_hostDeterminesRptStatus(String umaRptStatusPath) throws Exception {
        final RptIntrospectionResponse status = TUma.requestRptStatus(getApiTagetURI(url), umaRptStatusPath, rpt.getRpt());
        UmaTestUtil.assertIt(status);

    }

    /**
     * *******************************************************************************
     * 7 Requesting party access protected resource at host via requester
     */
    @Test(dependsOnMethods = {"_6_hostDeterminesRptStatus"})
    public void _7_requesterAccessProtectedResourceWithEnoughPermissionsRpt() throws Exception {
        showTitle("_7_requesterAccessProtectedResourceWithEnoughPermissionsRpt");
        // Scenario for case when there is valid RPT in request with enough
        // permissions
    }

    // use normal test method instead of @AfterClass because it will not work
    // with ResourceRequestEnvironment seam class which is used
    // behind TUma wrapper.
    @Test(dependsOnMethods = {"_7_requesterAccessProtectedResourceWithEnoughPermissionsRpt"})
    @Parameters({"umaRegisterResourcePath"})
    public void cleanUp(String umaRegisterResourcePath) {
        if (resource != null) {
            TUma.deleteResource(getApiTagetURI(url), pat, umaRegisterResourcePath, resource.getId());
        }
    }

}
