/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import io.jans.as.client.BaseTest;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.test.UmaTestUtil;
import io.jans.as.model.uma.wrapper.Token;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Test cases for the obtaining UMA PAT token flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
public class ObtainPatTokenFlowHttpTest extends BaseTest {

    protected Token pat;

    /**
     * Test for the obtaining UMA PAT token
     */
    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void testObtainPatTokenFlow(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        showTitle("testObtainPatTokenFlow");

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assertIt(pat);
    }

    /**
     * Test for the obtaining UMA PAT token using refresh token
     */
    //@Test(dependsOnMethods = {"testObtainPatTokenFlow"})
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void testObtainPatTokenUsingRefreshTokenFlow(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        showTitle("testObtainPatTokenUsingRefreshTokenFlow");

        // Request new access token using the refresh token.
        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient1.execRefreshToken(pat.getScope(), pat.getRefreshToken(), umaPatClientId, umaPatClientSecret);

        showClient(tokenClient1);
        AssertBuilder.tokenResponse(response1)
                .notNullRefreshToken()
                .check();
    }
}