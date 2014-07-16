package org.xdi.oxauth.ws.rs;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.core.BaseClientResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.IdClient;
import org.xdi.oxauth.client.uma.RequesterPermissionTokenService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.uma.AuthorizationResponse;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/06/2013
 */

public class IdGenWsHttpTest extends BaseTest {

    protected Token m_aat;
    protected String m_rpt;
    protected MetadataConfiguration m_metadataConfiguration;
    protected String m_umaAmHost;

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaAatClientId", "umaAatClientSecret", "umaAmHost"})
    public void init(final String umaMetaDataUrl,
                     final String umaAatClientId, final String umaAatClientSecret, String umaAmHost) throws Exception {
        m_metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        UmaTestUtil.assert_(m_metadataConfiguration);

        m_umaAmHost = umaAmHost;

        m_aat = UmaClient.requestAat(tokenEndpoint, umaAatClientId, umaAatClientSecret);
        UmaTestUtil.assert_(m_aat);

        final RequesterPermissionTokenService rptService = UmaClientFactory.instance().createRequesterPermissionTokenService(m_metadataConfiguration);

        // Get requester permission token
        RequesterPermissionTokenResponse requesterPermissionTokenResponse = null;
        try {
            requesterPermissionTokenResponse = rptService.getRequesterPermissionToken("Bearer " + m_aat.getAccessToken(), umaAmHost);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assert_(requesterPermissionTokenResponse);
        m_rpt = requesterPermissionTokenResponse.getToken();
    }

    //    @Test(dependsOnMethods = {"init"})
    @Test
    public void test() {
        final String prefix = "@!1111";

        try {
            final Id clientId = IdClient.generateIdWithRpt(getIdGenEndpoint(), prefix, IdType.CLIENTS, m_rpt);
            // should never run till this line: rpt is not authorized, therefore must fail with ClientResponseFailure
            Assert.assertTrue(false);
        } catch (ClientResponseFailure e) {
            Assert.assertEquals(e.getResponse().getStatus(), 403); // forbidden : rpt is not authorized yet

            final BaseClientResponse<ResourceSetPermissionTicket> r = (BaseClientResponse) e.getResponse();
            r.setReturnType(ResourceSetPermissionTicket.class);
            final ResourceSetPermissionTicket ticket = r.getEntity();
            UmaTestUtil.assert_(ticket);

            authorizeRpt(ticket.getTicket());

            final Id clientId = IdClient.generateIdWithRpt(getIdGenEndpoint(), prefix, IdType.CLIENTS, m_rpt);
            System.out.println("Client generated ID: " + clientId);
            Assert.assertTrue(clientId != null && StringUtils.isNotBlank(clientId.getId()) && clientId.getId().startsWith(prefix));

            final Id peopleId = IdClient.generateIdWithRpt(getIdGenEndpoint(), prefix, IdType.PEOPLE, m_rpt);
            System.out.println("People generated ID: " + peopleId);
            Assert.assertTrue(peopleId != null && StringUtils.isNotBlank(peopleId.getId()) && peopleId.getId().startsWith(prefix));
        }
    }

    private void authorizeRpt(String p_ticket) {
        // Authorize RPT token to access permission ticket
        ClientResponse<AuthorizationResponse> authorizationResponse = null;
        try {
            RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(m_rpt, p_ticket);
            authorizationResponse = UmaClientFactory.instance().createAuthorizationRequestService(m_metadataConfiguration).requestRptPermissionAuthorization(
                    "Bearer " + m_aat.getAccessToken(),
                    m_umaAmHost,
                    rptAuthorizationRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        UmaTestUtil.assertAuthorizationRequest(authorizationResponse);
    }

}
