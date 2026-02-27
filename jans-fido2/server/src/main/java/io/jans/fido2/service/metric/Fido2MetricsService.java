/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsAggregation;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.model.metric.Fido2MetricsData;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.OptionalDouble;

/**
 * Service for managing FIDO2 metrics data operations
 * 
 * @author FIDO2 Team
 */
@ApplicationScoped
@Named("fido2MetricsService")
public class Fido2MetricsService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private PersistenceEntryManager persistenceEntryManager;

    private static final String METRICS_ENTRY_BASE_DN = "ou=fido2-metrics,o=jans";
    private static final String METRICS_AGGREGATION_BASE_DN = "ou=fido2-aggregations,o=jans";

    // ========== METRICS ENTRY OPERATIONS ==========

    /**
     * Store a metrics entry asynchronously
     */
    public void storeMetricsEntry(Fido2MetricsEntry entry) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                entry.setDn(generateMetricsEntryDn(entry.getId()));
                persistenceEntryManager.persist(entry);
                log.debug("Stored FIDO2 metrics entry: {}", entry.getId());
            } catch (Exception e) {
                log.error("Failed to store FIDO2 metrics entry: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Store metrics data as a metrics entry
     */
    public void storeMetricsData(Fido2MetricsData metricsData) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        Fido2MetricsEntry entry = convertToMetricsEntry(metricsData);
        storeMetricsEntry(entry);
    }

    /**
     * Get metrics entries by time range
     */
    public List<Fido2MetricsEntry> getMetricsEntries(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Convert LocalDateTime to Date for SQL persistence filters
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            Filter filter = Filter.createANDFilter(
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startDate),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endDate)
            );

            List<Fido2MetricsEntry> entries = persistenceEntryManager.findEntries(
                METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
            );

            return entries;
        } catch (Exception e) {
            log.error("Failed to retrieve metrics entries: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get metrics entries by user
     */
    public List<Fido2MetricsEntry> getMetricsEntriesByUser(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Convert LocalDateTime to Date for SQL persistence filters
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansFido2MetricsUserId", userId),
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startDate),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endDate)
            );

            return persistenceEntryManager.findEntries(
                METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve metrics entries for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get metrics entries by operation type
     */
    public List<Fido2MetricsEntry> getMetricsEntriesByOperation(String operationType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Convert LocalDateTime to Date for SQL persistence filters
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansFido2MetricsOperationType", operationType),
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startDate),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endDate)
            );

            return persistenceEntryManager.findEntries(
                METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve metrics entries for operation {}: {}", operationType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ========== AGGREGATION OPERATIONS ==========

    /**
     * Create hourly aggregation
     */
    public void createHourlyAggregation(LocalDateTime hour) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime startTime = hour.withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endTime = startTime.plusHours(1);

                Fido2MetricsAggregation aggregation = calculateAggregation(
                    "HOURLY", 
                    hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")),
                    startTime, 
                    endTime
                );

                if (aggregation != null) {
                    aggregation.setDn(generateAggregationDn(aggregation.getId()));
                    persistenceEntryManager.persist(aggregation);
                    log.debug("Created hourly aggregation for: {}", hour);
                }
            } catch (Exception e) {
                log.error("Failed to create hourly aggregation for {}: {}", hour, e.getMessage(), e);
            }
        });
    }

    /**
     * Create daily aggregation
     */
    public void createDailyAggregation(LocalDateTime day) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime startTime = day.withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endTime = startTime.plusDays(1);

                Fido2MetricsAggregation aggregation = calculateAggregation(
                    Fido2MetricsConstants.DAILY, 
                    day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    startTime, 
                    endTime
                );

                if (aggregation != null) {
                    aggregation.setDn(generateAggregationDn(aggregation.getId()));
                    persistenceEntryManager.persist(aggregation);
                    log.debug("Created daily aggregation for: {}", day);
                }
            } catch (Exception e) {
                log.error("Failed to create daily aggregation for {}: {}", day, e.getMessage(), e);
            }
        });
    }

    /**
     * Create weekly aggregation
     */
    public void createWeeklyAggregation(LocalDateTime week) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime startTime = week.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endTime = startTime.plusWeeks(1);

                Fido2MetricsAggregation aggregation = calculateAggregation(
                    Fido2MetricsConstants.WEEKLY, 
                    week.format(DateTimeFormatter.ofPattern("yyyy-'W'ww")),
                    startTime, 
                    endTime
                );

                if (aggregation != null) {
                    aggregation.setDn(generateAggregationDn(aggregation.getId()));
                    persistenceEntryManager.persist(aggregation);
                    log.debug("Created weekly aggregation for: {}", week);
                }
            } catch (Exception e) {
                log.error("Failed to create weekly aggregation for {}: {}", week, e.getMessage(), e);
            }
        });
    }

    /**
     * Create monthly aggregation
     */
    public void createMonthlyAggregation(LocalDateTime month) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                LocalDateTime startTime = month.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endTime = startTime.plusMonths(1);

                Fido2MetricsAggregation aggregation = calculateAggregation(
                    "MONTHLY", 
                    month.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    startTime, 
                    endTime
                );

                if (aggregation != null) {
                    aggregation.setDn(generateAggregationDn(aggregation.getId()));
                    persistenceEntryManager.persist(aggregation);
                    log.debug("Created monthly aggregation for: {}", month);
                }
            } catch (Exception e) {
                log.error("Failed to create monthly aggregation for {}: {}", month, e.getMessage(), e);
            }
        });
    }

    /**
     * Get aggregations by time range
     * Uses interval overlap logic: finds aggregations that overlap with the query range
     * An aggregation overlaps if: aggregation.startTime &lt;= queryEndTime AND aggregation.endTime &gt;= queryStartTime
     */
    public List<Fido2MetricsAggregation> getAggregations(String aggregationType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Convert LocalDateTime to Date for SQL persistence filters
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            // Interval overlap check: aggregation overlaps query if:
            // aggregation.startTime <= queryEndTime AND aggregation.endTime >= queryStartTime
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansAggregationType", aggregationType),
                Filter.createLessOrEqualFilter("jansStartTime", endDate),  // aggregation starts before/at query end
                Filter.createGreaterOrEqualFilter("jansEndTime", startDate) // aggregation ends after/at query start
            );

            return persistenceEntryManager.findEntries(
                METRICS_AGGREGATION_BASE_DN, Fido2MetricsAggregation.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve aggregations: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Cleanup old data based on retention policy
     */
    public void cleanupOldData(int retentionDays) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Use UTC timezone to align with FIDO2 services
                // Direct conversion from Instant to Date (no intermediate LocalDateTime needed)
                Date cutoffDate = Date.from(
                    ZonedDateTime.now(ZoneId.of("UTC"))
                        .minusDays(retentionDays)
                        .toInstant()
                );
                
                // Cleanup old metrics entries
                Filter filter = Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, cutoffDate);
                List<Fido2MetricsEntry> entries = persistenceEntryManager.findEntries(
                    METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
                );
                
                for (Fido2MetricsEntry entry : entries) {
                    persistenceEntryManager.remove(entry);
                }
                
                log.info("Cleaned up {} old metrics entries", entries.size());
            } catch (Exception e) {
                log.error("Failed to cleanup old data: {}", e.getMessage(), e);
            }
        });
    }

    // ========== ANALYTICS AND REPORTING ==========

    /**
     * Get user adoption metrics
     */
    public Map<String, Object> getUserAdoptionMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Fido2MetricsEntry> entries = getMetricsEntries(startTime, endTime);
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Total unique users
        Set<String> uniqueUsers = entries.stream()
            .map(Fido2MetricsEntry::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        metrics.put(Fido2MetricsConstants.TOTAL_UNIQUE_USERS, uniqueUsers.size());

        // New users (first registration)
        Set<String> newUsers = entries.stream()
            .filter(e -> Fido2MetricsConstants.REGISTRATION.equals(e.getOperationType()) && Fido2MetricsConstants.SUCCESS.equals(e.getStatus()))
            .map(Fido2MetricsEntry::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        metrics.put(Fido2MetricsConstants.NEW_USERS, newUsers.size());

        // Returning users
        Set<String> returningUsers = new HashSet<>(uniqueUsers);
        returningUsers.removeAll(newUsers);
        metrics.put(Fido2MetricsConstants.RETURNING_USERS, returningUsers.size());

        // Adoption rate
        if (!uniqueUsers.isEmpty()) {
            metrics.put(Fido2MetricsConstants.ADOPTION_RATE, (double) newUsers.size() / uniqueUsers.size());
        }

        return metrics;
    }

    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Fido2MetricsEntry> entries = getMetricsEntries(startTime, endTime);
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Registration performance
        List<Long> registrationDurations = entries.stream()
            .filter(e -> "REGISTRATION".equals(e.getOperationType()) && e.getDurationMs() != null)
            .map(Fido2MetricsEntry::getDurationMs)
            .collect(Collectors.toList());
        
        if (!registrationDurations.isEmpty()) {
            metrics.put("registrationAvgDuration", registrationDurations.stream().mapToLong(Long::longValue).average().orElse(0.0));
            metrics.put("registrationMinDuration", registrationDurations.stream().mapToLong(Long::longValue).min().orElse(0L));
            metrics.put("registrationMaxDuration", registrationDurations.stream().mapToLong(Long::longValue).max().orElse(0L));
        }

        // Authentication performance
        List<Long> authenticationDurations = entries.stream()
            .filter(e -> "AUTHENTICATION".equals(e.getOperationType()) && e.getDurationMs() != null)
            .map(Fido2MetricsEntry::getDurationMs)
            .collect(Collectors.toList());
        
        if (!authenticationDurations.isEmpty()) {
            metrics.put("authenticationAvgDuration", authenticationDurations.stream().mapToLong(Long::longValue).average().orElse(0.0));
            metrics.put("authenticationMinDuration", authenticationDurations.stream().mapToLong(Long::longValue).min().orElse(0L));
            metrics.put("authenticationMaxDuration", authenticationDurations.stream().mapToLong(Long::longValue).max().orElse(0L));
        }

        return metrics;
    }

    /**
     * Get device/platform analytics
     */
    public Map<String, Object> getDeviceAnalytics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Fido2MetricsEntry> entries = getMetricsEntries(startTime, endTime);
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Device types
        Map<String, Long> deviceTypes = entries.stream()
            .filter(e -> e.getDeviceInfo() != null && e.getDeviceInfo().getDeviceType() != null)
            .collect(Collectors.groupingBy(
                e -> e.getDeviceInfo().getDeviceType(),
                Collectors.counting()
            ));
        analytics.put("deviceTypes", deviceTypes);

        // Authenticator types
        Map<String, Long> authenticatorTypes = entries.stream()
            .filter(e -> e.getAuthenticatorType() != null)
            .collect(Collectors.groupingBy(
                Fido2MetricsEntry::getAuthenticatorType,
                Collectors.counting()
            ));
        analytics.put("authenticatorTypes", authenticatorTypes);

        // Browsers
        Map<String, Long> browsers = entries.stream()
            .filter(e -> e.getDeviceInfo() != null && e.getDeviceInfo().getBrowser() != null)
            .collect(Collectors.groupingBy(
                e -> e.getDeviceInfo().getBrowser(),
                Collectors.counting()
            ));
        analytics.put("browsers", browsers);

        // Operating systems
        Map<String, Long> operatingSystems = entries.stream()
            .filter(e -> e.getDeviceInfo() != null && e.getDeviceInfo().getOs() != null)
            .collect(Collectors.groupingBy(
                e -> e.getDeviceInfo().getOs(),
                Collectors.counting()
            ));
        analytics.put("operatingSystems", operatingSystems);

        return analytics;
    }

    /**
     * Get error analysis
     */
    public Map<String, Object> getErrorAnalysis(LocalDateTime startTime, LocalDateTime endTime) {
        List<Fido2MetricsEntry> entries = getMetricsEntries(startTime, endTime);
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Error categories
        Map<String, Long> errorCategories = entries.stream()
            .filter(e -> e.getErrorCategory() != null)
            .collect(Collectors.groupingBy(
                Fido2MetricsEntry::getErrorCategory,
                Collectors.counting()
            ));
        analysis.put("errorCategories", errorCategories);

        // Top errors
        Map<String, Long> topErrors = entries.stream()
            .filter(e -> e.getErrorReason() != null)
            .collect(Collectors.groupingBy(
                Fido2MetricsEntry::getErrorReason,
                Collectors.counting()
            ));
        analysis.put("topErrors", topErrors);

        // Single-pass tally of status counts (ATTEMPT = started, SUCCESS/FAILURE = completed)
        long totalStarted = 0;
        long successfulOperations = 0;
        long failedOperations = 0;
        for (Fido2MetricsEntry e : entries) {
            String status = e.getStatus();
            if (Fido2MetricsConstants.ATTEMPT.equals(status)) {
                totalStarted++;
            } else if (Fido2MetricsConstants.SUCCESS.equals(status)) {
                successfulOperations++;
            } else if (Fido2MetricsConstants.FAILURE.equals(status)) {
                failedOperations++;
            }
        }

        if (totalStarted > 0) {
            // Normal case: rates as proportion of started operations (ATTEMPT count)
            double successRate = (double) successfulOperations / totalStarted;
            double failureRate = (double) failedOperations / totalStarted;
            // When mixed legacy/new data (SUCCESS+FAILURE > ATTEMPT), scale so completionRate = successRate + failureRate and all stay in [0.0, 1.0]
            double rawCompletion = successRate + failureRate;
            if (rawCompletion > 1.0) {
                double scale = 1.0 / rawCompletion;
                successRate *= scale;
                failureRate *= scale;
            }
            double completionRate = successRate + failureRate;
            double dropOffRate = Math.max(0.0, 1.0 - completionRate);

            analysis.put(Fido2MetricsConstants.SUCCESS_RATE, successRate);
            analysis.put(Fido2MetricsConstants.FAILURE_RATE, failureRate);
            analysis.put(Fido2MetricsConstants.COMPLETION_RATE, completionRate);
            analysis.put(Fido2MetricsConstants.DROP_OFF_RATE, dropOffRate);
        } else {
            // Fallback when no ATTEMPT entries (e.g. legacy data): use completed-only denominator
            long totalCompleted = successfulOperations + failedOperations;
            if (totalCompleted > 0) {
                double successRate = (double) successfulOperations / totalCompleted;
                double failureRate = (double) failedOperations / totalCompleted;
                analysis.put(Fido2MetricsConstants.SUCCESS_RATE, successRate);
                analysis.put(Fido2MetricsConstants.FAILURE_RATE, failureRate);
                analysis.put(Fido2MetricsConstants.COMPLETION_RATE, 1.0);
                analysis.put(Fido2MetricsConstants.DROP_OFF_RATE, 0.0);
            } else {
                // Empty dataset: emit rate keys with defaults so response shape is stable for clients
                analysis.put(Fido2MetricsConstants.SUCCESS_RATE, 0.0);
                analysis.put(Fido2MetricsConstants.FAILURE_RATE, 0.0);
                analysis.put(Fido2MetricsConstants.COMPLETION_RATE, 0.0);
                analysis.put(Fido2MetricsConstants.DROP_OFF_RATE, 0.0);
            }
        }

        return analysis;
    }

    /**
     * Calculate aggregation for a specific time period
     */
    private Fido2MetricsAggregation calculateAggregation(String aggregationType, String period, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Get all entries for the time period
            List<Fido2MetricsEntry> entries = getMetricsEntriesByTimeRange(startTime, endTime);
            
            if (entries.isEmpty()) {
                return null;
            }

            // Convert LocalDateTime to Date for ORM persistence (reuse convertToDate for consistency)
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            Fido2MetricsAggregation aggregation = new Fido2MetricsAggregation(aggregationType, period, startDate, endDate);
            Map<String, Object> metricsData = new HashMap<>();

            // Calculate registration metrics
            long registrationAttempts = entries.stream()
                .filter(e -> Fido2MetricsConstants.REGISTRATION.equals(e.getOperationType()))
                .count();
            
            long registrationSuccesses = entries.stream()
                .filter(e -> Fido2MetricsConstants.REGISTRATION.equals(e.getOperationType()) && 
                           Fido2MetricsConstants.SUCCESS.equals(e.getStatus()))
                .count();
            
            long registrationFailures = registrationAttempts - registrationSuccesses;
            
            metricsData.put(Fido2MetricsConstants.REGISTRATION_ATTEMPTS, registrationAttempts);
            metricsData.put(Fido2MetricsConstants.REGISTRATION_SUCCESSES, registrationSuccesses);
            metricsData.put(Fido2MetricsConstants.REGISTRATION_FAILURES, registrationFailures);
            
            if (registrationAttempts > 0) {
                metricsData.put(Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE, (double) registrationSuccesses / registrationAttempts);
            }

            // Calculate authentication metrics
            long authenticationAttempts = entries.stream()
                .filter(e -> Fido2MetricsConstants.AUTHENTICATION.equals(e.getOperationType()))
                .count();
            
            long authenticationSuccesses = entries.stream()
                .filter(e -> Fido2MetricsConstants.AUTHENTICATION.equals(e.getOperationType()) && 
                           Fido2MetricsConstants.SUCCESS.equals(e.getStatus()))
                .count();
            
            long authenticationFailures = authenticationAttempts - authenticationSuccesses;
            
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS, authenticationAttempts);
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_SUCCESSES, authenticationSuccesses);
            metricsData.put(Fido2MetricsConstants.AUTHENTICATION_FAILURES, authenticationFailures);
            
            if (authenticationAttempts > 0) {
                metricsData.put(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE, (double) authenticationSuccesses / authenticationAttempts);
            }

            // Calculate fallback events
            long fallbackEvents = entries.stream()
                .filter(e -> Fido2MetricsConstants.FALLBACK.equals(e.getOperationType()))
                .count();
            metricsData.put(Fido2MetricsConstants.FALLBACK_EVENTS, fallbackEvents);

            // Calculate unique users
            Set<String> uniqueUsers = entries.stream()
                .filter(e -> e.getUserId() != null)
                .map(Fido2MetricsEntry::getUserId)
                .collect(Collectors.toSet());
            aggregation.setUniqueUsers((long) uniqueUsers.size());

            // Calculate device types
            Map<String, Long> deviceTypes = entries.stream()
                .filter(e -> e.getAuthenticatorType() != null)
                .collect(Collectors.groupingBy(
                    Fido2MetricsEntry::getAuthenticatorType,
                    Collectors.counting()
                ));
            metricsData.put(Fido2MetricsConstants.DEVICE_TYPES, deviceTypes);

            // Calculate error counts
            Map<String, Long> errorCounts = entries.stream()
                .filter(e -> e.getErrorReason() != null)
                .collect(Collectors.groupingBy(
                    Fido2MetricsEntry::getErrorReason,
                    Collectors.counting()
                ));
            metricsData.put(Fido2MetricsConstants.ERROR_COUNTS, errorCounts);

            // Calculate average durations
            OptionalDouble avgRegistrationDuration = entries.stream()
                .filter(e -> Fido2MetricsConstants.REGISTRATION.equals(e.getOperationType()) && 
                           e.getDurationMs() != null)
                .mapToLong(Fido2MetricsEntry::getDurationMs)
                .average();
            
            if (avgRegistrationDuration.isPresent()) {
                metricsData.put(Fido2MetricsConstants.REGISTRATION_AVG_DURATION, avgRegistrationDuration.getAsDouble());
            }

            OptionalDouble avgAuthenticationDuration = entries.stream()
                .filter(e -> Fido2MetricsConstants.AUTHENTICATION.equals(e.getOperationType()) && 
                           e.getDurationMs() != null)
                .mapToLong(Fido2MetricsEntry::getDurationMs)
                .average();
            
            if (avgAuthenticationDuration.isPresent()) {
                metricsData.put(Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION, avgAuthenticationDuration.getAsDouble());
            }

            aggregation.setMetricsData(metricsData);
            return aggregation;

        } catch (Exception e) {
            log.error("Failed to calculate aggregation for period {}: {}", period, e.getMessage(), e);
            return null;
        }
    }

    private List<Fido2MetricsEntry> getMetricsEntriesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // Convert LocalDateTime to Date for SQL persistence filters
            Date startDate = convertToDate(startTime);
            Date endDate = convertToDate(endTime);
            
            Filter filter = Filter.createANDFilter(
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startDate),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endDate)
            );

            return persistenceEntryManager.findEntries(
                METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve metrics entries for time range: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Convert LocalDateTime to Date for persistence layer compatibility
     * 
     * IMPORTANT: This method assumes the input LocalDateTime is already in UTC timezone.
     * The conversion applies UTC timezone without validation, so callers must ensure
     * they pass UTC-aligned LocalDateTime values.
     * 
     * @param dateTime LocalDateTime value that must be in UTC (not validated)
     * @return Date object representing the same instant in UTC
     */
    private Date convertToDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    // ========== HELPER METHODS ==========

    private boolean isFido2MetricsEnabled() {
        return appConfiguration.isFido2MetricsEnabled();
    }

    private String generateMetricsEntryDn(String id) {
        return String.format("jansId=%s,%s", id, METRICS_ENTRY_BASE_DN);
    }

    private String generateAggregationDn(String id) {
        return String.format("jansId=%s,%s", id, METRICS_AGGREGATION_BASE_DN);
    }

    private Fido2MetricsEntry convertToMetricsEntry(Fido2MetricsData metricsData) {
        Fido2MetricsEntry entry = new Fido2MetricsEntry();
        entry.setId(UUID.randomUUID().toString());
        
        // Convert LocalDateTime to Date for ORM compatibility (already in UTC)
        if (metricsData.getTimestamp() != null) {
            entry.setTimestamp(convertToDate(metricsData.getTimestamp()));
        }
        
        // Essential fields - always set
        setEssentialFields(entry, metricsData);
        
        // Optional fields - only set if available
        setOptionalFields(entry, metricsData);
        
        // Device info - only set if available and non-empty
        setDeviceInfo(entry, metricsData);
        
        return entry;
    }
    
    /**
     * Set essential fields that are always present
     */
    private void setEssentialFields(Fido2MetricsEntry entry, Fido2MetricsData metricsData) {
        entry.setUserId(metricsData.getUserId());
        entry.setUsername(metricsData.getUsername());
        entry.setOperationType(metricsData.getOperationType());
        entry.setStatus(metricsData.getOperationStatus());
    }
    
    /**
     * Set optional fields that may be null or empty
     */
    private void setOptionalFields(Fido2MetricsEntry entry, Fido2MetricsData metricsData) {
        // Performance metrics
        if (metricsData.getDurationMs() != null) {
            entry.setDurationMs(metricsData.getDurationMs());
        }
        
        // Authenticator info
        setIfNotEmpty(metricsData.getAuthenticatorType(), entry::setAuthenticatorType);
        
        // Error info
        setIfNotEmpty(metricsData.getErrorReason(), entry::setErrorReason);
        setIfNotEmpty(metricsData.getErrorCategory(), entry::setErrorCategory);
        
        // Fallback info
        setIfNotEmpty(metricsData.getFallbackMethod(), entry::setFallbackMethod);
        setIfNotEmpty(metricsData.getFallbackReason(), entry::setFallbackReason);
        
        // Network info
        setIfNotEmpty(metricsData.getIpAddress(), entry::setIpAddress);
        setIfNotEmpty(metricsData.getUserAgent(), entry::setUserAgent);
        
        // Session info
        setIfNotEmpty(metricsData.getSessionId(), entry::setSessionId);
        
        // Cluster info
        setIfNotEmpty(metricsData.getNodeId(), entry::setNodeId);
    }
    
    /**
     * Set field value if string is not null and not empty
     */
    private void setIfNotEmpty(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value);
        }
    }
    
    /**
     * Set device info if available and non-empty
     */
    private void setDeviceInfo(Fido2MetricsEntry entry, Fido2MetricsData metricsData) {
        if (metricsData.getDeviceInfo() == null) {
            return;
        }
        
        Fido2MetricsEntry.DeviceInfo deviceInfo = new Fido2MetricsEntry.DeviceInfo();
        boolean hasDeviceInfo = false;
        
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getBrowser(), deviceInfo::setBrowser);
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getBrowserVersion(), deviceInfo::setBrowserVersion);
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getOperatingSystem(), deviceInfo::setOs);
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getOsVersion(), deviceInfo::setOsVersion);
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getDeviceType(), deviceInfo::setDeviceType);
        hasDeviceInfo |= setDeviceField(metricsData.getDeviceInfo().getUserAgent(), deviceInfo::setUserAgent);
        
        if (hasDeviceInfo) {
            entry.setDeviceInfo(deviceInfo);
        }
    }
    
    /**
     * Set device field if value is not null and not empty
     * @return true if field was set, false otherwise
     */
    private boolean setDeviceField(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value);
            return true;
        }
        return false;
    }

    // ========== TREND ANALYSIS METHODS (GitHub Issue #4) ==========

    /**
     * Get trend analysis for a specific aggregation type and time period
     */
    public Map<String, Object> getTrendAnalysis(String aggregationType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Fido2MetricsAggregation> aggregations = getAggregations(aggregationType, startTime, endTime);
            
            if (aggregations.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, Object> trendAnalysis = new HashMap<>();
            
            // Extract data points for trend calculation
            List<Map<String, Object>> dataPoints = new ArrayList<>();
            for (Fido2MetricsAggregation agg : aggregations) {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", agg.getStartTime());
                point.put("period", agg.getPeriod());
                point.put("metrics", agg.getMetricsData());
                dataPoints.add(point);
            }
            trendAnalysis.put("dataPoints", dataPoints);
            
            // Calculate growth rate
            double growthRate = calculateGrowthRate(aggregations);
            trendAnalysis.put("growthRate", growthRate);
            
            // Determine trend direction
            String trendDirection = determineTrendDirection(aggregations);
            trendAnalysis.put("trendDirection", trendDirection);
            
            // Generate insights
            Map<String, Object> insights = generateTrendInsights(aggregations);
            trendAnalysis.put("insights", insights);
            
            return trendAnalysis;
            
        } catch (Exception e) {
            log.error("Failed to calculate trend analysis: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get period-over-period comparison
     */
    public Map<String, Object> getPeriodOverPeriodComparison(String aggregationType, int periods) {
        try {
            java.time.temporal.ChronoUnit chronoUnit = getChronoUnitForAggregationType(aggregationType);
            
            // Use UTC timezone to align with FIDO2 services
            LocalDateTime endTime = alignEndToBoundary(
                ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime(), 
                chronoUnit
            );
            LocalDateTime startTime = endTime.minus(periods, chronoUnit);

            List<Fido2MetricsAggregation> currentPeriod = getAggregations(aggregationType, startTime, endTime);
            
            // Get previous period for comparison
            LocalDateTime previousEndTime = startTime;
            LocalDateTime previousStartTime = previousEndTime.minus(periods, chronoUnit);
            
            List<Fido2MetricsAggregation> previousPeriod = getAggregations(aggregationType, previousStartTime, previousEndTime);

            Map<String, Object> comparison = new HashMap<>();
            comparison.put(Fido2MetricsConstants.CURRENT_PERIOD, calculatePeriodSummary(currentPeriod));
            comparison.put(Fido2MetricsConstants.PREVIOUS_PERIOD, calculatePeriodSummary(previousPeriod));
            comparison.put(Fido2MetricsConstants.COMPARISON, calculatePeriodComparison(currentPeriod, previousPeriod));
            
            return comparison;
            
        } catch (Exception e) {
            log.error("Failed to calculate period-over-period comparison: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get aggregation summary for a time period
     */
    public Map<String, Object> getAggregationSummary(String aggregationType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Fido2MetricsAggregation> aggregations = getAggregations(aggregationType, startTime, endTime);
            return calculatePeriodSummary(aggregations);
        } catch (Exception e) {
            log.error("Failed to get aggregation summary: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    // ========== PRIVATE HELPER METHODS FOR TREND ANALYSIS ==========

    /**
     * Align the end time to the start of the current period boundary
     * This prevents partial/short windows in Period-over-Period comparisons
     */
    private LocalDateTime alignEndToBoundary(LocalDateTime ref, java.time.temporal.ChronoUnit unit) {
        if (unit == java.time.temporal.ChronoUnit.HOURS) {
            return ref.withMinute(0).withSecond(0).withNano(0);
        } else if (unit == java.time.temporal.ChronoUnit.DAYS) {
            return ref.withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if (unit == java.time.temporal.ChronoUnit.WEEKS) {
            return ref.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else { // MONTHS
            return ref.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    private java.time.temporal.ChronoUnit getChronoUnitForAggregationType(String aggregationType) {
        if (Fido2MetricsConstants.HOURLY.equals(aggregationType)) {
            return java.time.temporal.ChronoUnit.HOURS;
        } else if (Fido2MetricsConstants.DAILY.equals(aggregationType)) {
            return java.time.temporal.ChronoUnit.DAYS;
        } else if (Fido2MetricsConstants.WEEKLY.equals(aggregationType)) {
            return java.time.temporal.ChronoUnit.WEEKS;
        } else if (Fido2MetricsConstants.MONTHLY.equals(aggregationType)) {
            return java.time.temporal.ChronoUnit.MONTHS;
        } else {
            // Default to DAYS if unknown
            return java.time.temporal.ChronoUnit.DAYS;
        }
    }

    private double calculateGrowthRate(List<Fido2MetricsAggregation> aggregations) {
        if (aggregations.size() < 2) {
            return 0.0;
        }
        
        // Sort by start time
        aggregations.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        
        Fido2MetricsAggregation first = aggregations.get(0);
        Fido2MetricsAggregation last = aggregations.get(aggregations.size() - 1);
        
        Double firstValue = getTotalOperations(first);
        Double lastValue = getTotalOperations(last);
        
        if (firstValue == null || lastValue == null || firstValue == 0) {
            return 0.0;
        }
        
        return ((lastValue - firstValue) / firstValue) * 100.0;
    }

    private String determineTrendDirection(List<Fido2MetricsAggregation> aggregations) {
        double growthRate = calculateGrowthRate(aggregations);
        
        if (growthRate > 5.0) {
            return "INCREASING";
        } else if (growthRate < -5.0) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }

    private Map<String, Object> generateTrendInsights(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> insights = new HashMap<>();
        
        if (aggregations.isEmpty()) {
            return insights;
        }
        
        // Calculate average metrics across all aggregations
        double avgRegistrationSuccessRate = aggregations.stream()
            .mapToDouble(agg -> {
                Double val = getRegistrationSuccessRate(agg);
                return val != null ? val : 0.0;
            })
            .average()
            .orElse(0.0);
        insights.put("avgRegistrationSuccessRate", avgRegistrationSuccessRate);
        
        double avgAuthenticationSuccessRate = aggregations.stream()
            .mapToDouble(agg -> {
                Double val = getAuthenticationSuccessRate(agg);
                return val != null ? val : 0.0;
            })
            .average()
            .orElse(0.0);
        insights.put("avgAuthenticationSuccessRate", avgAuthenticationSuccessRate);
        
        // Peak usage detection
        Fido2MetricsAggregation peakAggregation = aggregations.stream()
            .max((a, b) -> Double.compare(getTotalOperations(a), getTotalOperations(b)))
            .orElse(null);
        
        if (peakAggregation != null) {
            insights.put("peakUsage", Map.of(
                "period", peakAggregation.getPeriod(),
                Fido2MetricsConstants.TOTAL_OPERATIONS, getTotalOperations(peakAggregation)
            ));
        }
        
        return insights;
    }

    private Map<String, Object> calculatePeriodSummary(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> summary = new HashMap<>();
        
        if (aggregations.isEmpty()) {
            return summary;
        }
        
        // Calculate totals
        long totalRegistrations = aggregations.stream()
            .mapToLong(agg -> {
                Long val = getRegistrationAttempts(agg);
                return val != null ? val : 0L;
            })
            .sum();
        long totalAuthentications = aggregations.stream()
            .mapToLong(agg -> {
                Long val = getAuthenticationAttempts(agg);
                return val != null ? val : 0L;
            })
            .sum();
        long totalFallbacks = aggregations.stream()
            .mapToLong(agg -> {
                Long val = getFallbackEvents(agg);
                return val != null ? val : 0L;
            })
            .sum();
        
        summary.put("totalRegistrations", totalRegistrations);
        summary.put("totalAuthentications", totalAuthentications);
        summary.put("totalFallbacks", totalFallbacks);
        summary.put(Fido2MetricsConstants.TOTAL_OPERATIONS, totalRegistrations + totalAuthentications);
        
        // Calculate averages
        double avgRegistrationSuccessRate = aggregations.stream()
            .mapToDouble(agg -> {
                Double val = getRegistrationSuccessRate(agg);
                return val != null ? val : 0.0;
            })
            .average()
            .orElse(0.0);
        double avgAuthenticationSuccessRate = aggregations.stream()
            .mapToDouble(agg -> {
                Double val = getAuthenticationSuccessRate(agg);
                return val != null ? val : 0.0;
            })
            .average()
            .orElse(0.0);
        
        summary.put("avgRegistrationSuccessRate", avgRegistrationSuccessRate);
        summary.put("avgAuthenticationSuccessRate", avgAuthenticationSuccessRate);
        
        return summary;
    }

    private Map<String, Object> calculatePeriodComparison(List<Fido2MetricsAggregation> current, List<Fido2MetricsAggregation> previous) {
        Map<String, Object> comparison = new HashMap<>();
        
        Map<String, Object> currentSummary = calculatePeriodSummary(current);
        Map<String, Object> previousSummary = calculatePeriodSummary(previous);
        
        // Calculate percentage changes
        long currentTotal = (Long) currentSummary.getOrDefault(Fido2MetricsConstants.TOTAL_OPERATIONS, 0L);
        long previousTotal = (Long) previousSummary.getOrDefault(Fido2MetricsConstants.TOTAL_OPERATIONS, 0L);
        
        if (previousTotal > 0) {
            double changePercent = ((double) (currentTotal - previousTotal) / previousTotal) * 100.0;
            comparison.put("totalOperationsChange", changePercent);
        } else {
            comparison.put("totalOperationsChange", currentTotal > 0 ? 100.0 : 0.0);
        }
        
        return comparison;
    }

    // Helper methods to extract values from aggregations
    private Double getTotalOperations(Fido2MetricsAggregation aggregation) {
        if (aggregation.getMetricsData() == null) {
            return 0.0;
        }
        
        Long registrations = aggregation.getLongMetric(Fido2MetricsConstants.REGISTRATION_ATTEMPTS);
        Long authentications = aggregation.getLongMetric(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS);
        
        long regCount = registrations != null ? registrations : 0L;
        long authCount = authentications != null ? authentications : 0L;
        
        return (double) (regCount + authCount);
    }

    private Long getRegistrationAttempts(Fido2MetricsAggregation aggregation) {
        return aggregation.getLongMetric(Fido2MetricsConstants.REGISTRATION_ATTEMPTS);
    }

    private Long getAuthenticationAttempts(Fido2MetricsAggregation aggregation) {
        return aggregation.getLongMetric(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS);
    }

    private Long getFallbackEvents(Fido2MetricsAggregation aggregation) {
        return aggregation.getLongMetric(Fido2MetricsConstants.FALLBACK_EVENTS);
    }

    private Double getRegistrationSuccessRate(Fido2MetricsAggregation aggregation) {
        return aggregation.getDoubleMetric(Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE);
    }

    private Double getAuthenticationSuccessRate(Fido2MetricsAggregation aggregation) {
        return aggregation.getDoubleMetric(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE);
    }

}
