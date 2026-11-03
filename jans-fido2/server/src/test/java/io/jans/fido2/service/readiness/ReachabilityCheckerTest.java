/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jans.fido2.service.readiness.WellKnownFetchResult.Failure;

@ExtendWith(MockitoExtension.class)
class ReachabilityCheckerTest {

    private static final ValidatedRpId RP = new ValidatedRpId("example.com");
    private static final String MARKER = "SECRET-MARKER-9f3a";

    @Mock
    private WellKnownFetcher fetcher;

    @InjectMocks
    private ReachabilityChecker checker;

    private static WellKnownFetchResult ok(int status, String contentType, String location, boolean truncated) {
        return WellKnownFetchResult.response(status, contentType, location,
                MARKER.getBytes(StandardCharsets.UTF_8), truncated);
    }

    private ReachabilityReport assetLinks(WellKnownFetchResult result) {
        when(fetcher.fetchAssetLinks(RP)).thenReturn(result);
        return checker.checkAssetLinks(RP);
    }

    private ReachabilityReport aasa(WellKnownFetchResult result) {
        when(fetcher.fetchAppleAppSiteAssociation(RP)).thenReturn(result);
        return checker.checkAppleAppSiteAssociation(RP);
    }

    private static CheckResult check(ReachabilityReport report, String id) {
        Optional<CheckResult> found = report.checks().stream().filter(c -> c.id().equals(id)).findFirst();
        assertTrue(found.isPresent(), "missing check " + id + " in " + report.checks());
        return found.get();
    }

    private static boolean has(ReachabilityReport report, String id) {
        return report.checks().stream().anyMatch(c -> c.id().equals(id));
    }

    // ---- HTTP 200 and content type

    @Test
    void ok200WithJson_passesEverything() {
        ReachabilityReport report = assetLinks(ok(200, "application/json", null, false));
        assertEquals(CheckStatus.PASS, check(report, "reachability.status").status());
        assertEquals(CheckStatus.PASS, check(report, "reachability.host").status());
        assertEquals(CheckStatus.PASS, check(report, "reachability.content-type").status());
        assertEquals(CheckStatus.PASS, check(report, "reachability.limits").status());
        assertFalse(has(report, "reachability.redirect"));
    }

    @Test
    void hostCheckNamesTheRpIdHost() {
        ReachabilityReport report = assetLinks(ok(200, "application/json", null, false));
        assertTrue(check(report, "reachability.host").reason().contains("example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/json; charset=utf-8", "Application/JSON", "  application/json ;charset=UTF-8" })
    void contentType_comparesTheMediaTypeOnly(String header) {
        assertEquals(CheckStatus.PASS,
                check(assetLinks(ok(200, header, null, false)), "reachability.content-type").status());
    }

    @Test
    void wrongContentType_failsForAssetLinks_warnsForAasa() {
        assertEquals(CheckStatus.FAIL,
                check(assetLinks(ok(200, "text/html", null, false)), "reachability.content-type").status());
        assertEquals(CheckStatus.WARN,
                check(aasa(ok(200, "text/html", null, false)), "reachability.content-type").status());
    }

    @Test
    void missingContentType_failsForBothFiles() {
        assertEquals(CheckStatus.FAIL,
                check(assetLinks(ok(200, null, null, false)), "reachability.content-type").status());
        assertEquals(CheckStatus.FAIL,
                check(aasa(ok(200, null, null, false)), "reachability.content-type").status());
    }

    @Test
    void contentTypeIsNotCheckedWhenThereIsNoSuccessfulResponse() {
        assertFalse(has(assetLinks(ok(404, "text/html", null, false)), "reachability.content-type"));
    }

    // ---- redirects

    @ParameterizedTest
    @ValueSource(ints = { 301, 302, 307, 308 })
    void redirect_failsAndSaysItIsNotFollowed(int status) {
        ReachabilityReport report = assetLinks(ok(status, null, "https://other.example.net/x", false));
        CheckResult result = check(report, "reachability.status");
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.reason().contains("not followed"), result.reason());
        assertEquals(CheckStatus.FAIL, check(report, "reachability.redirect").status());
        assertFalse(has(report, "reachability.host"));
        assertFalse(has(report, "reachability.content-type"));
    }

    @Test
    void redirectReasons_distinguishWwwSameHostAndOtherHost() {
        String www = check(assetLinks(ok(301, null, "https://www.example.com/.well-known/assetlinks.json", false)),
                "reachability.redirect").reason();
        String other = check(aasa(ok(301, null, "https://evil.test/a", false)), "reachability.redirect").reason();
        String same = check(assetLinks(ok(301, null, "/elsewhere", false)), "reachability.redirect").reason();
        String unknown = check(aasa(ok(301, null, null, false)), "reachability.redirect").reason();

        assertTrue(www.contains("www."), www);
        assertTrue(other.contains("different host"), other);
        assertTrue(same.contains("same host"), same);
        assertTrue(unknown.contains("could not be determined"), unknown);
        assertEquals(4, java.util.Set.of(www, other, same, unknown).size());
    }

    @Test
    void locationValueIsNeverEchoed() {
        String location = "https://attacker.test/" + MARKER + "?token=abc";
        ReachabilityReport report = assetLinks(ok(302, null, location, false));
        for (CheckResult result : report.checks()) {
            assertFalse(result.reason().contains("attacker.test"), result.toString());
            assertFalse(result.reason().contains(MARKER), result.toString());
            assertFalse(result.reason().contains("token"), result.toString());
        }
    }

    // ---- HTTP errors

    @ParameterizedTest
    @ValueSource(ints = { 403, 404, 500, 503, 204 })
    void nonSuccessStatus_failsAndStatesTheCode(int status) {
        CheckResult result = check(assetLinks(ok(status, "application/json", null, false)), "reachability.status");
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.reason().contains(String.valueOf(status)), result.reason());
    }

    // ---- size and timeout

    @Test
    void truncatedBody_failsWithSizeReason() {
        CheckResult result = check(assetLinks(ok(200, "application/json", null, true)), "reachability.limits");
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.reason().contains("larger than limit"), result.reason());
    }

    @Test
    void timeout_failsWithTimeReason() {
        ReachabilityReport report = assetLinks(WellKnownFetchResult.failed(Failure.TIMEOUT));
        CheckResult result = check(report, "reachability.limits");
        assertEquals(CheckStatus.FAIL, result.status());
        assertTrue(result.reason().contains("no response within limit"), result.reason());
        assertEquals(1, report.checks().size());
    }

    // ---- fetch failures

    @ParameterizedTest
    @ValueSource(strings = { "UNRESOLVABLE", "BLOCKED_ADDRESS", "TLS_FAILED", "CONNECTION_FAILED", "ERROR" })
    void fetchFailure_failsWithAFixedReason(String name) {
        ReachabilityReport report = aasa(WellKnownFetchResult.failed(Failure.valueOf(name)));
        assertEquals(1, report.checks().size());
        CheckResult result = check(report, "reachability.status");
        assertEquals(CheckStatus.FAIL, result.status());
        assertFalse(result.reason().isEmpty());
        assertFalse(result.reason().contains(name), "enum name leaked: " + result.reason());
    }

    @Test
    void fetchFailureReasonsAreDistinct() {
        long distinct = java.util.stream.Stream.of(Failure.UNRESOLVABLE, Failure.BLOCKED_ADDRESS,
                Failure.TLS_FAILED, Failure.CONNECTION_FAILED, Failure.ERROR).map(f -> {
                    when(fetcher.fetchAssetLinks(RP)).thenReturn(WellKnownFetchResult.failed(f));
                    return check(checker.checkAssetLinks(RP), "reachability.status").reason();
                }).distinct().count();
        assertEquals(5, distinct);
    }

    // ---- no leakage, one fetch

    @Test
    void noResultContainsBodyContent() {
        for (WellKnownFetchResult result : List.of(ok(200, "application/json", null, false),
                ok(200, "text/html", null, true), ok(404, null, null, false), ok(301, null, "/x", false))) {
            for (CheckResult check : assetLinks(result).checks()) {
                assertFalse(check.reason().contains(MARKER), check.toString());
            }
        }
    }

    @Test
    void reportCarriesTheFetchForTheContentChecks() {
        WellKnownFetchResult fetch = ok(200, "application/json", null, false);
        assertTrue(assetLinks(fetch).fetch() == fetch);
    }

    @Test
    void oneFetchPerPlatformPerRun() {
        when(fetcher.fetchAssetLinks(RP)).thenReturn(ok(200, "application/json", null, false));
        when(fetcher.fetchAppleAppSiteAssociation(RP)).thenReturn(ok(200, "application/json", null, false));

        checker.checkAssetLinks(RP);
        verify(fetcher).fetchAssetLinks(RP);
        verify(fetcher, never()).fetchAppleAppSiteAssociation(RP);

        checker.checkAppleAppSiteAssociation(RP);
        verify(fetcher).fetchAppleAppSiteAssociation(RP);
    }
}
