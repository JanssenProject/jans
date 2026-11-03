/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.fido2.model.conf.AndroidApp;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.IosApp;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.service.readiness.NativeReadinessService.NotFound;
import io.jans.fido2.service.readiness.NativeReadinessService.RateLimited;
import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NativeReadinessServiceTest {

    private static final String RP_ID = "example.com";
    private static final String MARKER = "SECRET-MARKER-9f3a";

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private ReachabilityChecker reachabilityChecker;
    @Mock
    private ContentChecker contentChecker;
    @Mock
    private ReadinessRateLimiter rateLimiter;

    @InjectMocks
    private NativeReadinessService service;

    private Fido2Configuration config;
    private long now = 1_000_000L;

    @BeforeEach
    void setUp() {
        config = new Fido2Configuration();
        config.setNativeReadinessEnabled(true);
        config.setNativeReadinessAllowExternalFetch(true);
        config.setRequestedParties(List.of(party(RP_ID)));
        when(appConfiguration.getFido2Configuration()).thenReturn(config);
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(0L);
        when(rateLimiter.nextAllowedAt(anyString())).thenReturn(now + 60_000);
        service.clock = () -> now;
    }

    private static RequestedParty party(String id) {
        AndroidApp android = new AndroidApp();
        android.setPackageName("com.example.app");
        android.setSha256CertFingerprints(List.of("AB:CD"));
        IosApp ios = new IosApp();
        ios.setTeamId("T9A667JL6T");
        ios.setBundleId("com.example.app");
        RequestedParty party = new RequestedParty();
        party.setId(id);
        party.setAndroidApps(List.of(android));
        party.setIosApps(List.of(ios));
        return party;
    }

    private static CheckResult pass(String id) {
        return new CheckResult(id, CheckStatus.PASS, "fixed reason");
    }

    private static ReachabilityReport report(WellKnownFetchResult fetch) {
        return new ReachabilityReport(fetch, List.of(pass("reachability.status")));
    }

    private void fetchSucceeds() {
        WellKnownFetchResult fetch = WellKnownFetchResult.response(200, "application/json", null,
                MARKER.getBytes(StandardCharsets.UTF_8), false);
        when(reachabilityChecker.checkAssetLinks(any(), any())).thenReturn(report(fetch));
        when(reachabilityChecker.checkAppleAppSiteAssociation(any(), any())).thenReturn(report(fetch));
        when(contentChecker.checkAssetLinks(any(), any())).thenReturn(List.of(pass("content.android.package")));
        when(contentChecker.checkAppleAppSiteAssociation(any(), any())).thenReturn(List.of(pass("content.ios.app")));
    }

    private List<List<Object>> auditLines() {
        List<List<Object>> lines = new ArrayList<>();
        for (Invocation invocation : mockingDetails(log).getInvocations()) {
            if (invocation.getMethod().getName().equals("info")) {
                lines.add(Arrays.asList(invocation.getArguments()));
            }
        }
        return lines;
    }

    // ---- gates

    @Test
    void disabled_returnsFixedAnswer_andDoesNoWork() {
        config.setNativeReadinessEnabled(false);

        NativeReadinessResult result = service.check(RP_ID, false, "admin");

        assertFalse(result.isEnabled());
        assertTrue(result.getPlatforms().isEmpty());
        verifyNoInteractions(reachabilityChecker, contentChecker, rateLimiter);
    }

    @Test
    void externalFetchOff_runsOnlyNetworkFreeChecks() {
        config.setNativeReadinessAllowExternalFetch(false);

        NativeReadinessResult result = service.check(RP_ID, true, "admin");

        assertTrue(result.isEnabled());
        assertFalse(result.isExternalFetch());
        assertTrue(result.getPlatforms().get("android").stream().anyMatch(c -> c.getCheck().equals("fetch.skipped")));
        verifyNoInteractions(reachabilityChecker, contentChecker, rateLimiter);
    }

    @Test
    void unknownRp_isNotFound_andFetchesNothing() {
        assertThrows(NotFound.class, () -> service.check("other.example.org", false, "admin"));
        assertThrows(NotFound.class, () -> service.check(null, false, "admin"));

        verifyNoInteractions(reachabilityChecker, contentChecker, rateLimiter);
    }

    @Test
    void storedIdThatIsNotAHostName_isAFailResult_notAFetch() {
        config.setRequestedParties(List.of(party("https://jans.io")));

        NativeReadinessResult result = service.check("https://jans.io", false, "admin");

        assertEquals("fail", result.getPlatforms().get("android").get(0).getStatus());
        assertEquals("config.rp-id", result.getPlatforms().get("android").get(0).getCheck());
        verifyNoInteractions(reachabilityChecker, rateLimiter);
    }

    @Test
    void platformWithoutApps_isNotFetched() {
        RequestedParty onlyIos = party(RP_ID);
        onlyIos.setAndroidApps(null);
        config.setRequestedParties(List.of(onlyIos));
        fetchSucceeds();

        service.check(RP_ID, false, "admin");

        verify(reachabilityChecker, never()).checkAssetLinks(any(), any());
        verify(reachabilityChecker).checkAppleAppSiteAssociation(any(), any());
        verify(rateLimiter).tryAcquire(RP_ID, 1);
    }

    // ---- cache

    @Test
    void secondCall_isServedFromCache_andRecheckBypassesIt() {
        fetchSucceeds();

        NativeReadinessResult first = service.check(RP_ID, false, "admin");
        NativeReadinessResult second = service.check(RP_ID, false, "admin");
        NativeReadinessResult forced = service.check(RP_ID, true, "admin");

        assertFalse(first.isFromCache());
        assertTrue(second.isFromCache());
        assertEquals(first.getCheckedAt(), second.getCheckedAt());
        assertFalse(forced.isFromCache());
        verify(reachabilityChecker, times(2)).checkAssetLinks(any(), any());
    }

    @Test
    void expiredEntry_isFetchedAgain() {
        fetchSucceeds();
        config.setNativeReadinessCacheTtlSeconds(3600);

        service.check(RP_ID, false, "admin");
        now += 3600 * 1000L + 1;
        NativeReadinessResult again = service.check(RP_ID, false, "admin");

        assertFalse(again.isFromCache());
        verify(reachabilityChecker, times(2)).checkAssetLinks(any(), any());
    }

    // ---- rate limits

    @Test
    void refusedByRateLimiter_is429Material_andFetchesNothing() {
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(30_001L);

        RateLimited limited = assertThrows(RateLimited.class, () -> service.check(RP_ID, true, "admin"));

        assertEquals(31, limited.retryAfterSeconds());
        verifyNoInteractions(reachabilityChecker, contentChecker);
        verify(rateLimiter, never()).release();
    }

    @Test
    void permitIsReleased_evenWhenAChecksFails() {
        when(reachabilityChecker.checkAssetLinks(any(), any())).thenThrow(new IllegalStateException(MARKER));

        assertThrows(IllegalStateException.class, () -> service.check(RP_ID, true, "admin"));

        verify(rateLimiter).release();
    }

    // ---- hard bounds

    @Test
    void configuredLimits_areClamped() {
        int max = NativeReadinessService.MAX_RESPONSE_BYTES_LIMIT;
        int limit = NativeReadinessService.TIMEOUT_MS_LIMIT;

        assertEquals(max, NativeReadinessService.clamp(Integer.MAX_VALUE, 131072, max));
        assertEquals(max, NativeReadinessService.clamp(max + 1, 131072, max));
        assertEquals(limit, NativeReadinessService.clamp(600_000, 3000, limit));
        assertEquals(5000, NativeReadinessService.clamp(5000, 3000, limit));
        assertEquals(3000, NativeReadinessService.clamp(0, 3000, limit));
        assertEquals(3000, NativeReadinessService.clamp(-5, 3000, limit));
    }

    // ---- audit, and what never leaves

    @Test
    void auditHasOneEntryPerExecutedCheck_withAllSixFields() {
        fetchSucceeds();

        NativeReadinessResult result = service.check(RP_ID, false, "alice@example.org");

        int checks = result.getPlatforms().values().stream().mapToInt(List::size).sum();
        List<List<Object>> lines = auditLines();
        assertEquals(checks, lines.size());
        List<Object> first = lines.get(0);
        assertTrue(first.get(0).toString().contains("rpId="));
        assertTrue(first.get(0).toString().contains("resolvedIpClass="));
        assertEquals(RP_ID, first.get(1));
        assertEquals("reachability.status", first.get(2));
        assertEquals("alice@example.org", first.get(3));
        assertTrue(first.get(4).toString().startsWith("1970-01-01T00:16:40"));
        assertEquals("pass", first.get(5));
        assertEquals("public", first.get(6));
    }

    @Test
    void cacheHit_isOneAuditLine_andNoFetch() {
        fetchSucceeds();
        service.check(RP_ID, false, "admin");
        int before = auditLines().size();

        service.check(RP_ID, false, "admin");

        List<List<Object>> lines = auditLines();
        assertEquals(before + 1, lines.size());
        assertEquals("cache.hit", lines.get(lines.size() - 1).get(2));
        assertEquals("none", lines.get(lines.size() - 1).get(6));
        verify(reachabilityChecker, times(1)).checkAssetLinks(any(), any());
    }

    @Test
    void ipClassIsCoarse_andNeverAnAddress() {
        WellKnownFetchResult blocked = WellKnownFetchResult.failed(Failure.BLOCKED_ADDRESS);
        when(reachabilityChecker.checkAssetLinks(any(), any())).thenReturn(report(blocked));
        when(reachabilityChecker.checkAppleAppSiteAssociation(any(), any())).thenReturn(report(blocked));

        service.check(RP_ID, true, "admin");

        assertTrue(auditLines().stream().anyMatch(line -> "blocked".equals(line.get(6))));
    }

    @Test
    void missingOrHostileRequestedBy_isSanitized() {
        fetchSucceeds();

        service.check(RP_ID, true, "bob\nINFO forged line");

        assertTrue(auditLines().stream().noneMatch(line -> line.get(3).toString().contains("\n")));
        assertEquals("unknown", NativeReadinessService.sanitize(null));
        assertEquals("unknown", NativeReadinessService.sanitize(""));
        assertEquals(128, NativeReadinessService.sanitize("a".repeat(500)).length());
    }

    @Test
    void fetchedContent_neverReachesTheResponseOrTheAudit() throws Exception {
        fetchSucceeds();

        NativeReadinessResult result = service.check(RP_ID, false, "admin");

        assertFalse(new ObjectMapper().writeValueAsString(result).contains(MARKER));
        assertTrue(auditLines().stream().noneMatch(line -> line.toString().contains(MARKER)));
    }
}
