/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.client.ws.rs.spiffe;

import io.jans.as.client.BaseTest;
import io.jans.as.client.ClientInfoClient;
import io.jans.as.client.ClientInfoResponse;
import io.jans.as.client.RegisterClient;
import io.jans.as.client.RegisterRequest;
import io.jans.as.client.RegisterResponse;
import io.jans.as.client.TestCryptoContext;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.client.AssertBuilder;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.register.ApplicationType;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.util.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Integration tests for SPIFFE JWT-SVID client authentication
 * (client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-spiffe), per
 * draft-ietf-oauth-spiffe-client-auth.
 * <p>
 * Unlike X.509-SVID (mTLS) client authentication, JWT-SVID is a plain token-endpoint parameter
 * and does not require a live TLS handshake or a reverse-proxy-forwarded certificate header, so it
 * is the practical credential type to exercise from this HTTP-only client-module test harness.
 * X.509-SVID is intentionally not covered here for the same reason mTLS X.509 flows aren't covered
 * elsewhere in this module (see {@code io.jans.as.client.dev.manual.MTSLClientAuthenticationMain}).
 * <p>
 * <b>Prerequisite:</b> the target server must have the {@code spiffe_client_auth} feature flag
 * enabled. {@link #spiffeJwtSvidAuthenticationMethodRS256} additionally requires a
 * {@code spiffeTrustDomains} entry configured for {@link #TRUST_DOMAIN} whose bundle contains a
 * {@code jwt-svid} key matching {@link TestCryptoContext}'s generated RS256 key - without that,
 * only the negative tests (which fail before signature verification is reached) can be expected
 * to pass.
 */
public class SpiffeJwtSvidTokenRequestHttpTest extends BaseTest {

    private static final String TRUST_DOMAIN = "jans.io";
    private static final String SPIFFE_ID = "spiffe://" + TRUST_DOMAIN + "/test-client";

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void spiffeJwtSvidAuthenticationMethodRS256(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("spiffeJwtSvidAuthenticationMethodRS256");

        List<GrantType> grantTypes = List.of(GrantType.CLIENT_CREDENTIALS);

        // 1. Register client with a spiffe_id
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(grantTypes);
        registerRequest.setSpiffeId(SPIFFE_ID);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Request Client Credentials Grant, authenticating with a SPIFFE JWT-SVID
        String jwtSvid = buildJwtSvid(SPIFFE_ID, issuer, fiveMinutesFromNow());

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.addCustomParameter("client_id", clientId);
        tokenRequest.addCustomParameter("client_assertion_type", ClientAssertionType.SPIFFE_JWT.toString());
        tokenRequest.addCustomParameter("client_assertion", jwtSvid);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        AssertBuilder.tokenResponse(tokenResponse).ok()
                .nullRefreshToken()
                .check();

        String accessToken = tokenResponse.getAccessToken();

        // 3. Request client info to confirm the resolved client is the one we registered
        ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
        ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(accessToken);

        showClient(clientInfoClient);
        assertEquals(clientInfoResponse.getStatus(), 200, "Unexpected response code: " + clientInfoResponse.getStatus());
        assertNotNull(clientInfoResponse.getClaim("inum"), "Unexpected result: inum not found");
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void spiffeJwtSvidAuthenticationMethodRS256Fail_mismatchedSpiffeId(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("spiffeJwtSvidAuthenticationMethodRS256Fail_mismatchedSpiffeId");

        // 1. Register client with a spiffe_id
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(List.of(GrantType.CLIENT_CREDENTIALS));
        registerRequest.setSpiffeId(SPIFFE_ID);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Present a JWT-SVID whose sub does not match the client's registered spiffe_id
        String mismatchedSpiffeId = "spiffe://" + TRUST_DOMAIN + "/some-other-workload";
        String jwtSvid = buildJwtSvid(mismatchedSpiffeId, issuer, fiveMinutesFromNow());

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.addCustomParameter("client_id", clientId);
        tokenRequest.addCustomParameter("client_assertion_type", ClientAssertionType.SPIFFE_JWT.toString());
        tokenRequest.addCustomParameter("client_assertion", jwtSvid);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void spiffeJwtSvidAuthenticationMethodRS256Fail_wrongAudience(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("spiffeJwtSvidAuthenticationMethodRS256Fail_wrongAudience");

        // 1. Register client with a spiffe_id
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(List.of(GrantType.CLIENT_CREDENTIALS));
        registerRequest.setSpiffeId(SPIFFE_ID);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        String clientId = registerResponse.getClientId();

        // 2. Present a JWT-SVID whose aud is not (solely) the server's issuer identifier
        String jwtSvid = buildJwtSvid(SPIFFE_ID, "https://not-the-issuer.example.org", fiveMinutesFromNow());

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.addCustomParameter("client_id", clientId);
        tokenRequest.addCustomParameter("client_assertion_type", ClientAssertionType.SPIFFE_JWT.toString());
        tokenRequest.addCustomParameter("client_assertion", jwtSvid);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertEquals(tokenResponse.getErrorType(), TokenErrorResponseType.INVALID_CLIENT);
        assertNotNull(tokenResponse.getErrorDescription());
    }

    @Parameters({"redirectUris", "sectorIdentifierUri"})
    @Test
    public void spiffeJwtSvidAuthenticationMethodFail_missingClientId(
            final String redirectUris, final String sectorIdentifierUri) throws Exception {
        showTitle("spiffeJwtSvidAuthenticationMethodFail_missingClientId");

        // 1. Register client with a spiffe_id
        RegisterRequest registerRequest = new RegisterRequest(ApplicationType.WEB, "jans test app",
                StringUtils.spaceSeparatedToList(redirectUris));
        registerRequest.setGrantTypes(List.of(GrantType.CLIENT_CREDENTIALS));
        registerRequest.setSpiffeId(SPIFFE_ID);
        registerRequest.setSectorIdentifierUri(sectorIdentifierUri);

        RegisterClient registerClient = new RegisterClient(registrationEndpoint);
        registerClient.setRequest(registerRequest);
        RegisterResponse registerResponse = registerClient.exec();

        showClient(registerClient);
        AssertBuilder.registerResponse(registerResponse).created().check();

        // 2. Present a valid JWT-SVID but omit the (required) client_id parameter
        String jwtSvid = buildJwtSvid(SPIFFE_ID, issuer, fiveMinutesFromNow());

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.addCustomParameter("client_assertion_type", ClientAssertionType.SPIFFE_JWT.toString());
        tokenRequest.addCustomParameter("client_assertion", jwtSvid);

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        TokenResponse tokenResponse = tokenClient.exec();

        showClient(tokenClient);
        assertEquals(tokenResponse.getStatus(), 401, "Unexpected response code: " + tokenResponse.getStatus());
        assertNotNull(tokenResponse.getErrorDescription());
    }

    private static String buildJwtSvid(String subjectSpiffeId, String audience, Date expirationTime) throws Exception {
        TestCryptoContext cryptoContext = TestCryptoContext.getInstance();
        AuthCryptoProvider cryptoProvider = cryptoContext.getCryptoProvider();
        String keyId = cryptoContext.getKeyId(Algorithm.RS256);

        Jwt jwtSvid = new Jwt();
        jwtSvid.getHeader().setType(JwtType.JWT);
        jwtSvid.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwtSvid.getHeader().setKeyId(keyId);

        jwtSvid.getClaims().setSubjectIdentifier(subjectSpiffeId);
        jwtSvid.getClaims().setAudience(audience);
        jwtSvid.getClaims().setJwtId(UUID.randomUUID());
        jwtSvid.getClaims().setExpirationTime(expirationTime);

        String signature = cryptoProvider.sign(jwtSvid.getSigningInput(), keyId, null, SignatureAlgorithm.RS256);
        jwtSvid.setEncodedSignature(signature);

        return jwtSvid.toString();
    }

    private static Date fiveMinutesFromNow() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, 5);
        return calendar.getTime();
    }
}
