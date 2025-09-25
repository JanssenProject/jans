/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * FIDO2 Metrics Aggregation - stores summary data for different time periods
 * Uses JSON storage for metrics data to provide flexibility and reduce schema complexity
 * 
 * @author FIDO2 Team
 */
@DataEntry
@ObjectClass(value = "jansFido2MetricsAggregation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2MetricsAggregation extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansAggregationType")
    private String aggregationType; // HOURLY, DAILY, WEEKLY, MONTHLY

    @AttributeName(name = "jansStartTime")
    private LocalDateTime startTime;

    @AttributeName(name = "jansEndTime")
    private LocalDateTime endTime;

    @AttributeName(name = "jansUniqueUsers")
    private Long uniqueUsers;

    @AttributeName(name = "jansLastUpdated")
    private LocalDateTime lastUpdated;

    /**
     * All metrics data stored as JSON for flexibility
     * Contains: registrationAttempts, registrationSuccesses, authenticationAttempts, 
     * authenticationSuccesses, deviceTypes, errorCounts, performanceMetrics, etc.
     */
    @AttributeName(name = "jansMetricsData")
    @JsonObject
    private transient Map<String, Object> metricsData;

    // Constructors
    public Fido2MetricsAggregation() {
        this.metricsData = new HashMap<>();
    }

    public Fido2MetricsAggregation(String aggregationType, String period, LocalDateTime startTime, LocalDateTime endTime) {
        this();
        this.aggregationType = aggregationType;
        this.id = aggregationType + "_" + period;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastUpdated = LocalDateTime.now();
    }

    // Core getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getUniqueUsers() {
        return uniqueUsers;
    }

    public void setUniqueUsers(Long uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Object> getMetricsData() {
        return metricsData;
    }

    public void setMetricsData(Map<String, Object> metricsData) {
        this.metricsData = metricsData;
    }

    // Helper methods for common metrics access
    public Long getRegistrationAttempts() {
        return getLongMetric(Fido2MetricsConstants.REGISTRATION_ATTEMPTS);
    }

    public void setRegistrationAttempts(Long value) {
        setMetric(Fido2MetricsConstants.REGISTRATION_ATTEMPTS, value);
    }

    public Long getRegistrationSuccesses() {
        return getLongMetric(Fido2MetricsConstants.REGISTRATION_SUCCESSES);
    }

    public void setRegistrationSuccesses(Long value) {
        setMetric(Fido2MetricsConstants.REGISTRATION_SUCCESSES, value);
    }

    public Long getRegistrationFailures() {
        return getLongMetric(Fido2MetricsConstants.REGISTRATION_FAILURES);
    }

    public void setRegistrationFailures(Long value) {
        setMetric(Fido2MetricsConstants.REGISTRATION_FAILURES, value);
    }

    public Long getAuthenticationAttempts() {
        return getLongMetric(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS);
    }

    public void setAuthenticationAttempts(Long value) {
        setMetric(Fido2MetricsConstants.AUTHENTICATION_ATTEMPTS, value);
    }

    public Long getAuthenticationSuccesses() {
        return getLongMetric(Fido2MetricsConstants.AUTHENTICATION_SUCCESSES);
    }

    public void setAuthenticationSuccesses(Long value) {
        setMetric(Fido2MetricsConstants.AUTHENTICATION_SUCCESSES, value);
    }

    public Long getAuthenticationFailures() {
        return getLongMetric(Fido2MetricsConstants.AUTHENTICATION_FAILURES);
    }

    public void setAuthenticationFailures(Long value) {
        setMetric(Fido2MetricsConstants.AUTHENTICATION_FAILURES, value);
    }

    public Long getFallbackEvents() {
        return getLongMetric(Fido2MetricsConstants.FALLBACK_EVENTS);
    }

    public void setFallbackEvents(Long value) {
        setMetric(Fido2MetricsConstants.FALLBACK_EVENTS, value);
    }

    public Double getRegistrationSuccessRate() {
        return getDoubleMetric(Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE);
    }

    public void setRegistrationSuccessRate(Double value) {
        setMetric(Fido2MetricsConstants.REGISTRATION_SUCCESS_RATE, value);
    }

    public Double getAuthenticationSuccessRate() {
        return getDoubleMetric(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE);
    }

    public void setAuthenticationSuccessRate(Double value) {
        setMetric(Fido2MetricsConstants.AUTHENTICATION_SUCCESS_RATE, value);
    }

    public Double getRegistrationAvgDuration() {
        return getDoubleMetric(Fido2MetricsConstants.REGISTRATION_AVG_DURATION);
    }

    public void setRegistrationAvgDuration(Double value) {
        setMetric(Fido2MetricsConstants.REGISTRATION_AVG_DURATION, value);
    }

    public Double getAuthenticationAvgDuration() {
        return getDoubleMetric(Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION);
    }

    public void setAuthenticationAvgDuration(Double value) {
        setMetric(Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION, value);
    }

    public Map<String, Long> getDeviceTypes() {
        return getMapMetric(Fido2MetricsConstants.DEVICE_TYPES);
    }

    public void setDeviceTypes(Map<String, Long> deviceTypes) {
        setMetric(Fido2MetricsConstants.DEVICE_TYPES, deviceTypes);
    }

    public Map<String, Long> getErrorCounts() {
        return getMapMetric(Fido2MetricsConstants.ERROR_COUNTS);
    }

    public void setErrorCounts(Map<String, Long> errorCounts) {
        setMetric(Fido2MetricsConstants.ERROR_COUNTS, errorCounts);
    }

    // Generic helper methods for metrics access
    private Long getLongMetric(String key) {
        if (metricsData == null) {
            return null;
        }
        Object value = metricsData.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double getDoubleMetric(String key) {
        if (metricsData == null) {
            return null;
        }
        Object value = metricsData.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getMapMetric(String key) {
        if (metricsData == null) {
            return Collections.emptyMap();
        }
        Object value = metricsData.get(key);
        if (value instanceof Map) {
            return (Map<String, Long>) value;
        }
        return Collections.emptyMap();
    }

    private void setMetric(String key, Object value) {
        if (metricsData == null) {
            metricsData = new HashMap<>();
        }
        metricsData.put(key, value);
    }

    // Convenience method to add to existing metric
    public void incrementMetric(String key, Long increment) {
        Long current = getLongMetric(key);
        setMetric(key, (current != null ? current : 0L) + (increment != null ? increment : 0L));
    }

    // Convenience method to set performance metrics
    public void setPerformanceMetrics(Map<String, Object> performanceMetrics) {
        setMetric(Fido2MetricsConstants.PERFORMANCE_METRICS, performanceMetrics);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPerformanceMetrics() {
        if (metricsData == null) {
            return Collections.emptyMap();
        }
        Object value = metricsData.get(Fido2MetricsConstants.PERFORMANCE_METRICS);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Fido2MetricsAggregation that = (Fido2MetricsAggregation) obj;
        return Objects.equals(id, that.id) &&
               Objects.equals(aggregationType, that.aggregationType) &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aggregationType, startTime, endTime);
    }

    @Override
    public String toString() {
        return "Fido2MetricsAggregation{" +
                "id='" + id + '\'' +
                ", aggregationType='" + aggregationType + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", uniqueUsers=" + uniqueUsers +
                ", lastUpdated=" + lastUpdated +
                ", metricsDataSize=" + (metricsData != null ? metricsData.size() : 0) +
                '}';
    }
}