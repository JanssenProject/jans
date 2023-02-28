/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ws.rs.uma;

import io.jans.as.client.BaseTest;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaRptIntrospectionService;
import io.jans.as.client.uma.UmaTokenService;
import io.jans.as.client.uma.wrapper.UmaClient;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaNeedInfoResponse;
import io.jans.as.model.uma.UmaTokenResponse;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.as.model.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

import static io.jans.as.test.UmaTestUtil.assertIt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test flow for the accessing protected resource (HTTP)
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */
public class AccessProtectedResourceFlowHttpTest extends BaseTest {

    protected UmaMetadata metadata;

    protected RegisterResourceFlowHttpTest registerResourceTest;
    protected UmaRegisterPermissionFlowHttpTest permissionFlowTest;

    protected UmaRptIntrospectionService rptStatusService;
    protected UmaTokenService tokenService;

    protected Token pat;
    protected String rpt;
    protected UmaNeedInfoResponse needInfo;
    protected String claimsGatheringTicket;

    public static String encodeCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(Util.getBytes(username + ":" + password));
    }

    @BeforeClass
    @Parameters({"umaMetaDataUrl", "umaPatClientId", "umaPatClientSecret"})
    public void init(final String umaMetaDataUrl, final String umaPatClientId, final String umaPatClientSecret) throws Exception {
        this.metadata = UmaClientFactory.instance().createMetadataService(umaMetaDataUrl, clientEngine(true)).getMetadata();
        assertIt(this.metadata);

        pat = UmaClient.requestPat(tokenEndpoint, umaPatClientId, umaPatClientSecret, clientEngine(true));
        assertIt(pat);

        this.registerResourceTest = new RegisterResourceFlowHttpTest(this.metadata);
        this.registerResourceTest.pat = this.pat;

        this.permissionFlowTest = new UmaRegisterPermissionFlowHttpTest(this.metadata);
        this.permissionFlowTest.registerResourceTest = this.registerResourceTest;

        this.rptStatusService = UmaClientFactory.instance().createRptStatusService(metadata, clientEngine(true));
        this.tokenService = UmaClientFactory.instance().createTokenService(metadata, clientEngine(true));
    }

    /**
     * Register resource
     */
    @Test
    public void registerResource() throws Exception {
        showTitle("registerResource");
        this.registerResourceTest.addResource();
    }

    /**
     * RS registers permissions for specific resource.
     */
    @Test(dependsOnMethods = {"registerResource"})
    public void rsRegisterPermissions() throws Exception {
        showTitle("rsRegisterPermissions");
        permissionFlowTest.testRegisterPermission();
    }

    /**
     * RP requests RPT with ticket and gets needs_info error (not all claims are provided, so redirect to claims-gathering endpoint)
     */
    @Test(dependsOnMethods = {"rsRegisterPermissions"})
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void requestRptAndGetNeedsInfo(String umaPatClientId, String umaPatClientSecret) throws Exception {
        showTitle("requestRptAndGetNeedsInfo");

        try {
            tokenService.requestRpt(
                    "Basic " + encodeCredentials(umaPatClientId, umaPatClientSecret),
                    GrantType.OXAUTH_UMA_TICKET.getValue(),
                    permissionFlowTest.ticket,
                    null, null, null, null, null);
        } catch (ClientErrorException ex) {
            // expected need_info error :
            // sample:  {"error":"need_info","ticket":"c024311b-f451-41db-95aa-cd405f16eed4","required_claims":[{"issuer":["https://localhost:8443"],"name":"country","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"country"},{"issuer":["https://localhost:8443"],"name":"city","claim_token_format":["http://openid.net/specs/openid-connect-core-1_0.html#IDToken"],"claim_type":"string","friendly_name":"city"}],"redirect_user":"https://localhost:8443/restv1/uma/gather_claimsgathering_id=sampleClaimsGathering&&?gathering_id=sampleClaimsGathering&&"}
            String entity = ex.getResponse().readEntity(String.class);
            System.out.println(entity);

            assertEquals(ex.getResponse().getStatus(), Response.Status.FORBIDDEN.getStatusCode(), "Unexpected response status");

            needInfo = Util.createJsonMapper().readValue(entity, UmaNeedInfoResponse.class);
            assertIt(needInfo);
            return;
        }

        throw new AssertionError("need_info error was not returned");
    }

    @Test(dependsOnMethods = {"requestRptAndGetNeedsInfo"})
    @Parameters({"umaPatClientId"})
    public void claimsGathering(String umaPatClientId) throws Exception {
        String gatheringUrl = needInfo.buildClaimsGatheringUrl(umaPatClientId, this.metadata.getClaimsInteractionEndpoint());

        System.out.println(gatheringUrl);
        System.out.println();
        try {
            startSelenium();
            navigateToAuhorizationUrl(driver, gatheringUrl);
            System.out.println(driver.getCurrentUrl());

            driver.findElement(By.id("loginForm:country")).sendKeys("US");
            driver.findElement(By.id("loginForm:gather")).click();

            Thread.sleep(1000);
            System.out.println(driver.getCurrentUrl());

            driver.findElement(By.id("loginForm:city")).sendKeys("NY");
            driver.findElement(By.id("loginForm:gather")).click();
            Thread.sleep(1200);
            // Finally after claims-redirect flow user gets redirect with new ticket
            // Sample: https://client.example.com/cb?ticket=e8e7bc0b-75de-4939-a9b1-2425dab3d5ec
            System.out.println(driver.getCurrentUrl());
            claimsGatheringTicket = StringUtils.substringAfter(driver.getCurrentUrl(), "ticket=");
        } finally {
            stopSelenium();
        }
    }

    /**
     * Request RPT with all claims provided
     */
    @Test(dependsOnMethods = {"claimsGathering"})
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void successfulRptRequest(String umaPatClientId, String umaPatClientSecret) throws Exception {
        showTitle("successfulRptRequest");

        UmaTokenResponse response = tokenService.requestRpt(
                "Basic " + encodeCredentials(umaPatClientId, umaPatClientSecret),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                claimsGatheringTicket,
                null, null, null, null, null);
        assertIt(response);

        this.rpt = response.getAccessToken();
    }

    @Test(dependsOnMethods = {"successfulRptRequest"})
    @Parameters({"umaPatClientId", "umaPatClientSecret"})
    public void repeatRptRequest(String umaPatClientId, String umaPatClientSecret) throws Exception {
        showTitle("repeatRptRequest");
        rsRegisterPermissions();
        requestRptAndGetNeedsInfo(umaPatClientId, umaPatClientSecret);
        claimsGathering(umaPatClientId);

        showTitle("Request RPT with existing RPT (upgrade case) ... ");

        UmaTokenResponse response = tokenService.requestRpt(
                "Basic " + encodeCredentials(umaPatClientId, umaPatClientSecret),
                GrantType.OXAUTH_UMA_TICKET.getValue(),
                claimsGatheringTicket,
                null, null, null, this.rpt, "oxd");
        assertIt(response);
        assertTrue(response.getUpgraded());

        this.rpt = response.getAccessToken();
    }

    /**
     * RPT status request
     */
    @Test(dependsOnMethods = {"repeatRptRequest"})
    @Parameters()
    public void rptStatus() {
        showTitle("rptStatus");
        assertIt(this.rptStatusService.requestRptStatus("Bearer " + pat.getAccessToken(), rpt, ""));
    }
}