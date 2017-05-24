package org.xdi.oxauth.ws.rs.uma;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.UmaRptAuthorizationService;
import org.xdi.oxauth.client.uma.UmaRptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class TrustElevationHttpTest extends BaseTest {

    protected UmaConfiguration metadataConfiguration;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected UmaRegisterPermissionFlowHttpTest registerPermissionTest;

    protected UmaRptStatusService rptStatusService;
    protected UmaRptAuthorizationService rptPermissionAuthorizationService;

    protected Token m_pat;

    @Test
    @Parameters({"umaMetaDataUrl", "umaAmHost",
            "umaPatClientId", "umaPatClientSecret"
    })
    public void trustElevation(final String umaMetaDataUrl, final String umaAmHost,
                     final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        this.metadataConfiguration = UmaClientFactory.instance().createMetaDataConfigurationService(umaMetaDataUrl).getMetadataConfiguration();
        UmaTestUtil.assert_(this.metadataConfiguration);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadataConfiguration);
        this.registerPermissionTest = new UmaRegisterPermissionFlowHttpTest(this.metadataConfiguration);

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadataConfiguration);
        this.rptPermissionAuthorizationService = UmaClientFactory.instance().createAuthorizationRequestService(metadataConfiguration);

        m_pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);

        UmaTestUtil.assert_(m_pat);

        final List<String> rsScopes = Arrays.asList("http://gluu.example.com/dev/scopes/view", "http://gluu.example.com/dev/scopes/all");

        this.registerResourceTest.m_pat = m_pat;
        final String resourceId = this.registerResourceTest.registerResource(rsScopes);

        this.registerPermissionTest.registerResourceTest = registerResourceTest;
        this.registerPermissionTest.registerResourcePermission(umaAmHost, resourceId, rsScopes);

        /**
        RptIntrospectionResponse rptStatus = this.rptStatusService.requestRptStatus("Bearer " + m_pat.getAccessToken(),
                this.umaObtainRptTokenFlowHttpTest.rptToken, "");

        RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, permissionFlowHttpTest.ticketForFullAccess);

        try {
            RptAuthorizationResponse authorizationResponse = this.rptPermissionAuthorizationService.requestRptPermissionAuthorization(
                    "Bearer " + m_aat.getAccessToken(), umaAmHost, rptAuthorizationRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        rptStatus = this.rptStatusService.requestRptStatus("Bearer " + m_pat.getAccessToken(),
                       this.umaObtainRptTokenFlowHttpTest.rptToken, "");
        **/
    }
}
