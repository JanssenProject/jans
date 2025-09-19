/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * FIDO2 Metrics Entry - stores individual metric events
 * 
 * @author FIDO2 Team
 */
@DataEntry
@ObjectClass("jansFido2MetricsEntry")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2MetricsEntry extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansMetricType")
    private String metricType;

    @AttributeName(name = "jansTimestamp")
    private LocalDateTime timestamp;

    @AttributeName(name = "jansUserId")
    private String userId;

    @AttributeName(name = "jansUsername")
    private String username;

    @AttributeName(name = "jansOperationType")
    private String operationType; // REGISTRATION, AUTHENTICATION, FALLBACK

    @AttributeName(name = "jansStatus")
    private String status; // SUCCESS, FAILURE, ATTEMPT

    @AttributeName(name = "jansDurationMs")
    private Long durationMs;

    @AttributeName(name = "jansAuthenticatorType")
    private String authenticatorType; // PLATFORM, CROSS_PLATFORM, SECURITY_KEY

    @AttributeName(name = "jansDeviceInfo")
    @JsonObject
    private DeviceInfo deviceInfo;

    @AttributeName(name = "jansErrorReason")
    private String errorReason;

    @AttributeName(name = "jansErrorCategory")
    private String errorCategory;

    @AttributeName(name = "jansFallbackMethod")
    private String fallbackMethod;

    @AttributeName(name = "jansFallbackReason")
    private String fallbackReason;

    @AttributeName(name = "jansUserAgent")
    private String userAgent;

    @AttributeName(name = "jansIpAddress")
    private String ipAddress;

    @AttributeName(name = "jansSessionId")
    private String sessionId;

    @AttributeName(name = "jansAdditionalData")
    @JsonObject
    private transient Map<String, Object> additionalData;

    @AttributeName(name = "jansNodeId")
    private String nodeId;

    @AttributeName(name = "jansApplicationType")
    private String applicationType;

    // Constructors
    public Fido2MetricsEntry() {
        // Default constructor required for ORM
    }

    public Fido2MetricsEntry(String metricType, String operationType, String status) {
        this.metricType = metricType;
        this.operationType = operationType;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getAuthenticatorType() {
        return authenticatorType;
    }

    public void setAuthenticatorType(String authenticatorType) {
        this.authenticatorType = authenticatorType;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
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

    @Override
    public String toString() {
        return "Fido2MetricsEntry{" +
                "id='" + id + '\'' +
                ", metricType='" + metricType + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", operationType='" + operationType + '\'' +
                ", status='" + status + '\'' +
                ", durationMs=" + durationMs +
                ", authenticatorType='" + authenticatorType + '\'' +
                ", errorReason='" + errorReason + '\'' +
                ", errorCategory='" + errorCategory + '\'' +
                ", fallbackMethod='" + fallbackMethod + '\'' +
                ", fallbackReason='" + fallbackReason + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", applicationType='" + applicationType + '\'' +
                '}';
    }

    /**
     * Device information extracted from User-Agent
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("browser")
        private String browser;

        @JsonProperty("browser_version")
        private String browserVersion;

        @JsonProperty("os")
        private String os;

        @JsonProperty("os_version")
        private String osVersion;

        @JsonProperty("device_type")
        private String deviceType; // MOBILE, TABLET, DESKTOP

        @JsonProperty("platform")
        private String platform; // WINDOWS, MACOS, LINUX, ANDROID, IOS

        @JsonProperty("user_agent")
        private String userAgent;

        // Constructors
        public DeviceInfo() {
            // Default constructor required for JSON serialization/deserialization
        }

        public DeviceInfo(String browser, String os, String deviceType) {
            this.browser = browser;
            this.os = os;
            this.deviceType = deviceType;
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

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
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

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public String toString() {
            return "DeviceInfo{" +
                    "browser='" + browser + '\'' +
                    ", browserVersion='" + browserVersion + '\'' +
                    ", os='" + os + '\'' +
                    ", osVersion='" + osVersion + '\'' +
                    ", deviceType='" + deviceType + '\'' +
                    ", platform='" + platform + '\'' +
                    '}';
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Fido2MetricsEntry that = (Fido2MetricsEntry) obj;
        return Objects.equals(id, that.id) &&
               Objects.equals(metricType, that.metricType) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(operationType, that.operationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, metricType, timestamp, userId, operationType);
    }
}

