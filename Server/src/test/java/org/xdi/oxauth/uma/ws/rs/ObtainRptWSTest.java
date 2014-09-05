/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainRptWSTest extends BaseTest {

    private Token m_aat;
    private String m_rpt;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri", "umaRegisterResourcePath"})
    public void init(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                     String umaPatClientId, String umaPatClientSecret, String umaRedirectUri, String umaRegisterResourcePath) {
        m_aat = TUma.requestAat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
    }

    @Test(dependsOnMethods = "init")
    @Parameters({"umaRptPath", "umaAmHost"})
    public void obtainRpt(String umaRptPath, String umaAmHost) {
        final RequesterPermissionTokenResponse r = TUma.requestRpt(this, m_aat, umaRptPath, umaAmHost);
        UmaTestUtil.assert_(r);
        m_rpt = r.getToken();
    }
}
