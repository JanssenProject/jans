/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa;

import io.jans.as.client.BaseTest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.ssa.create.SsaCreateResponse;
import io.jans.as.client.ssa.get.SsaGetClient;
import io.jans.as.client.ssa.get.SsaGetResponse;
import io.jans.as.client.ssa.revoke.SsaRevokeClient;
import io.jans.as.client.ssa.revoke.SsaRevokeResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import org.apache.http.HttpStatus;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

public class SsaRevokeTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void revokeWithJtiResponseOK(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("revokeWithJtiResponseOK");
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
        SsaCreateResponse ssaCreateResponse = createSsaWithDefaultValues(accessToken, null, null, Boolean.TRUE);
        String jti = ssaCreateResponse.getJti();

        // Ssa first revocation
        String orgId = null;
        SsaRevokeClient ssaRevokeClient = new SsaRevokeClient(ssaEndpoint);
        SsaRevokeResponse firstSsaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(firstSsaRevokeResponse, "Response is null");
        assertEquals(firstSsaRevokeResponse.getStatus(), HttpStatus.SC_OK);

        // Ssa second revocation
        SsaRevokeResponse secondSsaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(secondSsaRevokeResponse, "Response is null");
        assertEquals(secondSsaRevokeResponse.getStatus(), 422);

        // Ssa get
        SsaGetClient ssaGetClient = new SsaGetClient(ssaEndpoint);
        SsaGetResponse ssaGetResponse = ssaGetClient.execSsaGet(accessToken, jti, orgId);
        showClient(ssaGetClient);
        assertNotNull(ssaGetResponse, "Ssa get response is null");
        assertTrue(ssaGetResponse.getSsaList().isEmpty());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void revokeWithOrgIdResponseOK(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("revokeWithOrgIdResponseOK");
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
        String orgId = "org-id-test-1";
        createSsaWithDefaultValues(accessToken, orgId, null, Boolean.TRUE);

        // Ssa first revocation
        String jti = null;
        SsaRevokeClient ssaRevokeClient = new SsaRevokeClient(ssaEndpoint);
        SsaRevokeResponse firstSsaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(firstSsaRevokeResponse, "Response is null");
        assertEquals(firstSsaRevokeResponse.getStatus(), HttpStatus.SC_OK);

        // Ssa second revocation
        SsaRevokeResponse secondSsaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(secondSsaRevokeResponse, "Response is null");
        assertEquals(secondSsaRevokeResponse.getStatus(), 422);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void revokeWithSsaListNotFoundResponse422(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("revokeWithSsaListNotFoundResponse422");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa revocation
        String jti = "WRONG-JTI";
        String orgId = null;
        SsaRevokeClient ssaRevokeClient = new SsaRevokeClient(ssaEndpoint);
        SsaRevokeResponse ssaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(ssaRevokeResponse, "Response is null");
        assertEquals(ssaRevokeResponse.getStatus(), 422);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void revokeWithQueryParamNullResponse406(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("revokeWithQueryParamNullResponse406");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        // Ssa revocation
        String jti = null;
        String orgId = null;
        SsaRevokeClient ssaRevokeClient = new SsaRevokeClient(ssaEndpoint);
        SsaRevokeResponse ssaRevokeResponse = ssaRevokeClient.execSsaRevoke(accessToken, jti, orgId);
        showClient(ssaRevokeClient);
        assertNotNull(ssaRevokeResponse, "Response is null");
        assertEquals(ssaRevokeResponse.getStatus(), 406);
    }
}