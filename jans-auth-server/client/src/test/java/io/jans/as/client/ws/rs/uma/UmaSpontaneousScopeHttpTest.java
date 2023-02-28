/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import com.google.common.collect.Lists;
import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.client.uma.UmaTokenService;
import io.jans.as.client.uma.wrapper.UmaClient;
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

import static io.jans.as.test.UmaTestUtil.assertIt;
import static org.junit.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaSpontaneousScopeHttpTest extends BaseTest {

    public static final String USER_2_SCOPE = "/user/2";
    private static final String REDIRECT_URI = "https://cb.example.com";
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
        registerClient.setExecutor(clientEngine(true));
        registerClient.setRequest(registerRequest);
        clientResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(clientResponse).created().check();
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl"})
    public void init(final String umaMetaDataUrl) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true)).getMetadata();
        assertIt(this.metadata);

        registerClient();

        pat = UmaClient.requestPat(tokenEndpoint, clientResponse.getClientId(), clientResponse.getClientSecret(), clientEngine(true));
        assertIt(pat);

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
                "Basic " + AccessProtectedResourceFlowHttpTest.encodeCredentials(clientResponse.getClientId(), clientResponse.getClientSecret()),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                permissionFlowTest.ticket,
                null, null, null, null, null);
        assertIt(response);

        this.rpt = response.getAccessToken();
    }

    @Test(dependsOnMethods = {"successfulRptRequest"})
    @Parameters()
    public void rptStatus() {
        showTitle("rptStatus");
        final RptIntrospectionResponse status = this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(), rpt, "");
        assertIt(status);

        // at the end scope registered by permission must be present in RPT permission with scope allowed by spontaneous scope check
        assertTrue(status.getPermissions().get(0).getScopes().contains(USER_2_SCOPE));
    }
}
