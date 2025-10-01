/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2024, Janssen Project
 */

package io.jans.fido2.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * FIDO2 metrics data model for storing detailed operation information
 *
 * @author Janssen Project
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2MetricsData implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("operation_type")
    private String operationType; // REGISTRATION, AUTHENTICATION, FALLBACK

    @JsonProperty("operation_status")
    private String operationStatus; // SUCCESS, FAILURE, ATTEMPT

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("device_info")
    private DeviceInfo deviceInfo;

    @JsonProperty("authenticator_type")
    private String authenticatorType; // PLATFORM, CROSS_PLATFORM, SECURITY_KEY

    @JsonProperty("error_reason")
    private String errorReason;

    @JsonProperty("error_category")
    private String errorCategory;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @JsonProperty("fallback_method")
    private String fallbackMethod;

    @JsonProperty("fallback_reason")
    private String fallbackReason;

    @JsonProperty("additional_data")
    private transient Map<String, Object> additionalData;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("application_type")
    private String applicationType;

    @JsonProperty("metric_type")
    private String metricType;

    @JsonProperty("retry_count")
    private Integer retryCount;

    @JsonProperty("concurrent_operations")
    private Integer concurrentOperations;

    @JsonProperty("memory_usage_mb")
    private Long memoryUsageMb;

    @JsonProperty("cpu_usage_percent")
    private Double cpuUsagePercent;

    public Fido2MetricsData() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getAuthenticatorType() {
        return authenticatorType;
    }

    public void setAuthenticatorType(String authenticatorType) {
        this.authenticatorType = authenticatorType;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getFallbackMethod() {
        return fallbackMethod;
    }

    public void setFallbackMethod(String fallbackMethod) {
        this.fallbackMethod = fallbackMethod;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getConcurrentOperations() {
        return concurrentOperations;
    }

    public void setConcurrentOperations(Integer concurrentOperations) {
        this.concurrentOperations = concurrentOperations;
    }

    public Long getMemoryUsageMb() {
        return memoryUsageMb;
    }

    public void setMemoryUsageMb(Long memoryUsageMb) {
        this.memoryUsageMb = memoryUsageMb;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    /**
     * Inner class for device information
     */
    public static class DeviceInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("browser")
        private String browser;

        @JsonProperty("browser_version")
        private String browserVersion;

        @JsonProperty("operating_system")
        private String operatingSystem;

        @JsonProperty("os_version")
        private String osVersion;

        @JsonProperty("device_type")
        private String deviceType; // MOBILE, DESKTOP, TABLET

        @JsonProperty("user_agent")
        private String userAgent;

        public DeviceInfo() {
            // Default constructor required for JSON serialization/deserialization
        }

        // Getters and Setters
        public String getBrowser() {
            return browser;
        }

        public void setBrowser(String browser) {
            this.browser = browser;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
        }

        public String getOperatingSystem() {
            return operatingSystem;
        }

        public void setOperatingSystem(String operatingSystem) {
            this.operatingSystem = operatingSystem;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }
}
