/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import io.jans.fido2.model.conf.Fido2Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbandonedCeremonyPolicyTest {

    /**
     * The invariant the whole sweep rests on: a pending ceremony must still exist when the first sweep
     * after its window runs. If retention did not exceed the window plus one full interval, the
     * cleaner could delete a lapsed ceremony before it was ever labelled.
     */
    @Test
    void pendingRetentionAlwaysOutlivesTheFirstSweepAfterTheWindow() {
        int[][] configurations = {
                { 180, 30 },    // the reported deployment
                { 120, 30 },    // code defaults
                { 120, 3600 },  // interval far larger than the window
                { 120, 0 },     // unusable interval
                { 1, 30 },      // window shorter than the interval
        };

        for (int[] configuration : configurations) {
            Fido2Configuration fido2Configuration = configuration(configuration[0], configuration[1]);

            int interval = AbandonedCeremonyPolicy.effectiveSweepInterval(fido2Configuration);
            int retention = AbandonedCeremonyPolicy.pendingCeremonyRetention(fido2Configuration);

            assertTrue(interval > 0, "interval must be runnable for " + configuration[0] + "/" + configuration[1]);
            // Worst case, a ceremony lapses just after a pass, so the next one is a full interval later.
            assertTrue(retention > configuration[0] + interval,
                    "retention " + retention + " must outlive window " + configuration[0] + " plus interval "
                            + interval);
        }
    }

    /**
     * An interval at or above the ceremony window is not usable: a ceremony could lapse and be deleted
     * between two passes. It is capped rather than honoured.
     */
    @Test
    void sweepIntervalIsCappedBelowTheCeremonyWindow() {
        Fido2Configuration fido2Configuration = configuration(180, 3600);

        int interval = AbandonedCeremonyPolicy.effectiveSweepInterval(fido2Configuration);

        assertTrue(interval < 180, "interval must stay below the ceremony window, was " + interval);
        assertTrue(AbandonedCeremonyPolicy.isSweepIntervalOverridden(fido2Configuration));
    }

    @Test
    void usableSweepIntervalIsHonouredUnchanged() {
        Fido2Configuration fido2Configuration = configuration(180, 30);

        assertEquals(30, AbandonedCeremonyPolicy.effectiveSweepInterval(fido2Configuration));
        assertFalse(AbandonedCeremonyPolicy.isSweepIntervalOverridden(fido2Configuration));
    }

    /**
     * A deployment that opted out keeps exactly the retention it had before, so nothing changes for it.
     */
    @Test
    void retentionIsUnchangedWhenRecordingIsDisabled() {
        Fido2Configuration fido2Configuration = configuration(180, 30);
        fido2Configuration.setRecordAbandonedAssertions(false);

        assertEquals(180, AbandonedCeremonyPolicy.pendingCeremonyRetention(fido2Configuration));
    }

    private Fido2Configuration configuration(int unfinishedRequestExpiration, int sweepInterval) {
        Fido2Configuration fido2Configuration = new Fido2Configuration();
        fido2Configuration.setUnfinishedRequestExpiration(unfinishedRequestExpiration);
        fido2Configuration.setAbandonedRequestSweepInterval(sweepInterval);
        return fido2Configuration;
    }
}
