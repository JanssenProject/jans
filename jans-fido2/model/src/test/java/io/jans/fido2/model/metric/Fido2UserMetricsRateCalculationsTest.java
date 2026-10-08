/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the rate-calculation methods on {@link Fido2UserMetrics}:
 * {@code getRegistrationSuccessRate}, {@code getAuthenticationSuccessRate},
 * {@code getOverallSuccessRate}, and {@code getFallbackRate}.
 *
 * <p>These methods are the first metrics surface that exercises actual computation
 * rather than value mapping; the null-safety and divide-by-zero branches are the
 * lines most likely to silently regress under refactor.</p>
 *
 * @author Janssen Project
 * @version 1.0
 */
class Fido2UserMetricsRateCalculationsTest {

    private static final double DELTA = 1e-9;

    @Test
    void registrationSuccessRate_nullTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(null);
        m.setSuccessfulRegistrations(5);

        assertEquals(0.0, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void registrationSuccessRate_zeroTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(0);
        m.setSuccessfulRegistrations(0);

        assertEquals(0.0, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void registrationSuccessRate_partialSuccess_returnsRatio() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(4);
        m.setSuccessfulRegistrations(3);

        assertEquals(0.75, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void registrationSuccessRate_allSuccess_returnsOne() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(7);
        m.setSuccessfulRegistrations(7);

        assertEquals(1.0, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void registrationSuccessRate_zeroSuccessWithPositiveTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(5);
        m.setSuccessfulRegistrations(0);

        assertEquals(0.0, m.getRegistrationSuccessRate(), DELTA);
    }

    @Test
    void authenticationSuccessRate_nullTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalAuthentications(null);
        m.setSuccessfulAuthentications(5);

        assertEquals(0.0, m.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void authenticationSuccessRate_zeroTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalAuthentications(0);
        m.setSuccessfulAuthentications(0);

        assertEquals(0.0, m.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void authenticationSuccessRate_partialSuccess_returnsRatio() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalAuthentications(4);
        m.setSuccessfulAuthentications(3);

        assertEquals(0.75, m.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void authenticationSuccessRate_allSuccess_returnsOne() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalAuthentications(9);
        m.setSuccessfulAuthentications(9);

        assertEquals(1.0, m.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void authenticationSuccessRate_zeroSuccessWithPositiveTotal_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalAuthentications(5);
        m.setSuccessfulAuthentications(0);

        assertEquals(0.0, m.getAuthenticationSuccessRate(), DELTA);
    }

    @Test
    void overallSuccessRate_allNullTotals_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(null);
        m.setTotalAuthentications(null);
        m.setSuccessfulRegistrations(null);
        m.setSuccessfulAuthentications(null);

        assertEquals(0.0, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void overallSuccessRate_allZeroTotals_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(0);
        m.setTotalAuthentications(0);
        m.setSuccessfulRegistrations(0);
        m.setSuccessfulAuthentications(0);

        assertEquals(0.0, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void overallSuccessRate_sumsBothNumeratorsAndDenominators() {
        // (2 + 3) / (4 + 6) = 0.5 — catches "use only one side" regressions
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setSuccessfulRegistrations(2);
        m.setTotalRegistrations(4);
        m.setSuccessfulAuthentications(3);
        m.setTotalAuthentications(6);

        assertEquals(0.5, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void overallSuccessRate_mixedNullAuthSide_treatedAsZero() {
        // Registration side populated, authentication side fully null —
        // null side must be treated as 0, not propagate to NPE.
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setSuccessfulRegistrations(3);
        m.setTotalRegistrations(4);
        m.setSuccessfulAuthentications(null);
        m.setTotalAuthentications(null);

        assertEquals(0.75, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void overallSuccessRate_mixedNullRegSide_treatedAsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setSuccessfulRegistrations(null);
        m.setTotalRegistrations(null);
        m.setSuccessfulAuthentications(1);
        m.setTotalAuthentications(2);

        assertEquals(0.5, m.getOverallSuccessRate(), DELTA);
    }

    @Test
    void fallbackRate_allNullTotals_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(null);
        m.setTotalAuthentications(null);
        m.setFallbackEvents(7);

        assertEquals(0.0, m.getFallbackRate(), DELTA);
    }

    @Test
    void fallbackRate_allZeroTotals_returnsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(0);
        m.setTotalAuthentications(0);
        m.setFallbackEvents(7);

        assertEquals(0.0, m.getFallbackRate(), DELTA);
    }

    @Test
    void fallbackRate_nullFallbackEvents_treatedAsZero() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(3);
        m.setTotalAuthentications(5);
        m.setFallbackEvents(null);

        assertEquals(0.0, m.getFallbackRate(), DELTA);
    }

    @Test
    void fallbackRate_populated_returnsRatio() {
        // 2 / (3 + 5) = 0.25
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(3);
        m.setTotalAuthentications(5);
        m.setFallbackEvents(2);

        assertEquals(0.25, m.getFallbackRate(), DELTA);
    }

    @Test
    void fallbackRate_mixedNullTotalSide_treatedAsZero() {
        // 1 / (0 + 4) = 0.25
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(null);
        m.setTotalAuthentications(4);
        m.setFallbackEvents(1);

        assertEquals(0.25, m.getFallbackRate(), DELTA);
    }

    /**
     * The rate getters are pure reads — calling them must not mutate
     * {@code lastUpdated} (or any other field touched by mutating helpers).
     */
    @Test
    void rateGetters_haveNoSideEffectsOnLastUpdated() {
        Fido2UserMetrics m = new Fido2UserMetrics();
        m.setTotalRegistrations(4);
        m.setSuccessfulRegistrations(3);
        m.setTotalAuthentications(6);
        m.setSuccessfulAuthentications(3);
        m.setFallbackEvents(2);

        Date snapshot = new Date(1_700_000_000_000L);
        m.setLastUpdated(snapshot);
        Date lastActivitySnapshot = new Date(1_699_000_000_000L);
        m.setLastActivityDate(lastActivitySnapshot);

        // Invoke every rate getter.
        m.getRegistrationSuccessRate();
        m.getAuthenticationSuccessRate();
        m.getOverallSuccessRate();
        m.getFallbackRate();

        // Same reference, same value — no mutation occurred.
        assertNotNull(m.getLastUpdated());
        assertSame(snapshot, m.getLastUpdated(),
                "rate getters must not replace lastUpdated");
        assertEquals(snapshot, m.getLastUpdated());
        assertSame(lastActivitySnapshot, m.getLastActivityDate(),
                "rate getters must not replace lastActivityDate");

        // And the counters themselves must be unchanged.
        assertEquals(4, m.getTotalRegistrations());
        assertEquals(3, m.getSuccessfulRegistrations());
        assertEquals(6, m.getTotalAuthentications());
        assertEquals(3, m.getSuccessfulAuthentications());
        assertEquals(2, m.getFallbackEvents());
    }
}
