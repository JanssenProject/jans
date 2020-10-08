package org.gluu.oxauth.ws.rs.uma;

import com.google.common.collect.Lists;
import org.gluu.oxauth.BaseTest;
import org.gluu.oxauth.client.RegisterClient;
import org.gluu.oxauth.client.RegisterRequest;
import org.gluu.oxauth.client.RegisterResponse;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaRptIntrospectionService;
import org.gluu.oxauth.client.uma.UmaTokenService;
import org.gluu.oxauth.client.uma.wrapper.UmaClient;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.uma.RptIntrospectionResponse;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaTokenResponse;
import io.jans.as.model.uma.wrapper.Token;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.List;

import static org.gluu.oxauth.model.uma.UmaTestUtil.assert_;
import static org.gluu.oxauth.ws.rs.uma.AccessProtectedResourceFlowHttpTest.encodeCredentials;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaSpontaneousScopeHttpTest extends BaseTest {

    private static final String REDIRECT_URI = "https://cb.example.com";
    public static final String USER_2_SCOPE = "/user/2";

    private UmaMetadata metadata;

    private RegisterResourceFlowHttpTest registerResourceTest;
    private UmaRegisterPermissionFlowHttpTest permissionFlowTest;

    private UmaRptIntrospectionService rptStatusService;
    private UmaTokenService tokenService;

    private Token pat;
    private String rpt;
    private RegisterResponse clientResponse;

    private void registerClient() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<String> scopes = Lists.newArrayList(
                "openid", "uma_protection", "profile", "address", "email", "phone", "user_name"
        );

        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "UMA Spontaneous scope test", Lists.newArrayList(REDIRECT_URI));
        registerRequest.setResponseTypes(Arrays.asList(ResponseType.values()));
        registerRequest.setGrantTypes(Arrays.asList(GrantType.values()));
        registerRequest.setScope(scopes);
        registerRequest.setAllowSpontaneousScopes(true); // allow spontaneous scopes (which are off by default)

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setExecutor(clientExecutor(true));
        registerClient.setRequest(registerRequest);
        clientResponse = registerClient.exec();

        showClient(registerClient);
        assertEquals(clientResponse.getStatus(), 200, "Unexpected response code: " + clientResponse.getEntity());
        assertNotNull(clientResponse.getClientId());
        assertNotNull(clientResponse.getClientSecret());
        assertNotNull(clientResponse.getRegistrationAccessToken());
        assertNotNull(clientResponse.getClientIdIssuedAt());
        assertNotNull(clientResponse.getClientSecretExpiresAt());
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true)).getMetadata();
        assert_(this.metadata);

        registerClient();

        pat = UmaClient.requestPat(tokenEndpoint, clientResponse.getClientId(), clientResponse.getClientSecret(), clientExecutor(true));
        assert_(pat);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.pat = this.pat;

        this.permissionFlowTest = new UmaRegisterPermissionFlowHttpTest(this.metadata);
        this.permissionFlowTest.registerResourceTest = this.registerResourceTest;

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadata, clientEngine(true));
        this.tokenService = UmaClientFactory.instance().createTokenService(metadata, clientEngine(true));
    }

    @Test
    public void registerResource() throws Exception {
        showTitle("registerResource");
        this.registerResourceTest.registerResource(Lists.newArrayList("^/user/.+$"));
    }


    @Test(dependsOnMethods = {"registerResource"})
    public void registerPermissions() throws Exception {
        showTitle("registerPermissions");
        permissionFlowTest.registerResourcePermission(Lists.newArrayList(USER_2_SCOPE));
    }

    @Test(dependsOnMethods = {"registerPermissions"})
    public void successfulRptRequest() throws Exception {
        showTitle("successfulRptRequest");

        UmaTokenResponse response = tokenService.requestRpt(
                "Basic " + encodeCredentials(clientResponse.getClientId(), clientResponse.getClientSecret()),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                permissionFlowTest.ticket,
                null, null, null, null, null);
        assert_(response);

        this.rpt = response.getAccessToken();
    }

    @Test(dependsOnMethods = {"successfulRptRequest"})
    @Parameters()
    public void rptStatus() {
        showTitle("rptStatus");
        final RptIntrospectionResponse status = this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(), rpt, "");
        assert_(status);

        // at the end scope registered by permission must be present in RPT permission with scope allowed by spontaneous scope check
        assertTrue(status.getPermissions().get(0).getScopes().contains(USER_2_SCOPE));
    }
}
