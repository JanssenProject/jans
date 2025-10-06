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
 * FIDO2 User Metrics - tracks individual user behavior and adoption patterns
 * 
 * @author FIDO2 Team
 */
@DataEntry
@ObjectClass("jansFido2UserMetrics")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2UserMetrics extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansUserId")
    private String userId;

    @AttributeName(name = "jansUsername")
    private String username;

    @AttributeName(name = "jansFirstRegistrationDate")
    private LocalDateTime firstRegistrationDate;

    @AttributeName(name = "jansLastActivityDate")
    private LocalDateTime lastActivityDate;

    @AttributeName(name = "jansTotalRegistrations")
    private Integer totalRegistrations;

    @AttributeName(name = "jansTotalAuthentications")
    private Integer totalAuthentications;

    @AttributeName(name = "jansSuccessfulRegistrations")
    private Integer successfulRegistrations;

    @AttributeName(name = "jansSuccessfulAuthentications")
    private Integer successfulAuthentications;

    @AttributeName(name = "jansFailedRegistrations")
    private Integer failedRegistrations;

    @AttributeName(name = "jansFailedAuthentications")
    private Integer failedAuthentications;

    @AttributeName(name = "jansFallbackEvents")
    private Integer fallbackEvents;

    @AttributeName(name = "jansPreferredAuthenticatorType")
    private String preferredAuthenticatorType;

    @AttributeName(name = "jansPreferredDeviceType")
    private String preferredDeviceType;

    @AttributeName(name = "jansPreferredBrowser")
    private String preferredBrowser;

    @AttributeName(name = "jansPreferredOs")
    private String preferredOs;

    @AttributeName(name = "jansAvgRegistrationDuration")
    private Double avgRegistrationDuration;

    @AttributeName(name = "jansAvgAuthenticationDuration")
    private Double avgAuthenticationDuration;

    @AttributeName(name = "jansLastIpAddress")
    private String lastIpAddress;

    @AttributeName(name = "jansLastUserAgent")
    private String lastUserAgent;

    @AttributeName(name = "jansIsActive")
    private Boolean isActive;

    @AttributeName(name = "jansUserSegments")
    @JsonObject
    private transient Map<String, Object> userSegments;

    @AttributeName(name = "jansBehaviorPatterns")
    @JsonObject
    private transient Map<String, Object> behaviorPatterns;

    @AttributeName(name = "jansRiskScore")
    private Double riskScore;

    @AttributeName(name = "jansEngagementLevel")
    private String engagementLevel; // HIGH, MEDIUM, LOW

    @AttributeName(name = "jansAdoptionStage")
    private String adoptionStage; // NEW, LEARNING, ADOPTED, EXPERT

    @AttributeName(name = "jansLastUpdated")
    private LocalDateTime lastUpdated;

    // Constructors
    public Fido2UserMetrics() {
        this.totalRegistrations = 0;
        this.totalAuthentications = 0;
        this.successfulRegistrations = 0;
        this.successfulAuthentications = 0;
        this.failedRegistrations = 0;
        this.failedAuthentications = 0;
        this.fallbackEvents = 0;
        this.isActive = true;
        this.lastUpdated = LocalDateTime.now();
    }

    public Fido2UserMetrics(String userId, String username) {
        this();
        this.userId = userId;
        this.username = username;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public LocalDateTime getFirstRegistrationDate() {
        return firstRegistrationDate;
    }

    public void setFirstRegistrationDate(LocalDateTime firstRegistrationDate) {
        this.firstRegistrationDate = firstRegistrationDate;
    }

    public LocalDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDateTime lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Integer getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(Integer totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public Integer getTotalAuthentications() {
        return totalAuthentications;
    }

    public void setTotalAuthentications(Integer totalAuthentications) {
        this.totalAuthentications = totalAuthentications;
    }

    public Integer getSuccessfulRegistrations() {
        return successfulRegistrations;
    }

    public void setSuccessfulRegistrations(Integer successfulRegistrations) {
        this.successfulRegistrations = successfulRegistrations;
    }

    public Integer getSuccessfulAuthentications() {
        return successfulAuthentications;
    }

    public void setSuccessfulAuthentications(Integer successfulAuthentications) {
        this.successfulAuthentications = successfulAuthentications;
    }

    public Integer getFailedRegistrations() {
        return failedRegistrations;
    }

    public void setFailedRegistrations(Integer failedRegistrations) {
        this.failedRegistrations = failedRegistrations;
    }

    public Integer getFailedAuthentications() {
        return failedAuthentications;
    }

    public void setFailedAuthentications(Integer failedAuthentications) {
        this.failedAuthentications = failedAuthentications;
    }

    public Integer getFallbackEvents() {
        return fallbackEvents;
    }

    public void setFallbackEvents(Integer fallbackEvents) {
        this.fallbackEvents = fallbackEvents;
    }

    public String getPreferredAuthenticatorType() {
        return preferredAuthenticatorType;
    }

    public void setPreferredAuthenticatorType(String preferredAuthenticatorType) {
        this.preferredAuthenticatorType = preferredAuthenticatorType;
    }

    public String getPreferredDeviceType() {
        return preferredDeviceType;
    }

    public void setPreferredDeviceType(String preferredDeviceType) {
        this.preferredDeviceType = preferredDeviceType;
    }

    public String getPreferredBrowser() {
        return preferredBrowser;
    }

    public void setPreferredBrowser(String preferredBrowser) {
        this.preferredBrowser = preferredBrowser;
    }

    public String getPreferredOs() {
        return preferredOs;
    }

    public void setPreferredOs(String preferredOs) {
        this.preferredOs = preferredOs;
    }

    public Double getAvgRegistrationDuration() {
        return avgRegistrationDuration;
    }

    public void setAvgRegistrationDuration(Double avgRegistrationDuration) {
        this.avgRegistrationDuration = avgRegistrationDuration;
    }

    public Double getAvgAuthenticationDuration() {
        return avgAuthenticationDuration;
    }

    public void setAvgAuthenticationDuration(Double avgAuthenticationDuration) {
        this.avgAuthenticationDuration = avgAuthenticationDuration;
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
    }

    public String getLastUserAgent() {
        return lastUserAgent;
    }

    public void setLastUserAgent(String lastUserAgent) {
        this.lastUserAgent = lastUserAgent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Map<String, Object> getUserSegments() {
        return userSegments;
    }

    public void setUserSegments(Map<String, Object> userSegments) {
        this.userSegments = userSegments;
    }

    public Map<String, Object> getBehaviorPatterns() {
        return behaviorPatterns;
    }

    public void setBehaviorPatterns(Map<String, Object> behaviorPatterns) {
        this.behaviorPatterns = behaviorPatterns;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getEngagementLevel() {
        return engagementLevel;
    }

    public void setEngagementLevel(String engagementLevel) {
        this.engagementLevel = engagementLevel;
    }

    public String getAdoptionStage() {
        return adoptionStage;
    }

    public void setAdoptionStage(String adoptionStage) {
        this.adoptionStage = adoptionStage;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Helper methods
    public void incrementRegistrations(boolean success) {
        this.totalRegistrations++;
        if (success) {
            this.successfulRegistrations++;
        } else {
            this.failedRegistrations++;
        }
        this.lastActivityDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public void incrementAuthentications(boolean success) {
        this.totalAuthentications++;
        if (success) {
            this.successfulAuthentications++;
        } else {
            this.failedAuthentications++;
        }
        this.lastActivityDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public void incrementFallbackEvents() {
        this.fallbackEvents++;
        this.lastActivityDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public double getRegistrationSuccessRate() {
        if (totalRegistrations == null || totalRegistrations == 0) {
            return 0.0;
        }
        return (double) successfulRegistrations / totalRegistrations;
    }

    public double getAuthenticationSuccessRate() {
        if (totalAuthentications == null || totalAuthentications == 0) {
            return 0.0;
        }
        return (double) successfulAuthentications / totalAuthentications;
    }

    public double getOverallSuccessRate() {
        int totalOperations = (totalRegistrations != null ? totalRegistrations : 0) + 
                            (totalAuthentications != null ? totalAuthentications : 0);
        int totalSuccesses = (successfulRegistrations != null ? successfulRegistrations : 0) + 
                           (successfulAuthentications != null ? successfulAuthentications : 0);
        
        if (totalOperations == 0) {
            return 0.0;
        }
        return (double) totalSuccesses / totalOperations;
    }

    public double getFallbackRate() {
        int totalOperations = (totalRegistrations != null ? totalRegistrations : 0) + 
                            (totalAuthentications != null ? totalAuthentications : 0);
        
        if (totalOperations == 0) {
            return 0.0;
        }
        return (double) (fallbackEvents != null ? fallbackEvents : 0) / totalOperations;
    }

    public boolean isNewUser() {
        return firstRegistrationDate != null && 
               firstRegistrationDate.isAfter(LocalDateTime.now().minusDays(30));
    }

    public boolean isActiveUser() {
        return lastActivityDate != null && 
               lastActivityDate.isAfter(LocalDateTime.now().minusDays(30));
    }

    public void updateEngagementLevel() {
        int totalOperations = (totalRegistrations != null ? totalRegistrations : 0) + 
                            (totalAuthentications != null ? totalAuthentications : 0);
        
        if (totalOperations >= 50) {
            this.engagementLevel = "HIGH";
        } else if (totalOperations >= 10) {
            this.engagementLevel = "MEDIUM";
        } else {
            this.engagementLevel = "LOW";
        }
    }

    public void updateAdoptionStage() {
        if (totalRegistrations == null || totalRegistrations == 0) {
            this.adoptionStage = "NEW";
        } else if (totalRegistrations == 1) {
            this.adoptionStage = "LEARNING";
        } else if (totalRegistrations <= 5) {
            this.adoptionStage = "ADOPTED";
        } else {
            this.adoptionStage = "EXPERT";
        }
    }

    @Override
    public String toString() {
        return "Fido2UserMetrics{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", firstRegistrationDate=" + firstRegistrationDate +
                ", lastActivityDate=" + lastActivityDate +
                ", totalRegistrations=" + totalRegistrations +
                ", totalAuthentications=" + totalAuthentications +
                ", successfulRegistrations=" + successfulRegistrations +
                ", successfulAuthentications=" + successfulAuthentications +
                ", engagementLevel='" + engagementLevel + '\'' +
                ", adoptionStage='" + adoptionStage + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Fido2UserMetrics that = (Fido2UserMetrics) obj;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(username, that.username) &&
               Objects.equals(firstRegistrationDate, that.firstRegistrationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, firstRegistrationDate);
    }
}

