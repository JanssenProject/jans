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
import io.jans.as.client.ssa.validate.SsaValidateClient;
import io.jans.as.client.ssa.validate.SsaValidateResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.DateUtil;
import org.apache.http.HttpStatus;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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
        SsaValidateResponse ssaValidateResponseFirst = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        AssertBuilder.ssaValidate(ssaValidateResponseFirst).status(HttpStatus.SC_OK).check();

        // Ssa second validation
        SsaValidateResponse ssaGetResponseSecond = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        AssertBuilder.ssaValidate(ssaGetResponseSecond).status(HttpStatus.SC_BAD_REQUEST)
                .errorType(SsaErrorResponseType.INVALID_JTI).check();
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
        SsaValidateResponse ssaValidateResponseFirst = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        AssertBuilder.ssaValidate(ssaValidateResponseFirst).status(HttpStatus.SC_OK).check();

        // Ssa second validation
        SsaValidateResponse ssaValidateResponseSecond = ssaValidateClient.execSsaValidate(jti);
        showClient(ssaValidateClient);
        AssertBuilder.ssaValidate(ssaValidateResponseSecond).status(HttpStatus.SC_OK).check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithNotFoundSsaResponse400(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithNotFoundSsaResponse400");
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
        SsaValidateResponse ssaValidateResponse = ssaValidateClient.execSsaValidate(jti);
        AssertBuilder.ssaValidate(ssaValidateResponse).status(HttpStatus.SC_BAD_REQUEST)
                .errorType(SsaErrorResponseType.INVALID_JTI).check();
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void validateWithExpiredSsaResponse400(final String redirectUris, final String sectorIdentifierUri) {
        showTitle("validateWithExpiredSsaResponse400");
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
        SsaValidateResponse ssaValidateResponse = ssaValidateClient.execSsaValidate(jti);
        AssertBuilder.ssaValidate(ssaValidateResponse).status(HttpStatus.SC_BAD_REQUEST)
                .errorType(SsaErrorResponseType.INVALID_JTI).check();
    }
}
