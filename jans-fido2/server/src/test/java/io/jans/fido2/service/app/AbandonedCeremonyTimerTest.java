/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.service.persist.AuthenticationPersistenceService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.orm.model.fido2.Fido2AuthenticationData;
import io.jans.orm.model.fido2.Fido2AuthenticationEntry;
import io.jans.orm.model.fido2.Fido2AuthenticationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbandonedCeremonyTimerTest {

    private static final String PEOPLE_DN = "ou=people,o=jans";
    private static final String ASSERTION_DN = "ou=fido2_auth,ou=fido2,o=jans";

    @InjectMocks
    private AbandonedCeremonyTimer timer;

    @Mock
    private Logger log;
    @Mock
    private AppConfiguration appConfiguration;
    @Mock
    private AuthenticationPersistenceService authenticationPersistenceService;
    @Mock
    private MetricService metricService;

    private Fido2Configuration fido2Configuration;

    @BeforeEach
    void setUp() {
        fido2Configuration = new Fido2Configuration();
        fido2Configuration.setUnfinishedRequestExpiration(180);
        fido2Configuration.setAbandonedRequestExpiration(86400);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Configuration);
        when(authenticationPersistenceService.getCeremonyBaseDns()).thenReturn(List.of(PEOPLE_DN, ASSERTION_DN));
    }

    /**
     * A ceremony that lapsed without ever being completed must be relabelled rather than left at
     * pending for the cleaner to delete unlabelled, and retained on its own short window.
     */
    @Test
    void processImpl_ifCeremonyLapsed_marksAbandonedAndRetainsOnAbandonedWindow() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = ceremonyEntry(authData);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(entry));

        timer.processImpl();

        assertEquals(Fido2AuthenticationStatus.abandoned, authData.getStatus());
        assertEquals(86400, entry.getTtl());
        verify(authenticationPersistenceService).update(entry);
    }

    /**
     * Conditional-UI ceremonies live under the assertion base DN rather than beneath a person entry,
     * and are the ones most likely to be abandoned. A sweep that visited only one subtree would miss
     * exactly those.
     */
    @Test
    void processImpl_sweepsBothCeremonySubtrees() {
        Fido2AuthenticationData identified = pendingCeremony("alice");
        Fido2AuthenticationData usernameless = pendingCeremony(null);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(ceremonyEntry(identified)));
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(ASSERTION_DN), any(), anyInt()))
                .thenReturn(List.of(ceremonyEntry(usernameless)));

        timer.processImpl();

        assertEquals(Fido2AuthenticationStatus.abandoned, identified.getStatus());
        assertEquals(Fido2AuthenticationStatus.abandoned, usernameless.getStatus());
        verify(authenticationPersistenceService, times(2)).update(any());
    }

    /**
     * The abandonment metric is attributed to the ceremony user and timed from when the ceremony was
     * issued, not from when the sweep happened to run.
     */
    @Test
    void processImpl_recordsAbandonmentAgainstCeremonyStartTime() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        Fido2AuthenticationEntry entry = ceremonyEntry(authData);
        Date issuedAt = entry.getCreationDate();
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(entry));

        timer.processImpl();

        verify(metricService).recordPasskeyAuthenticationAbandoned("alice", issuedAt.getTime(), null);
    }

    /**
     * The transition only fires from pending, which is what makes the sweep idempotent — across
     * repeated passes, across nodes, and across two base DNs that resolve to the same table on RDBMS.
     */
    @Test
    void processImpl_ifCeremonyAlreadyClaimed_doesNotRewriteOrDoubleCount() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        authData.setStatus(Fido2AuthenticationStatus.abandoned);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(ceremonyEntry(authData)));

        timer.processImpl();

        verify(authenticationPersistenceService, never()).update(any());
        verify(metricService, never()).recordPasskeyAuthenticationAbandoned(any(), anyLong(), any());
    }

    /**
     * A ceremony that was completed after the sweep read it must keep its outcome.
     */
    @Test
    void processImpl_ifCeremonyCompletedAfterRead_doesNotOverwriteOutcome() {
        Fido2AuthenticationData authData = pendingCeremony("alice");
        authData.setStatus(Fido2AuthenticationStatus.authenticated);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(ceremonyEntry(authData)));

        timer.processImpl();

        assertEquals(Fido2AuthenticationStatus.authenticated, authData.getStatus());
        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * One unwritable row must not abort the rest of the batch.
     */
    @Test
    void processImpl_ifOneRowCannotBeWritten_stillSweepsTheRest() {
        Fido2AuthenticationData broken = pendingCeremony("alice");
        Fido2AuthenticationData healthy = pendingCeremony("bob");
        Fido2AuthenticationEntry brokenEntry = ceremonyEntry(broken);
        Fido2AuthenticationEntry healthyEntry = ceremonyEntry(healthy);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenReturn(List.of(brokenEntry, healthyEntry));
        doThrow(new IllegalStateException("persistence down")).when(authenticationPersistenceService)
                .update(brokenEntry);

        timer.processImpl();

        verify(authenticationPersistenceService).update(healthyEntry);
    }

    /**
     * One unreadable subtree must not stop the other from being swept.
     */
    @Test
    void processImpl_ifOneSubtreeCannotBeRead_stillSweepsTheOther() {
        Fido2AuthenticationData usernameless = pendingCeremony(null);
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(PEOPLE_DN), any(), anyInt()))
                .thenThrow(new IllegalStateException("subtree unavailable"));
        when(authenticationPersistenceService.findLapsedPendingCeremonies(eq(ASSERTION_DN), any(), anyInt()))
                .thenReturn(List.of(ceremonyEntry(usernameless)));

        timer.processImpl();

        assertEquals(Fido2AuthenticationStatus.abandoned, usernameless.getStatus());
    }

    /**
     * The master switch restores the previous delete-unlabelled behaviour, and is re-read each pass
     * so it takes effect on a running server without a restart.
     */
    @Test
    void processImpl_ifRecordingDisabled_sweepsNothing() {
        fido2Configuration.setRecordAbandonedAssertions(false);

        timer.processImpl();

        verify(authenticationPersistenceService, never()).findLapsedPendingCeremonies(any(), any(), anyInt());
        verify(authenticationPersistenceService, never()).update(any());
    }

    /**
     * The sweep window is derived from the configured ceremony expiration, not a hard-coded default —
     * the reproduction deployment runs 180s while the code default is 120s.
     */
    @Test
    void processImpl_usesConfiguredCeremonyWindowForLapseCutoff() {
        fido2Configuration.setUnfinishedRequestExpiration(180);
        long before = System.currentTimeMillis();

        timer.processImpl();

        long after = System.currentTimeMillis();
        ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
        verify(authenticationPersistenceService).findLapsedPendingCeremonies(eq(PEOPLE_DN), cutoff.capture(), anyInt());

        // The cutoff sits exactly one configured ceremony window behind whenever the sweep ran, so
        // anything issued at or before it has outlived its window.
        long cutoffMillis = cutoff.getValue().getTime();
        assertTrue(cutoffMillis >= before - 180_000L && cutoffMillis <= after - 180_000L,
                "cutoff should be one 180s window behind the sweep, was " + (after - cutoffMillis) + "ms behind");
    }

    private Fido2AuthenticationData pendingCeremony(String username) {
        Fido2AuthenticationData authData = new Fido2AuthenticationData();
        authData.setUsername(username);
        authData.setStatus(Fido2AuthenticationStatus.pending);
        return authData;
    }

    /**
     * Entries are identified by DN — two entries sharing one would compare equal and make any
     * per-entry verification ambiguous.
     */
    private Fido2AuthenticationEntry ceremonyEntry(Fido2AuthenticationData authData) {
        return ceremonyEntry(authData, "ceremony-" + nextCeremonyId++);
    }

    private Fido2AuthenticationEntry ceremonyEntry(Fido2AuthenticationData authData, String id) {
        Fido2AuthenticationEntry entry = new Fido2AuthenticationEntry();
        entry.setDn("jansId=" + id + "," + PEOPLE_DN);
        entry.setId(id);
        entry.setCreationDate(new Date(System.currentTimeMillis() - 200_000L));
        entry.setAuthenticationData(authData);
        entry.setAuthenticationStatus(authData.getStatus());
        return entry;
    }

    private int nextCeremonyId = 1;
}
