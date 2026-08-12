/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.metric.Fido2CeremonyOutcomeReport;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CeremonyOutcomeServiceTest {

    @InjectMocks
    private CeremonyOutcomeService ceremonyOutcomeService;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private AuthenticationPersistenceService authenticationPersistenceService;

    private Fido2Configuration fido2Configuration;

    @BeforeEach
    void setUp() {
        fido2Configuration = new Fido2Configuration();
        fido2Configuration.setDeliberateCancellationThresholdMs(2000);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
    }

    /**
     * A rejection that arrives almost immediately is a user who dismissed the prompt without trying.
     */
    @Test
    void report_ifRejectedImmediately_isClassifiedAsDeliberateCancellation() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 900));

        assertEquals("NotAllowedError", ceremony.getErrorReason());
        assertEquals(Fido2MetricsConstants.CLIENT_CANCELLED, ceremony.getErrorCategory());
    }

    /**
     * A rejection that arrives many seconds later means the user was attempting verification in the
     * meantime — which is the case the server can otherwise never see.
     */
    @Test
    void report_ifRejectedAfterALongStruggle_isClassifiedAsVerificationAbandonment() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 24_000));

        assertEquals(Fido2MetricsConstants.CLIENT_VERIFICATION_ABANDONED, ceremony.getErrorCategory());
    }

    /**
     * The boundary is configurable because how long a genuine attempt takes varies by authenticator.
     */
    @Test
    void report_classificationFollowsTheConfiguredThreshold() {
        fido2Configuration.setDeliberateCancellationThresholdMs(10_000);
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 5_000));

        // Under the raised threshold this is now read as a cancellation, not a struggle.
        assertEquals(Fido2MetricsConstants.CLIENT_CANCELLED, ceremony.getErrorCategory());
    }

    /**
     * A missing or nonsensical elapsed time must not be silently bucketed as a cancellation, which is
     * what a zero would otherwise look like.
     */
    @Test
    void report_ifElapsedTimeIsUnusable_isNotClassifiedEitherWay() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 0));

        assertEquals(Fido2MetricsConstants.CLIENT_ABANDONED_UNCLASSIFIED, ceremony.getErrorCategory());
    }

    /**
     * The report normally arrives while the ceremony is still pending — the user gives up long before
     * the sweep relabels it — so pending has to be annotatable or the feature records nothing at all.
     */
    @Test
    void report_annotatesAPendingCeremony() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        Fido2AuthenticationEntry entry = entryFor(ceremony);
        when(authenticationPersistenceService.findByChallenge("challenge")).thenReturn(List.of(entry));

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 24_000));

        verify(authenticationPersistenceService).update(entry);
        // The status is the server's to decide; a client report never moves it.
        assertEquals(Fido2AuthenticationStatus.pending, ceremony.getStatus());
    }

    /**
     * A ceremony the server already decided cannot be recharacterised by the client — an assertion was
     * seen and judged, and no browser report revises that.
     */
    @Test
    void report_ifCeremonyAlreadyResolvedByTheServer_isIgnored() {
        for (Fido2AuthenticationStatus resolved : List.of(Fido2AuthenticationStatus.authenticated,
                Fido2AuthenticationStatus.failed)) {
            Fido2AuthenticationData ceremony = pendingCeremony();
            ceremony.setStatus(resolved);
            stubCeremony(ceremony);

            ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 24_000));

            assertNull(ceremony.getErrorReason(), "must not annotate a ceremony resolved as " + resolved);
        }
        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * First report wins. Otherwise anyone able to replay a challenge could keep rewriting how a
     * ceremony was characterised.
     */
    @Test
    void report_ifAlreadyReported_doesNotOverwrite() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        ceremony.setErrorReason("NotAllowedError");
        ceremony.setErrorCategory(Fido2MetricsConstants.CLIENT_VERIFICATION_ABANDONED);
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "AbortError", 100));

        assertEquals("NotAllowedError", ceremony.getErrorReason());
        assertEquals(Fido2MetricsConstants.CLIENT_VERIFICATION_ABANDONED, ceremony.getErrorCategory());
        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * The error name is written by the browser and reaches persistence and logs, so anything that is
     * not a plain identifier is stripped rather than stored.
     */
    @Test
    void report_sanitizesTheClientSuppliedErrorName() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "<script>alert(1)</script>", 100));

        assertEquals("scriptalert1script", ceremony.getErrorReason());
    }

    @Test
    void report_boundsAnOversizedErrorName() {
        Fido2AuthenticationData ceremony = pendingCeremony();
        stubCeremony(ceremony);

        ceremonyOutcomeService.report(report("challenge", "E".repeat(500), 100));

        assertEquals(Fido2MetricsConstants.MAX_LENGTH_CLIENT_ERROR_NAME, ceremony.getErrorReason().length());
    }

    /**
     * An unmatched challenge must be indistinguishable from a matched one, so the endpoint cannot be
     * used to probe which ceremonies exist.
     */
    @Test
    void report_ifChallengeMatchesNothing_isSilent() {
        when(authenticationPersistenceService.findByChallenge(any())).thenReturn(List.of());

        ceremonyOutcomeService.report(report("unknown", "NotAllowedError", 100));

        verify(authenticationPersistenceService, never()).update(any());
    }

    @Test
    void report_ifDisabled_recordsNothing() {
        fido2Configuration.setAcceptCeremonyOutcomeReports(false);

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 100));

        verify(authenticationPersistenceService, never()).findByChallenge(any());
    }

    @Test
    void report_ifChallengeIsMissing_isIgnoredWithoutLookup() {
        ceremonyOutcomeService.report(report("  ", "NotAllowedError", 100));

        verify(authenticationPersistenceService, never()).findByChallenge(any());
    }

    /**
     * A report is telemetry about a ceremony that is already over; losing one must never propagate.
     */
    @Test
    void report_ifPersistenceFails_doesNotPropagate() {
        stubCeremony(pendingCeremony());
        doThrow(new IllegalStateException("persistence down")).when(authenticationPersistenceService).update(any());

        ceremonyOutcomeService.report(report("challenge", "NotAllowedError", 100));
    }

    private Fido2AuthenticationData pendingCeremony() {
        Fido2AuthenticationData ceremony = new Fido2AuthenticationData();
        ceremony.setChallenge("challenge");
        ceremony.setStatus(Fido2AuthenticationStatus.pending);
        return ceremony;
    }

    private Fido2AuthenticationEntry entryFor(Fido2AuthenticationData ceremony) {
        Fido2AuthenticationEntry entry = new Fido2AuthenticationEntry();
        entry.setDn("jansId=ceremony,ou=fido2_auth,ou=fido2,o=jans");
        entry.setAuthenticationData(ceremony);
        entry.setAuthenticationStatus(ceremony.getStatus());
        return entry;
    }

    private void stubCeremony(Fido2AuthenticationData ceremony) {
        when(authenticationPersistenceService.findByChallenge(any())).thenReturn(List.of(entryFor(ceremony)));
    }

    private Fido2CeremonyOutcomeReport report(String challenge, String errorName, long elapsedMs) {
        Fido2CeremonyOutcomeReport outcomeReport = new Fido2CeremonyOutcomeReport();
        outcomeReport.setChallenge(challenge);
        outcomeReport.setErrorName(errorName);
        outcomeReport.setElapsedMs(elapsedMs);
        return outcomeReport;
    }
}
