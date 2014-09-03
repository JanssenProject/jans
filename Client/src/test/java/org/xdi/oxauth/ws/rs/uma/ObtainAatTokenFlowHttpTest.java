package org.xdi.oxauth.ws.rs.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenResponse;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * Test cases for the obtaining UMA AAT token flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
public class ObtainAatTokenFlowHttpTest extends BaseTest {

    protected Token m_aat;

    /**
     * Test for the obtaining UMA AAT token
     */
    @Test
    @Parameters({"umaUserId", "umaAatClientSecret"})
    public void testObtainAatTokenFlow(final String umaAatClientId, final String umaAatClientSecret) throws Exception {
        showTitle("testObtainAatTokenFlow");

        m_aat = UmaClient.requestAat(tokenEndpoint, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(m_aat);
    }

    /**
     * Test for the obtaining UMA AAT token using refresh token
     */
    //@Test(dependsOnMethods = {"testObtainAatTokenFlow"})
    @Parameters({"umaAatClientId", "umaAatClientSecret"})
    public void testObtainAatTokenUsingRefreshTokenFlow(final String umaAatClientId, final String umaAatClientSecret) throws Exception {
        showTitle("testObtainAatTokenUsingRefreshTokenFlow");

        // Request new access token using the refresh token.
        TokenClient tokenClient1 = new TokenClient(tokenEndpoint);
        TokenResponse response1 = tokenClient1.execRefreshToken(m_aat.getScope(), m_aat.getRefreshToken(), umaAatClientId, umaAatClientSecret);

        showClient(tokenClient1);
        assertEquals(response1.getStatus(), 200, "Unexpected response code: " + response1.getStatus());
        assertNotNull(response1.getEntity(), "The entity is null");
        assertNotNull(response1.getAccessToken(), "The access token is null");
        assertNotNull(response1.getTokenType(), "The token type is null");
        assertNotNull(response1.getRefreshToken(), "The refresh token is null");
        assertNotNull(response1.getScope(), "The scope is null");
    }
}