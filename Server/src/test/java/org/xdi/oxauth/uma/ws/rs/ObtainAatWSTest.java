/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import org.junit.runners.Parameterized.Parameters;
import org.junit.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainAatWSTest extends BaseTest {

    private Token aat;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaAatClientId", "umaAatClientSecret", "umaRedirectUri"})
    public void requestAat(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                           String umaAatClientId, String umaAatClientSecret, String umaRedirectUri) {
        aat = TUma.requestAat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaAatClientId, umaAatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(aat);
    }

    @Test(dependsOnMethods = "requestAat")
    @Parameters({"tokenPath", "umaAatClientId", "umaAatClientSecret"})
    public void requestNewAatByRefreshTokne(String tokenPath, String umaAatClientId, String umaAatClientSecret) {
        final Token newAat = TUma.newTokenByRefreshToken(this, tokenPath, aat, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(newAat);
    }
}
