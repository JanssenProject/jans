package org.xdi.oxauth.ws.rs.uma;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientResponseFailure;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.RequesterPermissionTokenService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * Test cases for the obtaining UMA requester permission token flow (HTTP)
 *
 * @author Yuriy Movchan Date: 10/03/2012
 */
public class ObtainRptTokenFlowHttpTest extends BaseTest {

    protected MetadataConfiguration metadataConfiguration;

    protected Token m_aat;
    protected String rptToken;

    public ObtainRptTokenFlowHttpTest() {
    }

    public ObtainRptTokenFlowHttpTest(MetadataConfiguration metadataConfiguration) {
        this.metadataConfiguration = metadataConfiguration;
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaAatClientId", "umaAatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaAatClientId, final String umaAatClientSecret) throws Exception {
        if (this.metadataConfiguration == null) {
            this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
            UmaTestUtil.assert_(this.metadataConfiguration);
        }

        m_aat = UmaClient.requestAat(tokenEndpoint, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(m_aat);
    }

    /**
     * Test for the obtaining UMA RPT token
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testObtainRptTokenFlow(final String umaAmHost) throws Exception {
        showTitle("testObtainRptTokenFlow");

        RequesterPermissionTokenService requesterPermissionTokenService = UmaClientFactory.instance().createRequesterPermissionTokenService(this.metadataConfiguration);

        // Get requester permission token
        RequesterPermissionTokenResponse requesterPermissionTokenResponse = null;
        try {
            requesterPermissionTokenResponse = requesterPermissionTokenService.getRequesterPermissionToken("Bearer " + m_aat.getAccessToken(), umaAmHost);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(requesterPermissionTokenResponse);

        this.rptToken = requesterPermissionTokenResponse.getToken();
    }

    /**
     * Test for the obtaining UMA RPT token
     */
    @Test
    @Parameters({"umaAmHost"})
    public void testObtainRptTokenFlowWithInvalidAat(final String umaAmHost) throws Exception {
        showTitle("testObtainRptTokenFlowWithInvalidAat");

        RequesterPermissionTokenService requesterPermissionTokenService = UmaClientFactory.instance().createRequesterPermissionTokenService(this.metadataConfiguration);

        // Get requester permission token
        RequesterPermissionTokenResponse requesterPermissionTokenResponse = null;
        try {
            requesterPermissionTokenResponse = requesterPermissionTokenService.getRequesterPermissionToken("Bearer " + m_aat.getAccessToken() + "_invalid", umaAmHost);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            assertEquals(ex.getResponse().getStatus(), Response.Status.UNAUTHORIZED.getStatusCode(), "Unexpected response status");
        }

        assertNull(requesterPermissionTokenResponse, "Requester permission token response is not null");
    }
}