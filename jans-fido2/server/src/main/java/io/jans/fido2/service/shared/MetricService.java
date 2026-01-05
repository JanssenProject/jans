/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.shared;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsData;
import io.jans.fido2.model.metric.Fido2MetricType;
import io.jans.fido2.model.metric.UserMetricsUpdateRequest;
import io.jans.fido2.service.util.DeviceInfoExtractor;
import io.jans.model.ApplicationType;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.service.net.NetworkService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends io.jans.service.metric.MetricService {
	
	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

	private static final long serialVersionUID = 7875838160379126796L;

	@Inject
    private Instance<MetricService> instance;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
    private StaticConfiguration staticConfiguration;

	@Inject
    private NetworkService networkService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
    @ReportMetric
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private PersistenceEntryManager userPersistenceEntryManager;

	@Inject
    private DeviceInfoExtractor deviceInfoExtractor;

    @Inject
    private Logger log;

    @Inject
    @Named("fido2MetricsService")
    private Instance<io.jans.fido2.service.metric.Fido2MetricsService> fido2MetricsServiceInstance;

    @Inject
    @Named("fido2UserMetricsService")
    private Instance<io.jans.fido2.service.metric.Fido2UserMetricsService> fido2UserMetricsServiceInstance;

    
    private static final String UNKNOWN_ERROR = "UNKNOWN";
    private static final String ATTEMPT_STATUS = "ATTEMPT";
    private static final String SUCCESS_STATUS = "SUCCESS";

    public void initTimer() {
    	initTimer(this.appConfiguration.getMetricReporterInterval(), this.appConfiguration.getMetricReporterKeepDataDays());
    }

	@Override
	public String baseDn() {
		return staticConfiguration.getBaseDn().getMetric();
	}

	public io.jans.service.metric.MetricService getMetricServiceInstance() {
		return instance.get();
	}

    @Override
    public boolean isMetricReporterEnabled() {
        return this.appConfiguration.getMetricReporterEnabled();
    }

    @Override
    public ApplicationType getApplicationType() {
        return ApplicationType.FIDO2;
    }

    @Override
    public PersistenceEntryManager getEntryManager() {
        return persistenceEntryManager;
    }

	@Override
	public String getNodeIndetifier() {
		return networkService.getMacAdress();
	}

    // ========== FIDO2 PASSKEY REGISTRATION METRICS ==========

    /**
     * Record passkey registration attempt
     *
     * @param username Username attempting registration
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     */
    public void recordPasskeyRegistrationAttempt(String username, HttpServletRequest request, long startTime) {
        recordRegistrationMetrics(username, request, startTime, null, ATTEMPT_STATUS, null, Fido2MetricType.FIDO2_REGISTRATION_ATTEMPT);
    }

    /**
     * Record successful passkey registration
     *
     * @param username Username who completed registration
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     * @param authenticatorType Type of authenticator used
     */
    public void recordPasskeyRegistrationSuccess(String username, HttpServletRequest request, long startTime, String authenticatorType) {
        recordRegistrationMetrics(username, request, startTime, authenticatorType, SUCCESS_STATUS, null, Fido2MetricType.FIDO2_REGISTRATION_SUCCESS);
    }

    /**
     * Record failed passkey registration
     *
     * @param username Username who failed registration
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     * @param errorReason Reason for failure
     * @param authenticatorType Type of authenticator used (if known)
     */
    public void recordPasskeyRegistrationFailure(String username, HttpServletRequest request, long startTime, String errorReason, String authenticatorType) {
        recordRegistrationMetrics(username, request, startTime, authenticatorType, "FAILURE", errorReason, Fido2MetricType.FIDO2_REGISTRATION_FAILURE);
    }

    /**
     * Common method to record registration metrics
     */
    private void recordRegistrationMetrics(String username, HttpServletRequest request, long startTime, 
                                        String authenticatorType, String status, String errorReason, Fido2MetricType metricType) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                recordBasicMetrics(metricType, startTime, status, Fido2MetricType.FIDO2_REGISTRATION_DURATION);
                recordDetailedMetrics(username, status, request, startTime, authenticatorType, errorReason, 
                                    this::createRegistrationMetricsData);
            } catch (Exception e) {
                log.warn("Failed to record passkey registration {} metrics: {}", status.toLowerCase(), e.getMessage());
            }
        });
    }

    // ========== FIDO2 PASSKEY AUTHENTICATION METRICS ==========

    /**
     * Record passkey authentication attempt
     *
     * @param username Username attempting authentication
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     */
    public void recordPasskeyAuthenticationAttempt(String username, HttpServletRequest request, long startTime) {
        recordAuthenticationMetrics(username, request, startTime, null, ATTEMPT_STATUS, null, Fido2MetricType.FIDO2_AUTHENTICATION_ATTEMPT);
    }

    /**
     * Record successful passkey authentication
     *
     * @param username Username who completed authentication
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     * @param authenticatorType Type of authenticator used
     */
    public void recordPasskeyAuthenticationSuccess(String username, HttpServletRequest request, long startTime, String authenticatorType) {
        recordAuthenticationMetrics(username, request, startTime, authenticatorType, SUCCESS_STATUS, null, Fido2MetricType.FIDO2_AUTHENTICATION_SUCCESS);
    }

    /**
     * Record failed passkey authentication
     *
     * @param username Username who failed authentication
     * @param request HTTP request for device info extraction
     * @param startTime Start time of the operation
     * @param errorReason Reason for failure
     * @param authenticatorType Type of authenticator used (if known)
     */
    public void recordPasskeyAuthenticationFailure(String username, HttpServletRequest request, long startTime, String errorReason, String authenticatorType) {
        recordAuthenticationMetrics(username, request, startTime, authenticatorType, "FAILURE", errorReason, Fido2MetricType.FIDO2_AUTHENTICATION_FAILURE);
    }

    /**
     * Common method to record authentication metrics
     */
    private void recordAuthenticationMetrics(String username, HttpServletRequest request, long startTime, 
                                          String authenticatorType, String status, String errorReason, Fido2MetricType metricType) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                recordBasicMetrics(metricType, startTime, status, Fido2MetricType.FIDO2_AUTHENTICATION_DURATION);
                recordDetailedMetrics(username, status, request, startTime, authenticatorType, errorReason, 
                                    this::createAuthenticationMetricsData);
            } catch (Exception e) {
                log.warn("Failed to record passkey authentication {} metrics: {}", status.toLowerCase(), e.getMessage());
            }
        });
    }

    /**
     * Record basic metrics (counter and timer)
     */
    private void recordBasicMetrics(Fido2MetricType metricType, long startTime, String status, Fido2MetricType durationMetricType) {
        incrementFido2Counter(metricType);
        
        if (appConfiguration.isFido2PerformanceMetrics() && !ATTEMPT_STATUS.equals(status)) {
            long duration = System.currentTimeMillis() - startTime;
            updateFido2Timer(durationMetricType, duration);
        }
    }
    
    /**
     * Record detailed metrics with device info collection
     */
    private void recordDetailedMetrics(String username, String status, HttpServletRequest request, long startTime, 
                                     String authenticatorType, String errorReason, 
                                     MetricsDataCreator dataCreator) {
        if (appConfiguration.isFido2DeviceInfoCollection()) {
            Fido2MetricsData metricsData = dataCreator.create(username, status, request, authenticatorType);
            
            if (!ATTEMPT_STATUS.equals(status)) {
                long duration = System.currentTimeMillis() - startTime;
                metricsData.setDurationMs(duration);
            }
            
            if (errorReason != null) {
                metricsData.setErrorReason(errorReason);
                if (appConfiguration.isFido2ErrorCategorization()) {
                    metricsData.setErrorCategory(categorizeError(errorReason));
                }
            }
            
            storeFido2MetricsData(metricsData);
            
            // Update user-level metrics (skip for ATTEMPT status)
            if (!ATTEMPT_STATUS.equals(status)) {
                updateUserMetrics(metricsData);
            }
        }
    }
    
    /**
     * Functional interface for creating metrics data
     */
    @FunctionalInterface
    private interface MetricsDataCreator {
        Fido2MetricsData create(String username, String status, HttpServletRequest request, String authenticatorType);
    }

    // ========== FIDO2 PASSKEY FALLBACK METRICS ==========

    /**
     * Record passkey fallback event (when user switches to password or other method)
     *
     * @param username Username who fell back
     * @param fallbackMethod Method user fell back to (e.g., "PASSWORD", "SMS")
     * @param reason Reason for fallback
     */
    public void recordPasskeyFallback(String username, String fallbackMethod, String reason) {
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                incrementFido2Counter(Fido2MetricType.FIDO2_FALLBACK_EVENT);
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = new Fido2MetricsData();
                    metricsData.setOperationType("FALLBACK");
                    metricsData.setOperationStatus("EVENT");
                    metricsData.setUsername(username);
                    metricsData.setFallbackMethod(fallbackMethod);
                    metricsData.setFallbackReason(reason);
                    // Use UTC timezone to align with FIDO2 services
                    LocalDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
                    metricsData.setStartTime(utcNow);
                    metricsData.setEndTime(utcNow);
                    
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey fallback metrics: {}", e.getMessage());
            }
        });
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Check if FIDO2 metrics collection is enabled
     */
    private boolean isFido2MetricsEnabled() {
        return appConfiguration.isFido2MetricsEnabled() && isMetricReporterEnabled();
    }

    /**
     * Create registration metrics data object
     */
    private Fido2MetricsData createRegistrationMetricsData(String username, String status, HttpServletRequest request, String authenticatorType) {
        return createMetricsData("REGISTRATION", username, status, request, authenticatorType);
    }

    /**
     * Create authentication metrics data object
     */
    private Fido2MetricsData createAuthenticationMetricsData(String username, String status, HttpServletRequest request, String authenticatorType) {
        return createMetricsData("AUTHENTICATION", username, status, request, authenticatorType);
    }

    /**
     * Common method to create metrics data objects
     */
    private Fido2MetricsData createMetricsData(String operationType, String username, String status, HttpServletRequest request, String authenticatorType) {
        Fido2MetricsData metricsData = new Fido2MetricsData();
        metricsData.setOperationType(operationType);
        metricsData.setOperationStatus(status);
        metricsData.setUsername(username);
        
        // Look up the real userId (inum) from username
        // This is the immutable unique identifier that should be used for analytics
        String userId = getUserIdFromUsername(username);
        metricsData.setUserId(userId);
        
        // Use UTC timezone to align with FIDO2 services
        LocalDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        metricsData.setStartTime(utcNow);
        metricsData.setEndTime(utcNow);
        
        if (authenticatorType != null) {
            metricsData.setAuthenticatorType(authenticatorType);
            incrementFido2Counter(Fido2MetricType.FIDO2_DEVICE_TYPE_USAGE);
        }
        
        // Extract HTTP request details
        if (request != null) {
            try {
                // Extract IP address - check proxy headers first, then fall back to remote address
                String ipAddress = extractIpAddress(request);
                metricsData.setIpAddress(ipAddress);
                
                // Extract User-Agent header
                String userAgent = request.getHeader("User-Agent");
                metricsData.setUserAgent(userAgent);
            } catch (Exception e) {
                log.debug("Failed to extract request details: {}", e.getMessage());
            }
            
            // Extract device info if enabled
            if (appConfiguration.isFido2DeviceInfoCollection()) {
                try {
                    metricsData.setDeviceInfo(deviceInfoExtractor.extractDeviceInfo(request));
                } catch (Exception e) {
                    log.debug("Failed to extract device info: {}", e.getMessage());
                    metricsData.setDeviceInfo(deviceInfoExtractor.createMinimalDeviceInfo());
                }
            }
        }
        
        // Set node identifier (for cluster environments) - only if available
        try {
            String nodeId = networkService.getMacAdress();
            if (nodeId != null && !nodeId.trim().isEmpty()) {
                metricsData.setNodeId(nodeId);
            }
        } catch (Exception e) {
            log.debug("Failed to get node ID: {}", e.getMessage());
        }
        
        // Note: applicationType is not set as it's always "FIDO2" and redundant
        
        return metricsData;
    }

    /**
     * Categorize error reasons for analytics
     */
    private String categorizeError(String errorReason) {
        if (errorReason == null) {
            return UNKNOWN_ERROR;
        }
        
        String lowerError = errorReason.toLowerCase();
        
        if (lowerError.contains("timeout") || lowerError.contains("expired")) {
            return "TIMEOUT";
        } else if (lowerError.contains("invalid") || lowerError.contains("malformed")) {
            return "INVALID_INPUT";
        } else if (lowerError.contains("not found") || lowerError.contains("missing")) {
            return "NOT_FOUND";
        } else if (lowerError.contains("unauthorized") || lowerError.contains("forbidden")) {
            return "AUTHORIZATION";
        } else if (lowerError.contains("server") || lowerError.contains("internal")) {
            return "SERVER_ERROR";
        } else if (lowerError.contains("network") || lowerError.contains("connection")) {
            return "NETWORK_ERROR";
        } else {
            return "OTHER";
        }
    }

    /**
     * Store FIDO2 metrics data to persistence layer
     */
    private void storeFido2MetricsData(Fido2MetricsData metricsData) {
        try {
            if (fido2MetricsServiceInstance != null && !fido2MetricsServiceInstance.isUnsatisfied()) {
                fido2MetricsServiceInstance.get().storeMetricsData(metricsData);
            } else {
                log.debug("Fido2MetricsService not available, skipping metrics data storage");
            }
        } catch (Exception e) {
            log.error("Failed to store FIDO2 metrics data: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to increment FIDO2 counters using the base MetricService
     */
    private void incrementFido2Counter(Fido2MetricType fido2MetricType) {
        // For now, we'll log the FIDO2 metrics since we don't have access to base MetricType
        // In a production environment, you might want to integrate with the base metric system
        log.debug("FIDO2 Counter incremented: {} - {}", fido2MetricType.getMetricName(), fido2MetricType.getDescription());
    }
    
    /**
     * Helper method to update FIDO2 timers using the base MetricService
     */
    private void updateFido2Timer(Fido2MetricType fido2MetricType, long duration) {
        // For now, we'll log the FIDO2 timer metrics
        // In a production environment, you might want to integrate with the base metric system
        log.debug("FIDO2 Timer updated: {} - {} ms", fido2MetricType.getMetricName(), duration);
    }

    /**
     * Extract IP address from HTTP request, checking proxy headers first
     * Handles X-Forwarded-For, Proxy-Client-IP, and other common proxy headers
     * 
     * @param request HTTP servlet request
     * @return Client IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // List of headers to check (in order of preference)
        String[] headersToTry = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        // Check each header
        for (String header : headersToTry) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs; take the first one
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex).trim();
                }
                return ip;
            }
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Look up the immutable userId (inum) from username
     * This ensures we use the stable unique identifier for analytics
     * 
     * @param username The username to look up
     * @return The user's inum (userId), or the username as fallback if lookup fails
     */
    private String getUserIdFromUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        try {
            // In Janssen, users are stored with "uid" attribute as username
            // and "inum" as the unique identifier
            io.jans.orm.model.base.SimpleBranch usersBranch = new io.jans.orm.model.base.SimpleBranch();
            usersBranch.setDn(staticConfiguration.getBaseDn().getPeople());
            
            // Search for user by uid (username)
            io.jans.orm.search.filter.Filter filter = io.jans.orm.search.filter.Filter.createEqualityFilter("uid", username);
            
            // Use a simple user object to get the inum
            java.util.List<io.jans.as.common.model.common.User> users = userPersistenceEntryManager.findEntries(
                staticConfiguration.getBaseDn().getPeople(),
                io.jans.as.common.model.common.User.class,
                filter,
                null,
                1
            );
            
            if (users != null && !users.isEmpty()) {
                io.jans.as.common.model.common.User user = users.get(0);
                String inum = user.getAttribute("inum");
                if (inum != null && !inum.trim().isEmpty()) {
                    log.debug("Resolved username '{}' to userId '{}'", username, inum);
                    return inum;
                }
            }
            
            // Fallback: if we can't find the inum, use username as identifier
            log.debug("Could not resolve userId for username '{}', using username as fallback", username);
            return username;
            
        } catch (Exception e) {
            // If lookup fails, use username as fallback (better than null)
            log.debug("Failed to look up userId for username '{}': {}, using username as fallback", 
                     username, e.getMessage());
            return username;
        }
    }

    /**
     * Update user-level metrics based on the metrics data
     */
    private void updateUserMetrics(Fido2MetricsData metricsData) {
        if (fido2UserMetricsServiceInstance == null || fido2UserMetricsServiceInstance.isUnsatisfied()) {
            log.debug("Fido2UserMetricsService not available, skipping user metrics update");
            return;
        }

        try {
            io.jans.fido2.service.metric.Fido2UserMetricsService userMetricsService = fido2UserMetricsServiceInstance.get();
            
            UserMetricsUpdateRequest request = new UserMetricsUpdateRequest();
            request.setUserId(metricsData.getUserId());
            request.setUsername(metricsData.getUsername());
            request.setSuccess(SUCCESS_STATUS.equals(metricsData.getOperationStatus()));
            request.setAuthenticatorType(metricsData.getAuthenticatorType());
            request.setDurationMs(metricsData.getDurationMs());
            
            // Extract device info if available
            if (metricsData.getDeviceInfo() != null) {
                request.setDeviceType(metricsData.getDeviceInfo().getDeviceType());
                request.setBrowser(metricsData.getDeviceInfo().getBrowser());
                request.setOs(metricsData.getDeviceInfo().getOperatingSystem());
            }
            
            request.setIpAddress(metricsData.getIpAddress());
            request.setUserAgent(metricsData.getUserAgent());
            request.setFallbackMethod(metricsData.getFallbackMethod());
            request.setFallbackReason(metricsData.getFallbackReason());

            // Call the appropriate user metrics update method based on operation type
            String operationType = metricsData.getOperationType();
            if ("REGISTRATION".equals(operationType)) {
                userMetricsService.updateUserRegistrationMetrics(request);
                log.debug("Updated user registration metrics for: {}", metricsData.getUsername());
            } else if ("AUTHENTICATION".equals(operationType)) {
                userMetricsService.updateUserAuthenticationMetrics(request);
                log.debug("Updated user authentication metrics for: {}", metricsData.getUsername());
            } else if ("FALLBACK".equals(operationType)) {
                userMetricsService.updateUserFallbackMetrics(
                    request.getUserId(),
                    request.getUsername(),
                    request.getIpAddress(),
                    request.getUserAgent()
                );
                log.debug("Updated user fallback metrics for: {}", metricsData.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to update user metrics: {}", e.getMessage(), e);
        }
    }

}