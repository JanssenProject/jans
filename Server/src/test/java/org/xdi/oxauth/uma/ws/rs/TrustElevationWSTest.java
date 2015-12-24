package org.xdi.oxauth.uma.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.*;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.util.Arrays;

import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class TrustElevationWSTest extends BaseTest {

    private Token m_pat;
    private Token m_aat;
    private RPTResponse m_rpt;
    private ResourceSetResponse m_resourceSet;
    private ResourceSetPermissionTicket m_ticket;

    @Test
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret",
            "umaPatClientId", "umaPatClientSecret",
            "umaAatClientId", "umaAatClientSecret",
            "umaRedirectUri", "umaRptPath", "umaAmHost",
            "umaRegisterResourcePath"
    })
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret,
                     String umaAatClientId, String umaAatClientSecret,
                     String umaRedirectUri, String umaRptPath, String umaAmHost,
                     String umaRegisterResourcePath) {
        m_pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        m_aat = TUma.requestAat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);

        m_rpt = TUma.requestRpt(this, m_aat, umaRptPath, umaAmHost);

        UmaTestUtil.assert_(m_pat);
        UmaTestUtil.assert_(m_aat);
        UmaTestUtil.assert_(m_rpt);

        m_resourceSet = TUma.registerResourceSet(this, m_pat, umaRegisterResourcePath, UmaTestUtil.createResourceSet());
        UmaTestUtil.assert_(m_resourceSet);
    }

    @Test(dependsOnMethods = {"init"})
    @Parameters({"umaAmHost", "umaHost", "umaPermissionPath"})
    public void registerPermissionForRpt(final String umaAmHost, String umaHost, String umaPermissionPath) throws Exception {
        final UmaPermission r = new UmaPermission();
        r.setResourceSetId(m_resourceSet.getId());
        r.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view"));

        m_ticket = TUma.registerPermission(this, m_pat, umaAmHost, umaHost, r, umaPermissionPath);
        UmaTestUtil.assert_(m_ticket);
    }

    @Test(dependsOnMethods = {"registerPermissionForRpt"})
    @Parameters({"umaPermissionAuthorizationPath", "umaAmHost"})
    public void authorizePermission(String umaPermissionAuthorizationPath, String umaAmHost) {
        final RptAuthorizationRequest request = new RptAuthorizationRequest();
        request.setRpt(m_rpt.getRpt());
        request.setTicket(m_ticket.getTicket());
        request.setClaims(new ClaimTokenList().addToken(new ClaimToken("clientClaim", "clientValue")));

        final RptAuthorizationResponse response = TUma.requestAuthorization(this, umaPermissionAuthorizationPath, umaAmHost, m_aat, request);
        assertNotNull(response, "Token response status is null");

//        final RptIntrospectionResponse status = TUma.requestRptStatus(this, umaRptStatusPath, umaAmHost, m_pat, m_rpt.getRpt());
//        UmaTestUtil.assert_(status);
    }

    // use normal test method instead of @AfterClass because it will not work with ResourceRequestEnvironment seam class which is used
    // behind TUma wrapper.
    @Test(dependsOnMethods = {"_7_requesterAccessProtectedResourceWithEnoughPermissionsRpt"})
    @Parameters({"umaRegisterResourcePath"})
    public void cleanUp(String umaRegisterResourcePath) {
        if (m_resourceSet != null) {
            TUma.deleteResourceSet(this, m_pat, umaRegisterResourcePath, m_resourceSet.getId());
        }
    }


}
