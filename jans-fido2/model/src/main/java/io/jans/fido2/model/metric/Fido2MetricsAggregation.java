/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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
import java.util.Map;
import java.util.Objects;

/**
 * FIDO2 Metrics Aggregation - stores summary data for different time periods
 * 
 * @author FIDO2 Team
 */
@DataEntry
@ObjectClass("jansFido2MetricsAggregation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2MetricsAggregation extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansAggregationType")
    private String aggregationType; // HOURLY, DAILY, WEEKLY, MONTHLY

    @AttributeName(name = "jansTimePeriod")
    private String timePeriod; // 2024-01-15, 2024-W03, 2024-01, etc.

    @AttributeName(name = "jansStartTime")
    private LocalDateTime startTime;

    @AttributeName(name = "jansEndTime")
    private LocalDateTime endTime;

    @AttributeName(name = "jansNodeId")
    private String nodeId;

    @AttributeName(name = "jansApplicationType")
    private String applicationType;

    // Registration Metrics
    @AttributeName(name = "jansRegistrationAttempts")
    private Long registrationAttempts;

    @AttributeName(name = "jansRegistrationSuccesses")
    private Long registrationSuccesses;

    @AttributeName(name = "jansRegistrationFailures")
    private Long registrationFailures;

    @AttributeName(name = "jansRegistrationSuccessRate")
    private Double registrationSuccessRate;

    @AttributeName(name = "jansRegistrationAvgDuration")
    private Double registrationAvgDuration;

    @AttributeName(name = "jansRegistrationMinDuration")
    private Long registrationMinDuration;

    @AttributeName(name = "jansRegistrationMaxDuration")
    private Long registrationMaxDuration;

    // Authentication Metrics
    @AttributeName(name = "jansAuthenticationAttempts")
    private Long authenticationAttempts;

    @AttributeName(name = "jansAuthenticationSuccesses")
    private Long authenticationSuccesses;

    @AttributeName(name = "jansAuthenticationFailures")
    private Long authenticationFailures;

    @AttributeName(name = "jansAuthenticationSuccessRate")
    private Double authenticationSuccessRate;

    @AttributeName(name = "jansAuthenticationAvgDuration")
    private Double authenticationAvgDuration;

    @AttributeName(name = "jansAuthenticationMinDuration")
    private Long authenticationMinDuration;

    @AttributeName(name = "jansAuthenticationMaxDuration")
    private Long authenticationMaxDuration;

    // Fallback Metrics
    @AttributeName(name = "jansFallbackEvents")
    private Long fallbackEvents;

    @AttributeName(name = "jansFallbackRate")
    private Double fallbackRate;

    @AttributeName(name = "jansFallbackMethods")
    @JsonObject
    private Map<String, Long> fallbackMethods; // PASSWORD: 10, SMS: 5, etc.

    // Device/Platform Metrics
    @AttributeName(name = "jansDeviceTypes")
    @JsonObject
    private Map<String, Long> deviceTypes; // MOBILE: 100, DESKTOP: 200, etc.

    @AttributeName(name = "jansAuthenticatorTypes")
    @JsonObject
    private Map<String, Long> authenticatorTypes; // PLATFORM: 150, CROSS_PLATFORM: 50, etc.

    @AttributeName(name = "jansBrowsers")
    @JsonObject
    private Map<String, Long> browsers; // CHROME: 200, FIREFOX: 50, etc.

    @AttributeName(name = "jansOperatingSystems")
    @JsonObject
    private Map<String, Long> operatingSystems; // WINDOWS: 150, MACOS: 100, etc.

    // Error Analysis
    @AttributeName(name = "jansErrorCategories")
    @JsonObject
    private Map<String, Long> errorCategories; // TIMEOUT: 20, INVALID_INPUT: 15, etc.

    @AttributeName(name = "jansTopErrors")
    @JsonObject
    private Map<String, Long> topErrors; // Most common error messages

    // User Metrics
    @AttributeName(name = "jansUniqueUsers")
    private Long uniqueUsers;

    @AttributeName(name = "jansNewUsers")
    private Long newUsers;

    @AttributeName(name = "jansReturningUsers")
    private Long returningUsers;

    @AttributeName(name = "jansUserAdoptionRate")
    private Double userAdoptionRate;

    // Performance Metrics
    @AttributeName(name = "jansPeakConcurrentOperations")
    private Integer peakConcurrentOperations;

    @AttributeName(name = "jansAvgConcurrentOperations")
    private Double avgConcurrentOperations;

    @AttributeName(name = "jansPeakMemoryUsage")
    private Long peakMemoryUsage;

    @AttributeName(name = "jansAvgMemoryUsage")
    private Double avgMemoryUsage;

    @AttributeName(name = "jansPeakCpuUsage")
    private Double peakCpuUsage;

    @AttributeName(name = "jansAvgCpuUsage")
    private Double avgCpuUsage;

    // Geographic/Network Metrics
    @AttributeName(name = "jansTopIpAddresses")
    @JsonObject
    private Map<String, Long> topIpAddresses;

    @AttributeName(name = "jansGeographicDistribution")
    @JsonObject
    private Map<String, Long> geographicDistribution;

    // Additional Analytics
    @AttributeName(name = "jansSessionMetrics")
    @JsonObject
    private transient Map<String, Object> sessionMetrics;

    @AttributeName(name = "jansCustomMetrics")
    @JsonObject
    private transient Map<String, Object> customMetrics;

    @AttributeName(name = "jansLastUpdated")
    private LocalDateTime lastUpdated;

    @AttributeName(name = "jansDataQuality")
    private String dataQuality; // HIGH, MEDIUM, LOW

    @AttributeName(name = "jansCompleteness")
    private Double completeness; // 0.0 to 1.0

    // Constructors
    public Fido2MetricsAggregation() {
        this.lastUpdated = LocalDateTime.now();
    }

    public Fido2MetricsAggregation(String aggregationType, String timePeriod, LocalDateTime startTime, LocalDateTime endTime) {
        this.aggregationType = aggregationType;
        this.timePeriod = timePeriod;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public Long getRegistrationAttempts() {
        return registrationAttempts;
    }

    public void setRegistrationAttempts(Long registrationAttempts) {
        this.registrationAttempts = registrationAttempts;
    }

    public Long getRegistrationSuccesses() {
        return registrationSuccesses;
    }

    public void setRegistrationSuccesses(Long registrationSuccesses) {
        this.registrationSuccesses = registrationSuccesses;
    }

    public Long getRegistrationFailures() {
        return registrationFailures;
    }

    public void setRegistrationFailures(Long registrationFailures) {
        this.registrationFailures = registrationFailures;
    }

    public Double getRegistrationSuccessRate() {
        return registrationSuccessRate;
    }

    public void setRegistrationSuccessRate(Double registrationSuccessRate) {
        this.registrationSuccessRate = registrationSuccessRate;
    }

    public Double getRegistrationAvgDuration() {
        return registrationAvgDuration;
    }

    public void setRegistrationAvgDuration(Double registrationAvgDuration) {
        this.registrationAvgDuration = registrationAvgDuration;
    }

    public Long getRegistrationMinDuration() {
        return registrationMinDuration;
    }

    public void setRegistrationMinDuration(Long registrationMinDuration) {
        this.registrationMinDuration = registrationMinDuration;
    }

    public Long getRegistrationMaxDuration() {
        return registrationMaxDuration;
    }

    public void setRegistrationMaxDuration(Long registrationMaxDuration) {
        this.registrationMaxDuration = registrationMaxDuration;
    }

    public Long getAuthenticationAttempts() {
        return authenticationAttempts;
    }

    public void setAuthenticationAttempts(Long authenticationAttempts) {
        this.authenticationAttempts = authenticationAttempts;
    }

    public Long getAuthenticationSuccesses() {
        return authenticationSuccesses;
    }

    public void setAuthenticationSuccesses(Long authenticationSuccesses) {
        this.authenticationSuccesses = authenticationSuccesses;
    }

    public Long getAuthenticationFailures() {
        return authenticationFailures;
    }

    public void setAuthenticationFailures(Long authenticationFailures) {
        this.authenticationFailures = authenticationFailures;
    }

    public Double getAuthenticationSuccessRate() {
        return authenticationSuccessRate;
    }

    public void setAuthenticationSuccessRate(Double authenticationSuccessRate) {
        this.authenticationSuccessRate = authenticationSuccessRate;
    }

    public Double getAuthenticationAvgDuration() {
        return authenticationAvgDuration;
    }

    public void setAuthenticationAvgDuration(Double authenticationAvgDuration) {
        this.authenticationAvgDuration = authenticationAvgDuration;
    }

    public Long getAuthenticationMinDuration() {
        return authenticationMinDuration;
    }

    public void setAuthenticationMinDuration(Long authenticationMinDuration) {
        this.authenticationMinDuration = authenticationMinDuration;
    }

    public Long getAuthenticationMaxDuration() {
        return authenticationMaxDuration;
    }

    public void setAuthenticationMaxDuration(Long authenticationMaxDuration) {
        this.authenticationMaxDuration = authenticationMaxDuration;
    }

    public Long getFallbackEvents() {
        return fallbackEvents;
    }

    public void setFallbackEvents(Long fallbackEvents) {
        this.fallbackEvents = fallbackEvents;
    }

    public Double getFallbackRate() {
        return fallbackRate;
    }

    public void setFallbackRate(Double fallbackRate) {
        this.fallbackRate = fallbackRate;
    }

    public Map<String, Long> getFallbackMethods() {
        return fallbackMethods;
    }

    public void setFallbackMethods(Map<String, Long> fallbackMethods) {
        this.fallbackMethods = fallbackMethods;
    }

    public Map<String, Long> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(Map<String, Long> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public Map<String, Long> getAuthenticatorTypes() {
        return authenticatorTypes;
    }

    public void setAuthenticatorTypes(Map<String, Long> authenticatorTypes) {
        this.authenticatorTypes = authenticatorTypes;
    }

    public Map<String, Long> getBrowsers() {
        return browsers;
    }

    public void setBrowsers(Map<String, Long> browsers) {
        this.browsers = browsers;
    }

    public Map<String, Long> getOperatingSystems() {
        return operatingSystems;
    }

    public void setOperatingSystems(Map<String, Long> operatingSystems) {
        this.operatingSystems = operatingSystems;
    }

    public Map<String, Long> getErrorCategories() {
        return errorCategories;
    }

    public void setErrorCategories(Map<String, Long> errorCategories) {
        this.errorCategories = errorCategories;
    }

    public Map<String, Long> getTopErrors() {
        return topErrors;
    }

    public void setTopErrors(Map<String, Long> topErrors) {
        this.topErrors = topErrors;
    }

    public Long getUniqueUsers() {
        return uniqueUsers;
    }

    public void setUniqueUsers(Long uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    public Long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(Long newUsers) {
        this.newUsers = newUsers;
    }

    public Long getReturningUsers() {
        return returningUsers;
    }

    public void setReturningUsers(Long returningUsers) {
        this.returningUsers = returningUsers;
    }

    public Double getUserAdoptionRate() {
        return userAdoptionRate;
    }

    public void setUserAdoptionRate(Double userAdoptionRate) {
        this.userAdoptionRate = userAdoptionRate;
    }

    public Integer getPeakConcurrentOperations() {
        return peakConcurrentOperations;
    }

    public void setPeakConcurrentOperations(Integer peakConcurrentOperations) {
        this.peakConcurrentOperations = peakConcurrentOperations;
    }

    public Double getAvgConcurrentOperations() {
        return avgConcurrentOperations;
    }

    public void setAvgConcurrentOperations(Double avgConcurrentOperations) {
        this.avgConcurrentOperations = avgConcurrentOperations;
    }

    public Long getPeakMemoryUsage() {
        return peakMemoryUsage;
    }

    public void setPeakMemoryUsage(Long peakMemoryUsage) {
        this.peakMemoryUsage = peakMemoryUsage;
    }

    public Double getAvgMemoryUsage() {
        return avgMemoryUsage;
    }

    public void setAvgMemoryUsage(Double avgMemoryUsage) {
        this.avgMemoryUsage = avgMemoryUsage;
    }

    public Double getPeakCpuUsage() {
        return peakCpuUsage;
    }

    public void setPeakCpuUsage(Double peakCpuUsage) {
        this.peakCpuUsage = peakCpuUsage;
    }

    public Double getAvgCpuUsage() {
        return avgCpuUsage;
    }

    public void setAvgCpuUsage(Double avgCpuUsage) {
        this.avgCpuUsage = avgCpuUsage;
    }

    public Map<String, Long> getTopIpAddresses() {
        return topIpAddresses;
    }

    public void setTopIpAddresses(Map<String, Long> topIpAddresses) {
        this.topIpAddresses = topIpAddresses;
    }

    public Map<String, Long> getGeographicDistribution() {
        return geographicDistribution;
    }

    public void setGeographicDistribution(Map<String, Long> geographicDistribution) {
        this.geographicDistribution = geographicDistribution;
    }

    public Map<String, Object> getSessionMetrics() {
        return sessionMetrics;
    }

    public void setSessionMetrics(Map<String, Object> sessionMetrics) {
        this.sessionMetrics = sessionMetrics;
    }

    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }

    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public Double getCompleteness() {
        return completeness;
    }

    public void setCompleteness(Double completeness) {
        this.completeness = completeness;
    }

    // Helper methods for calculations
    public void calculateSuccessRates() {
        if (registrationAttempts != null && registrationAttempts > 0) {
            this.registrationSuccessRate = (double) registrationSuccesses / registrationAttempts;
        }
        
        if (authenticationAttempts != null && authenticationAttempts > 0) {
            this.authenticationSuccessRate = (double) authenticationSuccesses / authenticationAttempts;
        }
    }

    public void calculateFallbackRate() {
        long totalAttempts = (registrationAttempts != null ? registrationAttempts : 0) + 
                           (authenticationAttempts != null ? authenticationAttempts : 0);
        
        if (totalAttempts > 0 && fallbackEvents != null) {
            this.fallbackRate = (double) fallbackEvents / totalAttempts;
        }
    }

    public void calculateUserAdoptionRate() {
        if (uniqueUsers != null && uniqueUsers > 0 && newUsers != null) {
            this.userAdoptionRate = (double) newUsers / uniqueUsers;
        }
    }

    @Override
    public String toString() {
        return "Fido2MetricsAggregation{" +
                "id='" + id + '\'' +
                ", aggregationType='" + aggregationType + '\'' +
                ", timePeriod='" + timePeriod + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", registrationAttempts=" + registrationAttempts +
                ", registrationSuccesses=" + registrationSuccesses +
                ", authenticationAttempts=" + authenticationAttempts +
                ", authenticationSuccesses=" + authenticationSuccesses +
                ", fallbackEvents=" + fallbackEvents +
                ", uniqueUsers=" + uniqueUsers +
                ", lastUpdated=" + lastUpdated +
                '}';
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
}

