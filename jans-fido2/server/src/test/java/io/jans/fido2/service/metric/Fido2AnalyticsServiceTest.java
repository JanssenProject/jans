/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.metric.Fido2MetricsConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link Fido2AnalyticsService#generateExecutiveSummary}.
 *
 * <p>{@code getUserAdoptionMetrics} stores {@code totalUniqueUsers} and {@code newUsers} as
 * {@code Set.size()} — an {@code int}, autoboxed to {@code Integer} — so casting either to
 * {@code Long} here threw {@code ClassCastException} on every window with at least one active user.
 * The recomputed rate also measured window activity rather than a user population, which is exactly
 * the defect fixed in {@code Fido2MetricsService.getUserAdoptionMetrics} itself; reusing its
 * {@code adoptionRate} fixes both at once.
 *
 * @author Janssen Project
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Fido2AnalyticsServiceTest {

    @Mock
    private Logger log;

    @Mock
    private Fido2MetricsService metricsService;

    @InjectMocks
    private Fido2AnalyticsService fido2AnalyticsService;

    @Test
    void generateExecutiveSummary_ifWindowHasActiveUsers_doesNotThrow() {
        stubAdoption(Map.of(
                Fido2MetricsConstants.TOTAL_UNIQUE_USERS, 5,
                Fido2MetricsConstants.NEW_USERS, 2,
                Fido2MetricsConstants.ADOPTION_RATE, 0.6));

        Map<String, Object> summary = summary();

        assertEquals(0.6, (Double) summary.get(Fido2MetricsConstants.ADOPTION_RATE), 0.0001);
        assertTrue(insights(summary).stream().anyMatch(i -> i.contains("Strong user adoption")));
    }

    /**
     * A recompute from totalUniqueUsers/newUsers would have reported this as low adoption
     * (1 new / 5 active = 0.2), even though the population-correct adoptionRate is high. Reusing the
     * already-fixed figure keeps the two call sites in agreement instead of disagreeing about what
     * "adoption" means.
     */
    @Test
    void generateExecutiveSummary_usesAdoptionRateFromMetricsService_notWindowActivityRatio() {
        stubAdoption(Map.of(
                Fido2MetricsConstants.TOTAL_UNIQUE_USERS, 5,
                Fido2MetricsConstants.NEW_USERS, 1,
                Fido2MetricsConstants.ADOPTION_RATE, 0.75));

        List<String> insights = insights(summary());

        assertTrue(insights.stream().anyMatch(i -> i.contains("Strong user adoption")),
                "must reflect the fixed adoptionRate (0.75), not the window ratio newUsers/totalUniqueUsers (0.2)");
    }

    /**
     * With no population to measure adoption against, getUserAdoptionMetrics reports adoptionRate as
     * null rather than a misleading 0.0; the summary must not add an insight from an unknown rate, and
     * must not throw unboxing it.
     */
    @Test
    void generateExecutiveSummary_ifAdoptionRateIsUnknown_addsNoAdoptionInsight() {
        Map<String, Object> adoption = new HashMap<>();
        adoption.put(Fido2MetricsConstants.TOTAL_UNIQUE_USERS, 0);
        adoption.put(Fido2MetricsConstants.NEW_USERS, 0);
        adoption.put(Fido2MetricsConstants.ADOPTION_RATE, null);
        stubAdoption(adoption);

        List<String> insights = insights(summary());

        assertFalse(insights.stream().anyMatch(i -> i.contains("adoption")));
    }

    @SuppressWarnings("unchecked")
    private List<String> insights(Map<String, Object> summary) {
        return (List<String>) summary.get("keyInsights");
    }

    private void stubAdoption(Map<String, Object> adoption) {
        when(metricsService.getUserAdoptionMetrics(any(), any())).thenReturn(adoption);
        when(metricsService.getPerformanceMetrics(any(), any())).thenReturn(Map.of());
        when(metricsService.getErrorAnalysis(any(), any())).thenReturn(Map.of());
    }

    private Map<String, Object> summary() {
        return fido2AnalyticsService.generateExecutiveSummary(LocalDateTime.now().minusDays(1), LocalDateTime.now());
    }
}
