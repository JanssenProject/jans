package org.xdi.oxauth.uma.ws.rs;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.TUma;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainPatWSTest extends BaseTest {

    private Token m_pat;

    @Test
    @Parameters({"authorizePath", "tokenPath",
            "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret", "umaRedirectUri"})
    public void requestPat(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                           String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        m_pat = TUma.requestPat(this, authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId, umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assert_(m_pat);
    }

    @Test(dependsOnMethods = "requestPat")
    @Parameters({"tokenPath", "umaPatClientId", "umaPatClientSecret"})
    public void requestNewPatByRefreshTokne(String tokenPath, String umaPatClientId, String umaPatClientSecret) {
        final Token newPat = TUma.newTokenByRefreshToken(this, tokenPath, m_pat, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(newPat);
    }
}
