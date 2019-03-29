/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.ws.rs.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.gluu.oxauth.client.TokenClient;
import org.gluu.oxauth.client.TokenResponse;
import org.gluu.oxauth.client.uma.wrapper.UmaClient;
import org.gluu.oxauth.model.uma.wrapper.Token;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.model.uma.UmaTestUtil;

/**
 * Test cases for the obtaining UMA PAT token flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
public class ObtainPatTokenFlowHttpTest extends BaseTest {

    protected Token m_pat;

    /**
     * Test for the obtaining UMA PAT token
     */
    @Test
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void testObtainPatTokenFlow(final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        showTitle("testObtainPatTokenFlow");

        m_pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);
        UmaTestUtil.assert_(m_pat);
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
        TokenResponse response1 = tokenClient1.execRefreshToken(m_pat.getScope(), m_pat.getRefreshToken(), umaPatClientId, umaPatClientSecret);

        showClient(tokenClient1);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");
    }
}