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
import io.jans.as.client.ssa.jwtssa.SsaGetJwtClient;
import io.jans.as.client.ssa.jwtssa.SsaGetJwtResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import org.apache.http.HttpStatus;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

public class SsaGetJwtTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getJwtSsaWithJtiValidResponseOkWithJwtSsa(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getJwtSsaWithJtiValidResponseOkWithJwtSsa");
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

        // Get Jwt of SSA
        SsaGetJwtClient ssaGetJwtClient = new SsaGetJwtClient(ssaEndpoint);
        SsaGetJwtResponse ssaGetJwtResponse = ssaGetJwtClient.execGetJwtSsa(accessToken, jti);
        showClient(ssaGetJwtClient);
        AssertBuilder.ssaGetJwt(ssaGetJwtResponse).status(HttpStatus.SC_OK).check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getJwtSsaWithJtiWrongResponse422(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getJwtSsaWithJtiWrongResponse422");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        String jti = "wrong-jti";

        // Get Jwt of SSA
        SsaGetJwtClient ssaGetJwtClient = new SsaGetJwtClient(ssaEndpoint);
        SsaGetJwtResponse ssaGetJwtResponse = ssaGetJwtClient.execGetJwtSsa(accessToken, jti);
        showClient(ssaGetJwtClient);
        AssertBuilder.ssaGetJwt(ssaGetJwtResponse).status(422).check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void getJwtSsaWithSsaPortalScopeResponseUnauthorized(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("getJwtSsaWithSsaPortalScopeResponseUnauthorized");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client with scope admin
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

        // Create a new client with scope portal
        scopes = Collections.singletonList(SsaScopeType.SSA_PORTAL.getValue());
        registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        clientId = registerResponse.getClientId();
        clientSecret = registerResponse.getClientSecret();

        // Access token
        tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        accessToken = tokenResponse.getAccessToken();

        // Get JWT of SSA
        SsaGetJwtClient ssaGetJwtClient = new SsaGetJwtClient(ssaEndpoint);
        SsaGetJwtResponse ssaGetJwtResponse = ssaGetJwtClient.execGetJwtSsa(accessToken, jti);
        showClient(ssaGetJwtClient);
        AssertBuilder.ssaGetJwt(ssaGetJwtResponse).status(401).check();
    }
}