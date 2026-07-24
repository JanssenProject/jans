/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.persistence.model.ClientAttributes;
import io.jans.as.server.service.ClientIdMetadataService;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.SpiffeBundleService;
import io.jans.service.cdi.util.CdiUtil;
import org.json.JSONObject;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SpiffeJwtSvidAssertionTest {

    private static final String ISSUER = "https://server.example.com";
    private static final String SPIFFE_ID = "spiffe://example.org/my-workload";
    private static final String CLIENT_ID = "client-inum-1";
    private static final String KEY_ID = "kid1";

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_blankAssertion_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, "").initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_blankClientId_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, "", jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_missingSubClaim_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid(null, ISSUER, fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_missingAudClaim_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, null, fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_missingExpClaim_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, null);

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_expiredJwt_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesAgo());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_invalidSpiffeIdSyntaxInSub_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid("not-a-spiffe-id", ISSUER, fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_wildcardSubClaim_shouldThrow() throws Exception {
        // a presented (concrete) SPIFFE ID must never itself carry the "/*" wildcard suffix
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        String jwtSvid = buildJwtSvid("spiffe://example.org/*", ISSUER, fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_audienceHasMultipleValues_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, Arrays.asList(ISSUER, "https://someone-else.example.com"), fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_audienceNotServerIssuer_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, "https://not-the-issuer.example.com", fiveMinutesFromNow());

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_clientNotFound_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(null);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);

            new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
        }
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_clientHasNoRegisteredSpiffeId_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(clientWithSpiffeId(null));

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);

            new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
        }
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_subDoesNotMatchRegisteredSpiffeId_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(clientWithSpiffeId("spiffe://example.org/some-other-workload"));

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);

            new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
        }
    }

    @Test
    public void initAndVerify_noJwtSvidBundleForTrustDomain_shouldThrowWithoutAttemptingSignatureVerification() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        SpiffeBundleService spiffeBundleService = mock(SpiffeBundleService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(clientWithSpiffeId(SPIFFE_ID));
        when(spiffeBundleService.getJwtSvidJwks("example.org")).thenReturn(null);

        SpiffeJwtSvidAssertion assertion = new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid);
        boolean threw = false;
        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            cdiUtil.when(() -> CdiUtil.bean(SpiffeBundleService.class)).thenReturn(spiffeBundleService);

            assertion.initAndVerify();
        } catch (InvalidJwtException expected) {
            threw = true;
        }

        assertTrue(threw, "Expected InvalidJwtException to be thrown");
        verify(cryptoProvider, never()).verifySignature(any(), any(), any(), any(), any(), any());
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_invalidSignature_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        JSONObject jwks = new JSONObject().put("keys", new org.json.JSONArray());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        SpiffeBundleService spiffeBundleService = mock(SpiffeBundleService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(clientWithSpiffeId(SPIFFE_ID));
        when(spiffeBundleService.getJwtSvidJwks("example.org")).thenReturn(jwks);
        when(cryptoProvider.verifySignature(any(), any(), eq(KEY_ID), eq(jwks), eq(null), eq(SignatureAlgorithm.RS256))).thenReturn(false);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            cdiUtil.when(() -> CdiUtil.bean(SpiffeBundleService.class)).thenReturn(spiffeBundleService);

            new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid).initAndVerify();
        }
    }

    @Test
    public void initAndVerify_validAssertion_shouldSucceedAndExposeSubjectAndClient() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        JSONObject jwks = new JSONObject().put("keys", new org.json.JSONArray());
        Client client = clientWithSpiffeId(SPIFFE_ID);

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        SpiffeBundleService spiffeBundleService = mock(SpiffeBundleService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(client);
        when(spiffeBundleService.getJwtSvidJwks("example.org")).thenReturn(jwks);
        when(cryptoProvider.verifySignature(any(), any(), eq(KEY_ID), eq(jwks), eq(null), eq(SignatureAlgorithm.RS256))).thenReturn(true);

        SpiffeJwtSvidAssertion assertion = new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            cdiUtil.when(() -> CdiUtil.bean(SpiffeBundleService.class)).thenReturn(spiffeBundleService);

            assertion.initAndVerify();
        }

        assertEquals(assertion.getSubjectIdentifier(), SPIFFE_ID);
        assertSame(assertion.getClient(), client);
    }

    @Test
    public void initAndVerify_wildcardRegisteredSpiffeId_shouldMatchConcretePresentedId() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String presentedSpiffeId = "spiffe://example.org/fleet/instance-123";
        String jwtSvid = buildJwtSvid(presentedSpiffeId, ISSUER, fiveMinutesFromNow());

        JSONObject jwks = new JSONObject().put("keys", new org.json.JSONArray());
        Client client = clientWithSpiffeId("spiffe://example.org/fleet/*");

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        SpiffeBundleService spiffeBundleService = mock(SpiffeBundleService.class);
        when(clientIdMetadataService.isCimdClientId(CLIENT_ID)).thenReturn(false);
        when(clientService.getClient(CLIENT_ID)).thenReturn(client);
        when(spiffeBundleService.getJwtSvidJwks("example.org")).thenReturn(jwks);
        when(cryptoProvider.verifySignature(any(), any(), eq(KEY_ID), eq(jwks), eq(null), eq(SignatureAlgorithm.RS256))).thenReturn(true);

        SpiffeJwtSvidAssertion assertion = new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, jwtSvid);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            cdiUtil.when(() -> CdiUtil.bean(SpiffeBundleService.class)).thenReturn(spiffeBundleService);

            assertion.initAndVerify();
        }

        assertEquals(assertion.getSubjectIdentifier(), presentedSpiffeId);
    }

    @Test
    public void initAndVerify_cimdClientId_resolvesViaClientIdMetadataServiceNotClientService() throws Exception {
        String cimdClientId = "https://example.com/client-metadata";
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        JSONObject jwks = new JSONObject().put("keys", new org.json.JSONArray());
        Client cimdClient = clientWithSpiffeId(SPIFFE_ID);

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        SpiffeBundleService spiffeBundleService = mock(SpiffeBundleService.class);
        when(clientIdMetadataService.isCimdClientId(cimdClientId)).thenReturn(true);
        when(clientIdMetadataService.getClient(cimdClientId)).thenReturn(cimdClient);
        when(spiffeBundleService.getJwtSvidJwks("example.org")).thenReturn(jwks);
        when(cryptoProvider.verifySignature(any(), any(), eq(KEY_ID), eq(jwks), eq(null), eq(SignatureAlgorithm.RS256))).thenReturn(true);

        SpiffeJwtSvidAssertion assertion = new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, cimdClientId, jwtSvid);

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);
            cdiUtil.when(() -> CdiUtil.bean(SpiffeBundleService.class)).thenReturn(spiffeBundleService);

            assertion.initAndVerify();
        }

        assertSame(assertion.getClient(), cimdClient);
        verify(clientService, never()).getClient(any());
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void initAndVerify_cimdResolutionThrows_shouldTreatAsClientNotFound() throws Exception {
        String cimdClientId = "https://example.com/client-metadata";
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        when(appConfiguration.getIssuer()).thenReturn(ISSUER);
        String jwtSvid = buildJwtSvid(SPIFFE_ID, ISSUER, fiveMinutesFromNow());

        ClientIdMetadataService clientIdMetadataService = mock(ClientIdMetadataService.class);
        ClientService clientService = mock(ClientService.class);
        when(clientIdMetadataService.isCimdClientId(cimdClientId)).thenReturn(true);
        when(clientIdMetadataService.getClient(cimdClientId)).thenThrow(new RuntimeException("fetch failed"));

        try (MockedStatic<CdiUtil> cdiUtil = mockStatic(CdiUtil.class)) {
            cdiUtil.when(() -> CdiUtil.bean(ClientIdMetadataService.class)).thenReturn(clientIdMetadataService);
            cdiUtil.when(() -> CdiUtil.bean(ClientService.class)).thenReturn(clientService);

            new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, cimdClientId, jwtSvid).initAndVerify();
        }
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void getSubjectIdentifier_beforeVerify_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, "irrelevant").getSubjectIdentifier();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void getClient_beforeVerify_shouldThrow() throws Exception {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);

        new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, "irrelevant").getClient();
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void getSubjectIdentifier_afterFailedVerify_shouldThrow() throws InvalidJwtException {
        AppConfiguration appConfiguration = mock(AppConfiguration.class);
        AbstractCryptoProvider cryptoProvider = mock(AbstractCryptoProvider.class);
        SpiffeJwtSvidAssertion assertion = new SpiffeJwtSvidAssertion(appConfiguration, cryptoProvider, CLIENT_ID, "");

        try {
            assertion.initAndVerify();
        } catch (InvalidJwtException ignored) {
            // expected - assertion is left unverified because the encoded assertion is blank
        }

        // verified is still false, so this must throw regardless of the earlier failure's cause
        assertion.getSubjectIdentifier();
    }

    private static String buildJwtSvid(String subject, Object audience, Date expirationTime) {
        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(SignatureAlgorithm.RS256);
        jwt.getHeader().setKeyId(KEY_ID);

        if (subject != null) {
            jwt.getClaims().setSubjectIdentifier(subject);
        }
        if (audience instanceof String) {
            jwt.getClaims().setClaim(JwtClaimName.AUDIENCE, (String) audience);
        } else if (audience instanceof java.util.List) {
            jwt.getClaims().setClaim(JwtClaimName.AUDIENCE, (java.util.List<?>) audience);
        }
        if (expirationTime != null) {
            jwt.getClaims().setExpirationTime(expirationTime);
        }

        jwt.setEncodedSignature("fake-signature");
        return jwt.toString();
    }

    private static Date fiveMinutesFromNow() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, 5);
        return calendar.getTime();
    }

    private static Date fiveMinutesAgo() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, -5);
        return calendar.getTime();
    }

    private static Client clientWithSpiffeId(String spiffeId) {
        Client client = new Client();
        client.setClientId(CLIENT_ID);
        ClientAttributes attributes = new ClientAttributes();
        attributes.setSpiffeId(spiffeId);
        client.setAttributes(attributes);
        return client;
    }
}
