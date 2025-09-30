/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

/**
 * Constants for FIDO2 metrics to avoid string literal duplication
 *
 * @author Janssen Project
 * @version 1.0
 */
public final class Fido2MetricsConstants {

    // Prevent instantiation
    private Fido2MetricsConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Metric Keys
    public static final String TOTAL_UNIQUE_USERS = "totalUniqueUsers";
    public static final String NEW_USERS = "newUsers";
    public static final String RETURNING_USERS = "returningUsers";
    public static final String ADOPTION_RATE = "adoptionRate";
    public static final String SUCCESS_RATE = "successRate";
    public static final String FAILURE_RATE = "failureRate";

    // Aggregation Types
    public static final String HOURLY = "HOURLY";
    public static final String DAILY = "DAILY";
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";

    // Operation Types
    public static final String REGISTRATION = "REGISTRATION";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String FALLBACK = "FALLBACK";

    // Operation Status
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String ATTEMPT = "ATTEMPT";

    // Report Categories
    public static final String CATEGORY = "category";
    public static final String PRIORITY = "priority";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String ACTIONS = "actions";

    // Trend Types
    public static final String STABLE = "STABLE";
    public static final String INCREASING = "INCREASING";
    public static final String DECREASING = "DECREASING";

    // Data Quality
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";

    // Engagement Levels
    public static final String HIGH_ENGAGEMENT = "HIGH";
    public static final String MEDIUM_ENGAGEMENT = "MEDIUM";
    public static final String LOW_ENGAGEMENT = "LOW";

    // Adoption Stages
    public static final String EARLY = "EARLY";
    public static final String GROWTH = "GROWTH";
    public static final String MATURE = "MATURE";

    // Database Attributes
    public static final String JANS_TIMESTAMP = "jansTimestamp";
    
    // Service Names
    public static final String METRICS_SERVICE = "metricsService";
    
    // Benchmark Names
    public static final String REGISTRATION_BENCHMARK = "registrationBenchmark";
    public static final String AUTHENTICATION_BENCHMARK = "authenticationBenchmark";
    
    // Base DNs
    public static final String FIDO2_METRICS_ENTRY_BASE_DN = "ou=fido2-metrics,o=jans";
    public static final String FIDO2_METRICS_AGGREGATION_BASE_DN = "ou=fido2-aggregations,o=jans";
    public static final String FIDO2_USER_METRICS_BASE_DN = "ou=fido2-user-metrics,o=jans";
    
    // Metrics data keys
    public static final String REGISTRATION_ATTEMPTS = "registrationAttempts";
    public static final String REGISTRATION_SUCCESSES = "registrationSuccesses";
    public static final String REGISTRATION_FAILURES = "registrationFailures";
    public static final String REGISTRATION_SUCCESS_RATE = "registrationSuccessRate";
    public static final String REGISTRATION_AVG_DURATION = "registrationAvgDuration";
    public static final String AUTHENTICATION_ATTEMPTS = "authenticationAttempts";
    public static final String AUTHENTICATION_SUCCESSES = "authenticationSuccesses";
    public static final String AUTHENTICATION_FAILURES = "authenticationFailures";
    public static final String AUTHENTICATION_SUCCESS_RATE = "authenticationSuccessRate";
    public static final String AUTHENTICATION_AVG_DURATION = "authenticationAvgDuration";
    public static final String FALLBACK_EVENTS = "fallbackEvents";
    public static final String DEVICE_TYPES = "deviceTypes";
    public static final String ERROR_COUNTS = "errorCounts";
    public static final String PERFORMANCE_METRICS = "performanceMetrics";
}
