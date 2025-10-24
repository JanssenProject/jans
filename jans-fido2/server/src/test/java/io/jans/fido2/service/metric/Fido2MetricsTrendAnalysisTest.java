/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.metric.Fido2MetricsAggregation;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FIDO2 Metrics Trend Analysis functionality (GitHub Issue #11923)
 * 
 * @author FIDO2 Team
 */
@ExtendWith(MockitoExtension.class)
public class Fido2MetricsTrendAnalysisTest {

    @Mock
    private Logger log;

    @Mock
    private io.jans.fido2.model.conf.AppConfiguration appConfiguration;

    @Mock
    private io.jans.orm.PersistenceEntryManager persistenceEntryManager;

    @InjectMocks
    private Fido2MetricsService metricsService;

    private List<Fido2MetricsAggregation> testAggregations;

    @BeforeEach
    void setUp() {
        // Create test aggregations with increasing trend
        testAggregations = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Fido2MetricsAggregation aggregation = new Fido2MetricsAggregation("DAILY", "2024-01-0" + (i + 1), 
                LocalDateTime.now().minusDays(4 - i), LocalDateTime.now().minusDays(3 - i));
            
            Map<String, Object> metricsData = new HashMap<>();
            metricsData.put(Fido2MetricsConstants.REGISTRATION_ATTEMPTS, 100L + (i * 20));
            metricsData.put(Fido2MetricsConstants.REGISTRATION_SUCCESSES, 95L + (i * 18));
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS, 500L + (i * 50));
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_SUCCESSES, 480L + (i * 45));
            metricsData.put(Fido2MetricsConstants.FALLBACK_EVENTS, 5L + i);
            metricsData.put(Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE, 0.95 + (i * 0.01));
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE, 0.96 + (i * 0.005));
            
            aggregation.setMetricsData(metricsData);
            testAggregations.add(aggregation);
        }
    }

    @Test
    void testGetTrendAnalysis_WithValidData_ReturnsCorrectTrend() {
        // Mock the getAggregations method to return test data
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenReturn(testAggregations);

        // Execute trend analysis
        Map<String, Object> result = metricsService.getTrendAnalysis("DAILY", 
            LocalDateTime.now().minusDays(5), LocalDateTime.now());

        // Verify results
        assertNotNull(result);
        assertTrue(result.containsKey("dataPoints"));
        assertTrue(result.containsKey("growthRate"));
        assertTrue(result.containsKey("trendDirection"));
        assertTrue(result.containsKey("insights"));

        // Verify trend direction is INCREASING (since we have increasing data)
        assertEquals("INCREASING", result.get("trendDirection"));
        
        // Verify growth rate is positive
        Double growthRate = (Double) result.get("growthRate");
        assertTrue(growthRate > 0);
    }

    @Test
    void testGetTrendAnalysis_WithEmptyData_ReturnsEmptyMap() {
        // Mock empty result
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenReturn(Collections.emptyList());

        // Execute trend analysis
        Map<String, Object> result = metricsService.getTrendAnalysis("DAILY", 
            LocalDateTime.now().minusDays(5), LocalDateTime.now());

        // Verify empty result
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPeriodOverPeriodComparison_WithValidData_ReturnsComparison() {
        // Mock current and previous period data
        List<Fido2MetricsAggregation> currentPeriod = testAggregations.subList(0, 3);
        List<Fido2MetricsAggregation> previousPeriod = testAggregations.subList(2, 5);
        
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenReturn(currentPeriod)
            .thenReturn(previousPeriod);

        // Execute period comparison
        Map<String, Object> result = metricsService.getPeriodOverPeriodComparison("DAILY", 3);

        // Verify results
        assertNotNull(result);
        assertTrue(result.containsKey("currentPeriod"));
        assertTrue(result.containsKey("previousPeriod"));
        assertTrue(result.containsKey("comparison"));

        // Verify comparison contains change percentage
        Map<String, Object> comparison = (Map<String, Object>) result.get("comparison");
        assertTrue(comparison.containsKey("totalOperationsChange"));
    }

    @Test
    void testGetAggregationSummary_WithValidData_ReturnsSummary() {
        // Mock aggregation data
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenReturn(testAggregations);

        // Execute summary
        Map<String, Object> result = metricsService.getAggregationSummary("DAILY", 
            LocalDateTime.now().minusDays(5), LocalDateTime.now());

        // Verify results
        assertNotNull(result);
        assertTrue(result.containsKey("totalRegistrations"));
        assertTrue(result.containsKey("totalAuthentications"));
        assertTrue(result.containsKey("totalFallbacks"));
        assertTrue(result.containsKey("totalOperations"));
        assertTrue(result.containsKey("avgRegistrationSuccessRate"));
        assertTrue(result.containsKey("avgAuthenticationSuccessRate"));

        // Verify totals are calculated correctly
        Long totalRegistrations = (Long) result.get("totalRegistrations");
        Long totalAuthentications = (Long) result.get("totalAuthentications");
        Long totalOperations = (Long) result.get("totalOperations");
        
        assertEquals(totalRegistrations + totalAuthentications, totalOperations);
    }

    @Test
    void testGetTrendAnalysis_WithException_ReturnsEmptyMap() {
        // Mock exception
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenThrow(new RuntimeException("Database error"));

        // Execute trend analysis
        Map<String, Object> result = metricsService.getTrendAnalysis("DAILY", 
            LocalDateTime.now().minusDays(5), LocalDateTime.now());

        // Verify empty result on exception
        assertTrue(result.isEmpty());
    }

    @Test
    void testTrendAnalysis_WithStableData_ReturnsStableTrend() {
        // Create stable data (same values)
        List<Fido2MetricsAggregation> stableAggregations = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            Fido2MetricsAggregation aggregation = new Fido2MetricsAggregation("DAILY", "2024-01-0" + (i + 1), 
                LocalDateTime.now().minusDays(2 - i), LocalDateTime.now().minusDays(1 - i));
            
            Map<String, Object> metricsData = new HashMap<>();
            metricsData.put(Fido2MetricsConstants.REGISTRATION_ATTEMPTS, 100L);
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS, 500L);
            aggregation.setMetricsData(metricsData);
            stableAggregations.add(aggregation);
        }
        
        when(persistenceEntryManager.findEntries(anyString(), eq(Fido2MetricsAggregation.class), any()))
            .thenReturn(stableAggregations);

        // Execute trend analysis
        Map<String, Object> result = metricsService.getTrendAnalysis("DAILY", 
            LocalDateTime.now().minusDays(3), LocalDateTime.now());

        // Verify stable trend
        assertEquals("STABLE", result.get("trendDirection"));
        assertEquals(0.0, (Double) result.get("growthRate"), 0.01);
    }
}

