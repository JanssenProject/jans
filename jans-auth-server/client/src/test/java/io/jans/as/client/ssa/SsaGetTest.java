/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.client.ssa.create.SsaCreateResponse;
import io.jans.as.client.ssa.get.SsaGetClient;
import io.jans.as.client.ssa.get.SsaGetResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SsaGetTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getSsaSearchByOrgId(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getSsaSearchByOrgId");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Create ssa
        String orgId1 = "org-id-test-1";
        String orgId2 = "org-id-test-2";
        List<String> ssaCreateOrgId = Arrays.asList(orgId1, orgId1, orgId2);
        List<String> jtiList = createSsaList(accessToken, ssaCreateOrgId);

        // Ssa get
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, null, orgId1);
        AssertBuilder.ssaGet(ssaGetResponse)
                .ssaListSize(2)
                .jtiList(jtiList)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getSsaSearchByJti(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getSsaSearchByJti");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Create ssa
        String orgId1 = "org-id-test-1";
        List<String> ssaCreateOrgId = Arrays.asList(orgId1, orgId1);
        List<String> jtiList = createSsaList(accessToken, ssaCreateOrgId);
        String jti = jtiList.get(0);

        // Ssa get
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, jti, null);
        AssertBuilder.ssaGet(ssaGetResponse)
                .ssaListSize(1)
                .jtiList(jtiList)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getSsaSearchByOrgIdAndJti(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getSsaSearchByOrgIdAndJti");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Create ssa
        String orgId1 = "org-id-test-1";
        String orgId2 = "org-id-test-2";
        List<String> ssaCreateOrgId = Arrays.asList(orgId1, orgId1, orgId2);
        List<String> jtiList = createSsaList(accessToken, ssaCreateOrgId);
        String jti = jtiList.get(0);

        // Ssa get
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, jti, orgId1);
        AssertBuilder.ssaGet(ssaGetResponse)
                .ssaListSize(1)
                .jtiList(jtiList)
                .check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getSsaSearchByJtiNotExits(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getSsaSearchByJtiNotExits");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Create ssa
        String orgId1 = "org-id-test-1";
        List<String> ssaCreateOrgId = Arrays.asList(orgId1, orgId1);
        List<String> jtiList = createSsaList(accessToken, ssaCreateOrgId);
        String jti = "jti-not-found";

        // Ssa get
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, jti, null);
        AssertBuilder.ssaGet(ssaGetResponse)
                .ssaListSize(0)
                .jtiList(jtiList)
                .check();
    }

    private List<String> createSsaList(String accessToken, List<String> ssaCreateRequestList) {
        List<String> jtiList = new ArrayList<>();
        for (int i = 0; i < ssaCreateRequestList.size(); i++) {
            String orgId = ssaCreateRequestList.get(i);
            SsaCreateResponse ssaCreateResponse = createSsaWithDefaultValues(accessToken, orgId, null, Boolean.TRUE);
            Assert.assertNotNull(ssaCreateResponse, "Ssa create response is null, index: " + i);
            jtiList.add(ssaCreateResponse.getJti());
        }
        return jtiList;
    }
}