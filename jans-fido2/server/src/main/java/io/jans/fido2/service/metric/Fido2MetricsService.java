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
            Filter filter = Filter.createANDFilter(
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startTime),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endTime)
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
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansUserId", userId),
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startTime),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endTime)
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
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansOperationType", operationType),
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startTime),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endTime)
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
                    "DAILY", 
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
                    "WEEKLY", 
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
     */
    public List<Fido2MetricsAggregation> getAggregations(String aggregationType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Filter filter = Filter.createANDFilter(
                Filter.createEqualityFilter("jansAggregationType", aggregationType),
                Filter.createGreaterOrEqualFilter("jansStartTime", startTime),
                Filter.createLessOrEqualFilter("jansEndTime", endTime)
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
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
                
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

        // Success/failure rates
        long totalOperations = entries.size();
        long successfulOperations = entries.stream()
            .filter(e -> Fido2MetricsConstants.SUCCESS.equals(e.getStatus()))
            .count();
        
        if (totalOperations > 0) {
            analysis.put("successRate", (double) successfulOperations / totalOperations);
            analysis.put("failureRate", (double) (totalOperations - successfulOperations) / totalOperations);
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

            Fido2MetricsAggregation aggregation = new Fido2MetricsAggregation(aggregationType, period, startTime, endTime);
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
            Filter filter = Filter.createANDFilter(
                Filter.createGreaterOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, startTime),
                Filter.createLessOrEqualFilter(Fido2MetricsConstants.JANS_TIMESTAMP, endTime)
            );

            return persistenceEntryManager.findEntries(
                METRICS_ENTRY_BASE_DN, Fido2MetricsEntry.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve metrics entries for time range: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
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
        entry.setMetricType(metricsData.getMetricType());
        entry.setTimestamp(metricsData.getTimestamp());
        entry.setUserId(metricsData.getUserId());
        entry.setUsername(metricsData.getUsername());
        entry.setOperationType(metricsData.getOperationType());
        entry.setStatus(metricsData.getOperationStatus());
        entry.setDurationMs(metricsData.getDurationMs());
        entry.setAuthenticatorType(metricsData.getAuthenticatorType());
        entry.setErrorReason(metricsData.getErrorReason());
        entry.setErrorCategory(metricsData.getErrorCategory());
        entry.setFallbackMethod(metricsData.getFallbackMethod());
        entry.setFallbackReason(metricsData.getFallbackReason());
        entry.setSessionId(metricsData.getSessionId());
        entry.setIpAddress(metricsData.getIpAddress());
        entry.setUserAgent(metricsData.getUserAgent());
        entry.setNodeId(metricsData.getNodeId());
        entry.setApplicationType(metricsData.getApplicationType());
        
        // Convert device info
        if (metricsData.getDeviceInfo() != null) {
            Fido2MetricsEntry.DeviceInfo deviceInfo = new Fido2MetricsEntry.DeviceInfo();
            deviceInfo.setBrowser(metricsData.getDeviceInfo().getBrowser());
            deviceInfo.setBrowserVersion(metricsData.getDeviceInfo().getBrowserVersion());
            deviceInfo.setOs(metricsData.getDeviceInfo().getOperatingSystem());
            deviceInfo.setOsVersion(metricsData.getDeviceInfo().getOsVersion());
            deviceInfo.setDeviceType(metricsData.getDeviceInfo().getDeviceType());
            deviceInfo.setUserAgent(metricsData.getDeviceInfo().getUserAgent());
            entry.setDeviceInfo(deviceInfo);
        }
        
        return entry;
    }

}
