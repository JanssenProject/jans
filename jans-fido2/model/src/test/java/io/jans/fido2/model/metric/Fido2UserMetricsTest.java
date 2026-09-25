/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Fido2UserMetricsTest {

    private static final double DELTA = 1e-9;
    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    @Test
    void testDefaultConstructorInitialState() {
        long before = System.currentTimeMillis();
        Fido2UserMetrics m = new Fido2UserMetrics();
        long after = System.currentTimeMillis();

        assertEquals(0, m.getTotalRegistrations());
        assertEquals(0, m.getTotalAuthentications());
        assertEquals(0, m.getSuccessfulRegistrations());
        assertEquals(0, m.getSuccessfulAuthentications());
        assertEquals(0, m.getFailedRegistrations());
        assertEquals(0, m.getFailedAuthentications());
        assertEquals(0, m.getFallbackEvents());

        assertTrue(m.getIsActive(), "isActive must default to true");

        assertNotNull(m.getLastUpdated());
        long ts = m.getLastUpdated().getTime();
        assertTrue(ts >= before && ts <= after,
                "lastUpdated must be stamped within the construction window");
    }

    @Test
    void testTwoArgConstructor() {
        Fido2UserMetrics m = new Fido2UserMetrics("u-1", "alice");

        assertEquals("u-1", m.getUserId());
        assertEquals("alice", m.getUsername());

        assertNotNull(m.getId(), "2-arg ctor must generate an id");
        UUID parsed = UUID.fromString(m.getId());
        assertNotNull(parsed);

        assertEquals(0, m.getTotalRegistrations(), "default-ctor zeroing must still apply");
        assertEquals(0, m.getTotalAuthentications());
        assertEquals(0, m.getFallbackEvents());
        assertTrue(m.getIsActive(), "default-ctor isActive=true must still apply");
        assertNotNull(m.getLastUpdated());
    }

    @Test
    void testIncrementRegistrationsSuccess() {
        Fido2UserMetrics m = new Fido2UserMetrics();

        long before = System.currentTimeMillis();
        m.incrementRegistrations(true);
        long after = System.currentTimeMillis();

        assertEquals(1, m.getTotalRegistrations());
        assertEquals(1, m.getSuccessfulRegistrations());
        assertEquals(0, m.getFailedRegistrations());

        assertNotNull(m.getLastActivityDate());
        assertNotNull(m.getLastUpdated());
        assertTrue(m.getLastActivityDate().getTime() >= before && m.getLastActivityDate().getTime() <= after);
        assertTrue(m.getLastUpdated().getTime() >= before && m.getLastUpdated().getTime() <= after);
    }

    @Test
    void testIncrementRegistrationsFailure() {
        Fido2UserMetrics m = new Fido2UserMetrics();

        long before = System.currentTimeMillis();
        m.incrementRegistrations(false);
        long after = System.currentTimeMillis();

        assertEquals(1, m.getTotalRegistrations());
        assertEquals(0, m.getSuccessfulRegistrations());
        assertEquals(1, m.getFailedRegistrations());

        assertNotNull(m.getLastActivityDate());
        assertNotNull(m.getLastUpdated());
        assertTrue(m.getLastActivityDate().getTime() >= before && m.getLastActivityDate().getTime() <= after);
        assertTrue(m.getLastUpdated().getTime() >= before && m.getLastUpdated().getTime() <= after);
    }

    @Test
    void testIncrementAuthenticationsSuccessAndFailure() {
        Fido2UserMetrics m = new Fido2UserMetrics();

        long before = System.currentTimeMillis();
        m.incrementAuthentications(true);
        long after = System.currentTimeMillis();

        assertEquals(1, m.getTotalAuthentications());
        assertEquals(1, m.getSuccessfulAuthentications());
        assertEquals(0, m.getFailedAuthentications());
        assertNotNull(m.getLastActivityDate());
        assertNotNull(m.getLastUpdated());
        assertTrue(m.getLastActivityDate().getTime() >= before && m.getLastActivityDate().getTime() <= after);
        assertTrue(m.getLastUpdated().getTime() >= before && m.getLastUpdated().getTime() <= after);

        m.incrementAuthentications(false);
        assertEquals(2, m.getTotalAuthentications());
        assertEquals(1, m.getSuccessfulAuthentications());
        assertEquals(1, m.getFailedAuthentications());
    }

    @Test
    void testIncrementFallbackEvents() {
        Fido2UserMetrics m = new Fido2UserMetrics();

        long before = System.currentTimeMillis();
        m.incrementFallbackEvents();
        long after = System.currentTimeMillis();

        assertEquals(1, m.getFallbackEvents());
        assertNotNull(m.getLastActivityDate());
        assertNotNull(m.getLastUpdated());
        assertTrue(m.getLastActivityDate().getTime() >= before && m.getLastActivityDate().getTime() <= after);
        assertTrue(m.getLastUpdated().getTime() >= before && m.getLastUpdated().getTime() <= after);
    }

    @Test
    void testGetRegistrationSuccessRateHappyPath() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(10);
        m.setSuccessfulRegistrations(7);

        assertEquals(0.7, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void testGetRegistrationSuccessRateZeroAndNullGuards() {
        Fido2UserMetrics zeroTotals = new Fido2UserMetrics();
        assertEquals(0.0, zeroTotals.getRegistrationSuccessRate(), DELTA,
                "must return 0.0 (not NaN) when totalRegistrations is 0");

        Fido2UserMetrics nullTotals = new Fido2UserMetrics();
        nullTotals.setTotalRegistrations(null);
        assertEquals(0.0, nullTotals.getRegistrationSuccessRate(), DELTA,
                "must return 0.0 when totalRegistrations is null");
    }

    @Test
    void testGetAuthenticationSuccessRateHappyPathAndGuards() {
        Fido2UserMetrics happy = new Fido2UserMetrics();
        happy.setTotalAuthentications(8);
        happy.setSuccessfulAuthentications(6);
        assertEquals(0.75, happy.getAuthenticationSuccessRate(), DELTA);

        Fido2UserMetrics zero = new Fido2UserMetrics();
        assertEquals(0.0, zero.getAuthenticationSuccessRate(), DELTA);

        Fido2UserMetrics nullTotals = new Fido2UserMetrics();
        nullTotals.setTotalAuthentications(null);
        assertEquals(0.0, nullTotals.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void testGetOverallSuccessRateHappyPath() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(4);
        m.setSuccessfulRegistrations(2);
        m.setTotalAuthentications(6);
        m.setSuccessfulAuthentications(3);

        assertEquals(0.5, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void testGetOverallSuccessRateNullAndZeroGuards() {
        Fido2UserMetrics bothZero = new Fido2UserMetrics();
        assertEquals(0.0, bothZero.getOverallSuccessRate(), DELTA);

        Fido2UserMetrics bothNull = new Fido2UserMetrics();
        bothNull.setTotalRegistrations(null);
        bothNull.setTotalAuthentications(null);
        bothNull.setSuccessfulRegistrations(null);
        bothNull.setSuccessfulAuthentications(null);
        assertEquals(0.0, bothNull.getOverallSuccessRate(), DELTA);

        Fido2UserMetrics oneSideNull = new Fido2UserMetrics();
        oneSideNull.setTotalRegistrations(null);
        oneSideNull.setSuccessfulRegistrations(null);
        oneSideNull.setTotalAuthentications(4);
        oneSideNull.setSuccessfulAuthentications(1);
        assertEquals(0.25, oneSideNull.getOverallSuccessRate(), DELTA,
                "null side must be treated as 0 and the other side must drive the rate");
    }

    @Test
    void testGetFallbackRateHappyPathAndGuards() {
        Fido2UserMetrics happy = new Fido2UserMetrics();
        happy.setTotalRegistrations(4);
        happy.setTotalAuthentications(6);
        happy.setFallbackEvents(2);
        assertEquals(0.2, happy.getFallbackRate(), DELTA);

        Fido2UserMetrics bothZero = new Fido2UserMetrics();
        assertEquals(0.0, bothZero.getFallbackRate(), DELTA);

        Fido2UserMetrics bothNull = new Fido2UserMetrics();
        bothNull.setTotalRegistrations(null);
        bothNull.setTotalAuthentications(null);
        assertEquals(0.0, bothNull.getFallbackRate(), DELTA);

        Fido2UserMetrics nullFallback = new Fido2UserMetrics();
        nullFallback.setTotalRegistrations(5);
        nullFallback.setTotalAuthentications(5);
        nullFallback.setFallbackEvents(null);
        assertEquals(0.0, nullFallback.getFallbackRate(), DELTA,
                "null fallbackEvents must be treated as 0, not NPE");
    }

    @Test
    void testUpdateEngagementLevelBoundary50() {
        Fido2UserMetrics below = new Fido2UserMetrics();
        below.setTotalRegistrations(24);
        below.setTotalAuthentications(25);
        below.updateEngagementLevel();
        assertEquals("MEDIUM", below.getEngagementLevel(), "49 total → MEDIUM (catches >= vs > regression)");

        Fido2UserMetrics atBoundary = new Fido2UserMetrics();
        atBoundary.setTotalRegistrations(25);
        atBoundary.setTotalAuthentications(25);
        atBoundary.updateEngagementLevel();
        assertEquals("HIGH", atBoundary.getEngagementLevel(), "50 total → HIGH");
    }

    @Test
    void testUpdateEngagementLevelBoundary10() {
        Fido2UserMetrics below = new Fido2UserMetrics();
        below.setTotalRegistrations(4);
        below.setTotalAuthentications(5);
        below.updateEngagementLevel();
        assertEquals("LOW", below.getEngagementLevel(), "9 total → LOW");

        Fido2UserMetrics atBoundary = new Fido2UserMetrics();
        atBoundary.setTotalRegistrations(5);
        atBoundary.setTotalAuthentications(5);
        atBoundary.updateEngagementLevel();
        assertEquals("MEDIUM", atBoundary.getEngagementLevel(), "10 total → MEDIUM");
    }

    @Test
    void testUpdateEngagementLevelNullTotals() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(null);
        m.setTotalAuthentications(null);
        m.updateEngagementLevel();

        assertEquals("LOW", m.getEngagementLevel(), "null totals must be treated as 0 → LOW");
    }

    @Test
    void testUpdateAdoptionStageBoundaries() {
        Fido2UserMetrics zero = new Fido2UserMetrics();
        zero.setTotalRegistrations(0);
        zero.updateAdoptionStage();
        assertEquals("NEW", zero.getAdoptionStage());

        Fido2UserMetrics one = new Fido2UserMetrics();
        one.setTotalRegistrations(1);
        one.updateAdoptionStage();
        assertEquals("LEARNING", one.getAdoptionStage());

        Fido2UserMetrics five = new Fido2UserMetrics();
        five.setTotalRegistrations(5);
        five.updateAdoptionStage();
        assertEquals("ADOPTED", five.getAdoptionStage());

        Fido2UserMetrics six = new Fido2UserMetrics();
        six.setTotalRegistrations(6);
        six.updateAdoptionStage();
        assertEquals("EXPERT", six.getAdoptionStage());

        Fido2UserMetrics nullRegs = new Fido2UserMetrics();
        nullRegs.setTotalRegistrations(null);
        nullRegs.updateAdoptionStage();
        assertEquals("NEW", nullRegs.getAdoptionStage(), "null totalRegistrations must map to NEW");
    }

    @Test
    void testIsNewUser() {
        Fido2UserMetrics recent = new Fido2UserMetrics();
        recent.setFirstRegistrationDate(new Date(System.currentTimeMillis() - 29 * ONE_DAY_MS));
        assertTrue(recent.isNewUser(), "29 days ago must still count as new");

        Fido2UserMetrics old = new Fido2UserMetrics();
        old.setFirstRegistrationDate(new Date(System.currentTimeMillis() - 31 * ONE_DAY_MS));
        assertFalse(old.isNewUser(), "31 days ago must not count as new");

        Fido2UserMetrics nullDate = new Fido2UserMetrics();
        nullDate.setFirstRegistrationDate(null);
        assertFalse(nullDate.isNewUser(), "null firstRegistrationDate must not throw and must return false");
    }

    @Test
    void testIsActiveUser() {
        Fido2UserMetrics recent = new Fido2UserMetrics();
        recent.setLastActivityDate(new Date(System.currentTimeMillis() - 29 * ONE_DAY_MS));
        assertTrue(recent.isActiveUser(), "29 days ago must still count as active");

        Fido2UserMetrics old = new Fido2UserMetrics();
        old.setLastActivityDate(new Date(System.currentTimeMillis() - 31 * ONE_DAY_MS));
        assertFalse(old.isActiveUser(), "31 days ago must not count as active");

        Fido2UserMetrics nullDate = new Fido2UserMetrics();
        nullDate.setLastActivityDate(null);
        assertFalse(nullDate.isActiveUser(), "null lastActivityDate must not throw and must return false");
    }

    @Test
    void testEqualsContractForIdentityTuple() {
        Date firstReg = new Date(1_700_000_000_000L);

        Fido2UserMetrics a = new Fido2UserMetrics();
        a.setUserId("u-1");
        a.setUsername("alice");
        a.setFirstRegistrationDate(firstReg);

        Fido2UserMetrics b = new Fido2UserMetrics();
        b.setUserId("u-1");
        b.setUsername("alice");
        b.setFirstRegistrationDate(firstReg);

        // identical identity tuple → equal + same hash
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // counters and engagement level are NOT part of the identity tuple
        b.setTotalRegistrations(999);
        b.setEngagementLevel("HIGH");
        assertEquals(a, b, "unrelated fields must not break equality");
        assertEquals(a.hashCode(), b.hashCode(), "unrelated fields must not change hashCode");

        // reflexive
        assertEquals(a, a);

        // null and wrong-type
        assertNotEquals(a, null);
        assertNotEquals(a, "not-a-Fido2UserMetrics");

        // differing on each of the three identity-tuple fields breaks equality
        Fido2UserMetrics diffUserId = new Fido2UserMetrics();
        diffUserId.setUserId("u-2");
        diffUserId.setUsername("alice");
        diffUserId.setFirstRegistrationDate(firstReg);
        assertNotEquals(a, diffUserId);

        Fido2UserMetrics diffUsername = new Fido2UserMetrics();
        diffUsername.setUserId("u-1");
        diffUsername.setUsername("bob");
        diffUsername.setFirstRegistrationDate(firstReg);
        assertNotEquals(a, diffUsername);

        Fido2UserMetrics diffDate = new Fido2UserMetrics();
        diffDate.setUserId("u-1");
        diffDate.setUsername("alice");
        diffDate.setFirstRegistrationDate(new Date(firstReg.getTime() + 1));
        assertNotEquals(a, diffDate);
    }
}
