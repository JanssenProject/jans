package org.xdi.oxauth.ws.rs.uma;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseTest;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.UmaRptIntrospectionService;
import org.xdi.oxauth.client.uma.wrapper.UmaClient;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxauth.model.uma.UmaTestUtil;
import org.xdi.oxauth.model.uma.wrapper.Token;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/04/2015
 */

public class TrustElevationHttpTest extends BaseTest {

    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected UmaRegisterPermissionFlowHttpTest registerPermissionTest;

    protected UmaRptIntrospectionService rptStatusService;

    protected Token m_pat;

    @Test
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void trustElevation(final String umaMetaDataUrl,
                     final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl).getMetadata();
        UmaTestUtil.assert_(this.metadata);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerPermissionTest = new UmaRegisterPermissionFlowHttpTest(this.metadata);

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadata);

        m_pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret);

        UmaTestUtil.assert_(m_pat);

        final List<String> rsScopes = Arrays.asList("http://gluu.example.com/dev/scopes/view", "http://gluu.example.com/dev/scopes/all");

        this.registerResourceTest.pat = m_pat;
        final String resourceId = this.registerResourceTest.registerResource(rsScopes);

        this.registerPermissionTest.registerResourceTest = registerResourceTest;
        this.registerPermissionTest.registerResourcePermission(resourceId, rsScopes);

        /**
        RptIntrospectionResponse rptStatus = this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(),
                this.umaObtainRptTokenFlowHttpTest.rptToken, "");

        RptAuthorizationRequest rptAuthorizationRequest = new RptAuthorizationRequest(this.umaObtainRptTokenFlowHttpTest.rptToken, permissionFlowTest.ticket);

        try {
            RptAuthorizationResponse authorizationResponse = this.rptPermissionAuthorizationService.requestRptPermissionAuthorization(
                    "Bearer " + m_aat.getAccessToken(), rptAuthorizationRequest);
        } catch (ClientResponseFailure ex) {
            System.err.println(ex.getResponse().getEntity(String.class));
            throw ex;
        }

        rptStatus = this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(),
                       this.umaObtainRptTokenFlowHttpTest.rptToken, "");
        **/
    }
}
