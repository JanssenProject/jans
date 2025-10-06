/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2UserMetrics;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.fido2.model.metric.UserMetricsUpdateRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing FIDO2 user-level metrics
 * 
 * @author FIDO2 Team
 */
@ApplicationScoped
@Named("fido2UserMetricsService")
public class Fido2UserMetricsService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private PersistenceEntryManager persistenceEntryManager;

    private static final ResourceBundle METRICS_CONFIG = ResourceBundle.getBundle("fido2-metrics");
    private static final String USER_METRICS_BASE_DN = METRICS_CONFIG.getString("fido2.user.metrics.base.dn");
    private static final double HIGH_FALLBACK_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.high.fallback.rate.threshold"));
    private static final double MEDIUM_FALLBACK_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.medium.fallback.rate.threshold"));
    private static final double HIGH_FALLBACK_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.high.fallback.factor"));
    private static final double MEDIUM_FALLBACK_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.medium.fallback.factor"));
    private static final double LOW_SUCCESS_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.success.rate.low.threshold"));
    private static final double LOW_SUCCESS_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.low.success.factor"));
    private static final double MEDIUM_SUCCESS_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.success.rate.medium.threshold"));
    private static final double MEDIUM_SUCCESS_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.medium.success.factor"));
    private static final double NEW_USER_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.new.user.factor"));
    private static final double INACTIVE_USER_RISK_FACTOR = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.inactive.user.factor"));
    private static final double MAX_RISK_SCORE = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.risk.max.score"));

    /**
     * Update user metrics for a registration event
     */
    public void updateUserRegistrationMetrics(UserMetricsUpdateRequest request) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Fido2UserMetrics userMetrics = getUserMetrics(request.getUserId());
                if (userMetrics == null) {
                    userMetrics = new Fido2UserMetrics(request.getUserId(), request.getUsername());
                    userMetrics.setFirstRegistrationDate(LocalDateTime.now());
                }

                userMetrics.incrementRegistrations(request.isSuccess());
                userMetrics.setLastIpAddress(request.getIpAddress());
                userMetrics.setLastUserAgent(request.getUserAgent());

                if (request.isSuccess()) {
                    updatePreferredValues(userMetrics, request.getAuthenticatorType(), request.getDeviceType(), request.getBrowser(), request.getOs());
                    updateAverageDuration(userMetrics, request.getDurationMs(), true);
                }

                userMetrics.updateEngagementLevel();
                userMetrics.updateAdoptionStage();
                saveUserMetrics(userMetrics);

                log.debug("Updated user registration metrics for user: {}", request.getUserId());
            } catch (Exception e) {
                log.error("Failed to update user registration metrics for user {}: {}", request.getUserId(), e.getMessage(), e);
            }
        });
    }

    /**
     * Update user metrics for an authentication event
     */
    public void updateUserAuthenticationMetrics(UserMetricsUpdateRequest request) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Fido2UserMetrics userMetrics = getUserMetrics(request.getUserId());
                if (userMetrics == null) {
                    userMetrics = new Fido2UserMetrics(request.getUserId(), request.getUsername());
                }

                userMetrics.incrementAuthentications(request.isSuccess());
                userMetrics.setLastIpAddress(request.getIpAddress());
                userMetrics.setLastUserAgent(request.getUserAgent());

                if (request.isSuccess()) {
                    updatePreferredValues(userMetrics, request.getAuthenticatorType(), request.getDeviceType(), request.getBrowser(), request.getOs());
                    updateAverageDuration(userMetrics, request.getDurationMs(), false);
                }

                userMetrics.updateEngagementLevel();
                saveUserMetrics(userMetrics);

                log.debug("Updated user authentication metrics for user: {}", request.getUserId());
            } catch (Exception e) {
                log.error("Failed to update user authentication metrics for user {}: {}", request.getUserId(), e.getMessage(), e);
            }
        });
    }

    /**
     * Update user metrics for a fallback event
     */
    public void updateUserFallbackMetrics(String userId, String username, String ipAddress, String userAgent) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Fido2UserMetrics userMetrics = getUserMetrics(userId);
                if (userMetrics == null) {
                    userMetrics = new Fido2UserMetrics(userId, username);
                }

                userMetrics.incrementFallbackEvents();
                userMetrics.setLastIpAddress(ipAddress);
                userMetrics.setLastUserAgent(userAgent);

                saveUserMetrics(userMetrics);

                log.debug("Updated user fallback metrics for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to update user fallback metrics for user {}: {}", userId, e.getMessage(), e);
            }
        });
    }

    /**
     * Get user metrics by user ID
     */
    public Fido2UserMetrics getUserMetrics(String userId) {
        try {
            Filter filter = Filter.createEqualityFilter("jansUserId", userId);
            List<Fido2UserMetrics> entries = persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );

            return entries.isEmpty() ? null : entries.get(0);
        } catch (Exception e) {
            log.error("Failed to retrieve user metrics for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get user metrics by username
     */
    public Fido2UserMetrics getUserMetricsByUsername(String username) {
        try {
            Filter filter = Filter.createEqualityFilter("jansUsername", username);
            List<Fido2UserMetrics> entries = persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );

            return entries.isEmpty() ? null : entries.get(0);
        } catch (Exception e) {
            log.error("Failed to retrieve user metrics for username {}: {}", username, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get all active users
     */
    public List<Fido2UserMetrics> getActiveUsers() {
        try {
            Filter filter = Filter.createEqualityFilter("jansIsActive", true);
            return persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve active users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get users by engagement level
     */
    public List<Fido2UserMetrics> getUsersByEngagementLevel(String engagementLevel) {
        try {
            Filter filter = Filter.createEqualityFilter("jansEngagementLevel", engagementLevel);
            return persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve users by engagement level {}: {}", engagementLevel, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get users by adoption stage
     */
    public List<Fido2UserMetrics> getUsersByAdoptionStage(String adoptionStage) {
        try {
            Filter filter = Filter.createEqualityFilter("jansAdoptionStage", adoptionStage);
            return persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve users by adoption stage {}: {}", adoptionStage, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get new users (registered in the last N days)
     */
    public List<Fido2UserMetrics> getNewUsers(int days) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            Filter filter = Filter.createGreaterOrEqualFilter("jansFirstRegistrationDate", cutoffDate);
            return persistenceEntryManager.findEntries(
                USER_METRICS_BASE_DN, Fido2UserMetrics.class, filter
            );
        } catch (Exception e) {
            log.error("Failed to retrieve new users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get user adoption analytics
     */
    public Map<String, Object> getUserAdoptionAnalytics() {
        List<Fido2UserMetrics> allUsers = getActiveUsers();
        
        Map<String, Object> analytics = new HashMap<>();
        
        // Total users
        analytics.put("totalUsers", allUsers.size());
        
        // Users by adoption stage
        Map<String, Long> adoptionStages = allUsers.stream()
            .filter(u -> u.getAdoptionStage() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getAdoptionStage,
                Collectors.counting()
            ));
        analytics.put("adoptionStages", adoptionStages);
        
        // Users by engagement level
        Map<String, Long> engagementLevels = allUsers.stream()
            .filter(u -> u.getEngagementLevel() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getEngagementLevel,
                Collectors.counting()
            ));
        analytics.put("engagementLevels", engagementLevels);
        
        // Average success rates
        double avgRegistrationSuccessRate = allUsers.stream()
            .mapToDouble(Fido2UserMetrics::getRegistrationSuccessRate)
            .average()
            .orElse(0.0);
        analytics.put("avgRegistrationSuccessRate", avgRegistrationSuccessRate);
        
        double avgAuthenticationSuccessRate = allUsers.stream()
            .mapToDouble(Fido2UserMetrics::getAuthenticationSuccessRate)
            .average()
            .orElse(0.0);
        analytics.put("avgAuthenticationSuccessRate", avgAuthenticationSuccessRate);
        
        // Average fallback rate
        double avgFallbackRate = allUsers.stream()
            .mapToDouble(Fido2UserMetrics::getFallbackRate)
            .average()
            .orElse(0.0);
        analytics.put("avgFallbackRate", avgFallbackRate);
        
        // Preferred authenticator types
        Map<String, Long> preferredAuthenticators = allUsers.stream()
            .filter(u -> u.getPreferredAuthenticatorType() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getPreferredAuthenticatorType,
                Collectors.counting()
            ));
        analytics.put("preferredAuthenticators", preferredAuthenticators);
        
        // Preferred device types
        Map<String, Long> preferredDevices = allUsers.stream()
            .filter(u -> u.getPreferredDeviceType() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getPreferredDeviceType,
                Collectors.counting()
            ));
        analytics.put("preferredDevices", preferredDevices);
        
        return analytics;
    }

    /**
     * Get user behavior patterns
     */
    public Map<String, Object> getUserBehaviorPatterns(String userId) {
        Fido2UserMetrics userMetrics = getUserMetrics(userId);
        if (userMetrics == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> patterns = new HashMap<>();
        
        // Activity patterns
        patterns.put("totalOperations", (userMetrics.getTotalRegistrations() != null ? userMetrics.getTotalRegistrations() : 0) + 
                                       (userMetrics.getTotalAuthentications() != null ? userMetrics.getTotalAuthentications() : 0));
        patterns.put("successRate", userMetrics.getOverallSuccessRate());
        patterns.put("fallbackRate", userMetrics.getFallbackRate());
        patterns.put("engagementLevel", userMetrics.getEngagementLevel());
        patterns.put("adoptionStage", userMetrics.getAdoptionStage());
        
        // Performance patterns
        patterns.put("avgRegistrationDuration", userMetrics.getAvgRegistrationDuration());
        patterns.put("avgAuthenticationDuration", userMetrics.getAvgAuthenticationDuration());
        
        // Preference patterns
        patterns.put("preferredAuthenticator", userMetrics.getPreferredAuthenticatorType());
        patterns.put("preferredDevice", userMetrics.getPreferredDeviceType());
        patterns.put("preferredBrowser", userMetrics.getPreferredBrowser());
        patterns.put("preferredOs", userMetrics.getPreferredOs());
        
        // Temporal patterns
        patterns.put("isNewUser", userMetrics.isNewUser());
        patterns.put("isActiveUser", userMetrics.isActiveUser());
        patterns.put("daysSinceFirstRegistration", userMetrics.getFirstRegistrationDate() != null ? 
            java.time.temporal.ChronoUnit.DAYS.between(userMetrics.getFirstRegistrationDate(), LocalDateTime.now()) : 0);
        patterns.put("daysSinceLastActivity", userMetrics.getLastActivityDate() != null ? 
            java.time.temporal.ChronoUnit.DAYS.between(userMetrics.getLastActivityDate(), LocalDateTime.now()) : 0);
        
        return patterns;
    }

    /**
     * Calculate user risk score
     */
    public double calculateUserRiskScore(String userId) {
        Fido2UserMetrics userMetrics = getUserMetrics(userId);
        if (userMetrics == null) {
            return 0.0;
        }
        
        double riskScore = 0.0;
        
        // High fallback rate increases risk
        double fallbackRate = userMetrics.getFallbackRate();
        if (fallbackRate > HIGH_FALLBACK_RATE_THRESHOLD) {
            riskScore += HIGH_FALLBACK_RISK_FACTOR;
        } else if (fallbackRate > MEDIUM_FALLBACK_RATE_THRESHOLD) {
            riskScore += MEDIUM_FALLBACK_RISK_FACTOR;
        }
        
        // Low success rate increases risk
        double successRate = userMetrics.getOverallSuccessRate();
        if (successRate < LOW_SUCCESS_RATE_THRESHOLD) {
            riskScore += LOW_SUCCESS_RISK_FACTOR;
        } else if (successRate < MEDIUM_SUCCESS_RATE_THRESHOLD) {
            riskScore += MEDIUM_SUCCESS_RISK_FACTOR;
        }
        
        // New users have slightly higher risk
        if (userMetrics.isNewUser()) {
            riskScore += NEW_USER_RISK_FACTOR;
        }
        
        // Inactive users have higher risk
        if (!userMetrics.isActiveUser()) {
            riskScore += INACTIVE_USER_RISK_FACTOR;
        }
        
        return Math.min(riskScore, MAX_RISK_SCORE);
    }

    // Helper methods
    private boolean isFido2MetricsEnabled() {
        return appConfiguration.isFido2MetricsEnabled();
    }

    private void saveUserMetrics(Fido2UserMetrics userMetrics) {
        try {
            if (userMetrics.getDn() == null) {
                userMetrics.setDn(generateUserMetricsDn(userMetrics.getId()));
            }
            persistenceEntryManager.persist(userMetrics);
        } catch (Exception e) {
            log.error("Failed to save user metrics: {}", e.getMessage(), e);
        }
    }

    private String generateUserMetricsDn(String id) {
        return String.format("jansId=%s,%s", id, USER_METRICS_BASE_DN);
    }

    private void updatePreferredValues(Fido2UserMetrics userMetrics, String authenticatorType, 
                                     String deviceType, String browser, String os) {
        // Simple preference tracking - in a real implementation, you might want more sophisticated logic
        if (authenticatorType != null) {
            userMetrics.setPreferredAuthenticatorType(authenticatorType);
        }
        if (deviceType != null) {
            userMetrics.setPreferredDeviceType(deviceType);
        }
        if (browser != null) {
            userMetrics.setPreferredBrowser(browser);
        }
        if (os != null) {
            userMetrics.setPreferredOs(os);
        }
    }

    private void updateAverageDuration(Fido2UserMetrics userMetrics, Long durationMs, boolean isRegistration) {
        if (durationMs == null) {
            return;
        }
        
        if (isRegistration) {
            if (userMetrics.getAvgRegistrationDuration() == null) {
                userMetrics.setAvgRegistrationDuration(durationMs.doubleValue());
            } else {
                // Simple moving average - in production, you might want more sophisticated calculation
                double currentAvg = userMetrics.getAvgRegistrationDuration();
                int count = userMetrics.getSuccessfulRegistrations() != null ? userMetrics.getSuccessfulRegistrations() : 1;
                double newAvg = (currentAvg * (count - 1) + durationMs) / count;
                userMetrics.setAvgRegistrationDuration(newAvg);
            }
        } else {
            if (userMetrics.getAvgAuthenticationDuration() == null) {
                userMetrics.setAvgAuthenticationDuration(durationMs.doubleValue());
            } else {
                double currentAvg = userMetrics.getAvgAuthenticationDuration();
                int count = userMetrics.getSuccessfulAuthentications() != null ? userMetrics.getSuccessfulAuthentications() : 1;
                double newAvg = (currentAvg * (count - 1) + durationMs) / count;
                userMetrics.setAvgAuthenticationDuration(newAvg);
            }
        }
    }
}

