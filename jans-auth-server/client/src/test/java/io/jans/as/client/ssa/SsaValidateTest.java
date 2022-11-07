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
import io.jans.as.client.ssa.validate.SsaValidateClient;
import io.jans.as.client.ssa.validate.SsaValidateResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.DateUtil;
import org.apache.http.HttpStatus;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SsaValidateTest extends BaseTest {

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithOneTimeUseTrueResponseOK(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithOneTimeUseTrueResponseOK");
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

        // Ssa first validation
        SsaValidateClient ssaValidateClient = new SsaValidateClient(ssaEndpoint);
        SsaValidateResponse ssaGetResponseFirst = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        assertNotNull(ssaGetResponseFirst, "Response is null");
        assertEquals(ssaGetResponseFirst.getStatus(), HttpStatus.SC_OK);

        // Ssa second validation
        SsaValidateResponse ssaGetResponseSecond = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        assertNotNull(ssaGetResponseSecond, "Response is null");
        assertEquals(ssaGetResponseSecond.getStatus(), 422);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithOneTimeUseFalseResponseOK(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithOneTimeUseFalseResponseOK");
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
        SsaCreateResponse ssaCreateResponse = createSsaWithDefaultValues(accessToken, null, null, Boolean.FALSE);
        String jti = ssaCreateResponse.getJti();

        // Ssa first validation
        SsaValidateClient ssaValidateClient = new SsaValidateClient(ssaEndpoint);
        SsaValidateResponse ssaGetResponseFirst = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        assertNotNull(ssaGetResponseFirst, "Response is null");
        assertEquals(ssaGetResponseFirst.getStatus(), HttpStatus.SC_OK);

        // Ssa second validation
        SsaValidateResponse ssaGetResponseSecond = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        assertNotNull(ssaGetResponseSecond, "Response is null");
        assertEquals(ssaGetResponseSecond.getStatus(), HttpStatus.SC_OK);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithNotFoundSsaResponse422(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithNotFoundSsaResponse422");
        List<String> scopes = Collections.singletonList(SsaScopeType.SSA_ADMIN.getValue());

        // Register client
        RegisterResponse registerResponse = registerClient(redirectUris, Collections.singletonList(ResponseType.CODE),
                Collections.singletonList(GrantType.CLIENT_CREDENTIALS), scopes, sectorIdentifierUri);
        String clientId = registerResponse.getClientId();
        String clientSecret = registerResponse.getClientSecret();

        // Access token
        TokenResponse tokenResponse = tokenClientCredentialsGrant(SsaScopeType.SSA_ADMIN.getValue(), clientId, clientSecret);
        String accessToken = tokenResponse.getAccessToken();

        String jti = "WRONG-JTI";

        // Ssa validation
        SsaValidateClient ssaValidateClient = new SsaValidateClient(ssaEndpoint);
        SsaValidateResponse ssaGetResponse = ssaValidateClient.execSsaValidate(jti);
        assertNotNull(ssaGetResponse, "Response is null");
        assertEquals(ssaGetResponse.getStatus(), 422);
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithExpiredSsaResponse422(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithExpiredSsaResponse422");
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
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.HOUR, -24);
        SsaCreateResponse ssaCreateResponse = createSsaWithDefaultValues(accessToken, null, DateUtil.dateToUnixEpoch(calendar.getTime()), Boolean.FALSE);
        String jti = ssaCreateResponse.getJti();

        // Ssa validation
        SsaValidateClient ssaValidateClient = new SsaValidateClient(ssaEndpoint);
        SsaValidateResponse ssaGetResponse = ssaValidateClient.execSsaValidate(jti);
        assertNotNull(ssaGetResponse, "Response is null");
        assertEquals(ssaGetResponse.getStatus(), 422);
    }
}