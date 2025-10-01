/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.server.BaseTest;
import io.jans.as.server.model.uma.TUma;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 18/03/2013
 */

public class ObtainPatWSTest extends BaseTest {

    @ArquillianResource
    private URI url;

    private static Token pat;

    @Test
    @Parameters({"authorizePath", "tokenPath", "umaUserId", "umaUserSecret", "umaPatClientId", "umaPatClientSecret",
            "umaRedirectUri"})
    public void requestPat(String authorizePath, String tokenPath, String umaUserId, String umaUserSecret,
                           String umaPatClientId, String umaPatClientSecret, String umaRedirectUri) {
        pat = TUma.requestPat(getApiTagetURI(url), authorizePath, tokenPath, umaUserId, umaUserSecret, umaPatClientId,
                umaPatClientSecret, umaRedirectUri);
        UmaTestUtil.assertIt(pat);
    }

    @Test(dependsOnMethods = "requestPat")
    @Parameters({"tokenPath", "umaPatClientId", "umaPatClientSecret"})
    public void requestNewPatByRefreshTokne(String tokenPath, String umaPatClientId, String umaPatClientSecret) {
        final Token newPat = TUma.newTokenByRefreshToken(getApiTagetURI(url), tokenPath, pat, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assertIt(newPat);
    }
}
