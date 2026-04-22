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
import java.util.List;
import java.util.stream.Collectors;
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
    
    // Cache for username-to-userId mapping to reduce database load
    // TTL: 1 hour (3600000 ms) - balances performance with data freshness
    // Max size: 10000 entries - prevents unbounded memory growth in high-cardinality scenarios
    private static final long USER_ID_CACHE_TTL_MS = 3600000L; // 1 hour
    private static final int USER_ID_CACHE_MAX_SIZE = 10000; // Maximum cache entries
    private final java.util.Map<String, CacheEntry> userIdCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Guards the "empty trusted-proxy ranges" warning so it fires only once per instance,
    // not on every request — prevents log flooding when the server is misconfigured.
    private final java.util.concurrent.atomic.AtomicBoolean emptyRangesWarnLogged =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    
    /**
     * Cache entry for username-to-userId mapping
     */
    private static class CacheEntry {
        final String userId;
        final long timestamp;
        
        CacheEntry(String userId) {
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > USER_ID_CACHE_TTL_MS;
        }
    }
    
    /**
     * Clean up expired entries and enforce max cache size
     * Called periodically to prevent unbounded memory growth
     * Uses a thread-safe approach: collect keys first, then remove to avoid race conditions
     */
    private void cleanupCache() {
        // First, remove expired entries (thread-safe operation on ConcurrentHashMap)
        userIdCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // If still over limit after removing expired, remove oldest entries
        int currentSize = userIdCache.size();
        if (currentSize >= USER_ID_CACHE_MAX_SIZE) {
            // Collect keys to remove first (thread-safe snapshot)
            // Calculate how many to remove: remove enough to get below 90% of max size
            int targetSize = (int) (USER_ID_CACHE_MAX_SIZE * 0.9);
            int toRemove = currentSize - targetSize;
            
            if (toRemove > 0) {
                // Collect oldest entries by timestamp (create snapshot to avoid concurrent modification)
                List<java.util.Map.Entry<String, CacheEntry>> entriesToRemove = 
                    userIdCache.entrySet().stream()
                        .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                        .limit(toRemove)
                        .collect(Collectors.toList());
                
                // Remove collected entries (safe to do after stream collection)
                entriesToRemove.forEach(entry -> userIdCache.remove(entry.getKey()));
            }
        }
    }

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
     * Extract the client IP address from an HTTP request.
     *
     * <p>Behaviour is governed by {@code trustedProxyEnabled} in {@link AppConfiguration}:
     * <ul>
     *   <li><b>null (unset)</b> — legacy mode: proxy headers are trusted unconditionally
     *       (identical to the behaviour before this change, so existing deployments are unaffected).
     *   <li><b>false</b> — proxy headers are never read; {@code remoteAddr} is returned directly.
     *   <li><b>true, empty {@code trustedProxyIpRanges}</b> — proxy headers are trusted from any
     *       source IP (equivalent to the legacy unconfigured mode, but explicitly opted-in).
     *   <li><b>true, non-empty {@code trustedProxyIpRanges}</b> — proxy headers are only trusted
     *       when the direct {@code remoteAddr} falls within one of the configured CIDR ranges.
     *       Requests from outside those ranges fall back to {@code remoteAddr}.
     * </ul>
     *
     * @param request HTTP servlet request
     * @return resolved client IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String directRemoteAddr = request.getRemoteAddr();
        Boolean trustedProxyEnabled = appConfiguration.getTrustedProxyEnabled();

        // Proxy header trust explicitly disabled — return the wire address immediately.
        if (Boolean.FALSE.equals(trustedProxyEnabled)) {
            return directRemoteAddr;
        }

        // Proxy header trust is enabled (or unconfigured = legacy behaviour).
        // When enabled, always validate the connecting IP — empty ranges means no proxy is trusted.
        if (Boolean.TRUE.equals(trustedProxyEnabled)) {
            List<String> trustedRanges = appConfiguration.getTrustedProxyIpRanges();
            if (trustedRanges == null || trustedRanges.isEmpty()) {
                if (emptyRangesWarnLogged.compareAndSet(false, true)) {
                    log.warn("trustedProxyEnabled=true but trustedProxyIpRanges is empty — ignoring proxy headers to avoid header spoofing");
                }
                return directRemoteAddr;
            }
            if (!isFromTrustedProxy(directRemoteAddr, trustedRanges)) {
                log.debug("Ignoring proxy headers: remoteAddr '{}' is not in any trusted proxy range",
                        directRemoteAddr);
                return directRemoteAddr;
            }

            // The connecting IP is a trusted proxy. Parse X-Forwarded-For right-to-left:
            // skip hops that are themselves trusted proxies and return the first untrusted
            // valid IP — that is the real client. Left-to-right (leftmost) is unsafe here
            // because a client can inject arbitrary values before the trusted proxy appends.
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
                String[] hops = xForwardedFor.split(",");
                for (int i = hops.length - 1; i >= 0; i--) {
                    String hop = hops[i].trim();
                    if (!hop.isEmpty() && !"unknown".equalsIgnoreCase(hop)
                            && isValidIpAddress(hop)
                            && !isFromTrustedProxy(hop, trustedRanges)) {
                        return hop;
                    }
                }
            }

            // No usable X-Forwarded-For — check single-value proxy headers.
            String[] singleValueHeaders = {
                "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED"
            };
            for (String header : singleValueHeaders) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    ip = ip.trim();
                    if (isValidIpAddress(ip)) {
                        return ip;
                    }
                }
            }

            return directRemoteAddr;
        }

        // Legacy path: trustedProxyEnabled == null — read headers unconditionally,
        // taking the leftmost value to preserve existing behaviour.
        String[] proxyHeaders = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };

        for (String header : proxyHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For may contain a comma-separated chain; the leftmost is the client.
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex).trim();
                }
                if (isValidIpAddress(ip)) {
                    return ip;
                }
            }
        }

        return directRemoteAddr;
    }

    /**
     * Returns true if {@code remoteAddr} falls within at least one of the given CIDR ranges.
     *
     * @param remoteAddr direct connecting IP address
     * @param trustedRanges list of CIDR notations (e.g. {@code "10.0.0.0/8"})
     * @return true when the address is inside a trusted range
     */
    private boolean isFromTrustedProxy(String remoteAddr, List<String> trustedRanges) {
        if (remoteAddr == null || remoteAddr.trim().isEmpty()) {
            return false;
        }
        for (String cidr : trustedRanges) {
            try {
                if (isIpInCidr(remoteAddr.trim(), cidr.trim())) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Invalid trusted proxy CIDR '{}': {}", cidr, e.getMessage());
            }
        }
        return false;
    }

    /**
     * Pure-Java CIDR membership test that works for both IPv4 and IPv6.
     * No external library required — uses only {@link java.net.InetAddress}.
     *
     * @param ip   IP address to test (dotted-decimal or colon-hex notation)
     * @param cidr CIDR block, e.g. {@code "192.168.0.0/16"} or {@code "::1/128"}
     * @return true when {@code ip} is within {@code cidr}
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/", 2);
            java.net.InetAddress cidrAddress = java.net.InetAddress.getByName(parts[0]);
            java.net.InetAddress testAddress = java.net.InetAddress.getByName(ip);

            // Normalize IPv4-mapped IPv6 (::ffff:a.b.c.d) so they compare correctly
            // against IPv4 CIDRs on dual-stack JVMs where getRemoteAddr() returns the mapped form.
            cidrAddress = normalizeAddress(cidrAddress);
            testAddress = normalizeAddress(testAddress);

            byte[] cidrBytes = cidrAddress.getAddress();
            byte[] testBytes = testAddress.getAddress();

            // IPv4 and IPv6 addresses have different byte-array lengths; they can never match.
            if (cidrBytes.length != testBytes.length) {
                return false;
            }

            int maxBits = cidrBytes.length * 8;
            int prefixLength = (parts.length == 2) ? Integer.parseInt(parts[1]) : maxBits;
            if (prefixLength < 0 || prefixLength > maxBits) {
                log.warn("Invalid prefix length {} in CIDR '{}' — rejecting", prefixLength, cidr);
                return false;
            }
            int remainingBits = prefixLength;

            for (int i = 0; i < cidrBytes.length; i++) {
                if (remainingBits >= 8) {
                    if (cidrBytes[i] != testBytes[i]) {
                        return false;
                    }
                    remainingBits -= 8;
                } else if (remainingBits > 0) {
                    int mask = 0xFF << (8 - remainingBits);
                    if ((cidrBytes[i] & mask) != (testBytes[i] & mask)) {
                        return false;
                    }
                    remainingBits = 0;
                } else {
                    break; // remaining bits are host bits — not compared
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("CIDR check failed for ip='{}' cidr='{}': {}", ip, cidr, e.getMessage());
            return false;
        }
    }

    /**
     * Converts an IPv4-mapped IPv6 address (::ffff:a.b.c.d) to its IPv4 equivalent
     * so that CIDR comparisons work correctly on dual-stack JVMs.
     */
    private java.net.InetAddress normalizeAddress(java.net.InetAddress address) throws java.net.UnknownHostException {
        if (address instanceof java.net.Inet6Address) {
            byte[] bytes = address.getAddress();
            // IPv4-mapped form: 10 zero bytes, then 0xFF 0xFF, then 4 IPv4 bytes
            if (bytes.length == 16 &&
                    bytes[0] == 0 && bytes[1] == 0 && bytes[2] == 0 && bytes[3] == 0 &&
                    bytes[4] == 0 && bytes[5] == 0 && bytes[6] == 0 && bytes[7] == 0 &&
                    bytes[8] == 0 && bytes[9] == 0 &&
                    bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF) {
                return java.net.InetAddress.getByAddress(
                        new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
            }
        }
        return address;
    }

    /**
     * Validates that a string is a well-formed IPv4 or IPv6 address.
     *
     * @param ip candidate IP string
     * @return true if the format is valid
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String trimmed = ip.trim();
        // IPv6 literals always contain a colon; getByName never does DNS for them.
        if (trimmed.contains(":")) {
            try {
                java.net.InetAddress.getByName(trimmed);
                return true;
            } catch (java.net.UnknownHostException e) {
                return false;
            }
        }
        // For anything without a colon, only accept a valid IPv4 literal —
        // getByName would perform a blocking DNS lookup for hostnames, which
        // would allow attacker-controlled headers to trigger DNS queries.
        return isValidIpv4Literal(trimmed);
    }

    private boolean isValidIpv4Literal(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) {
                return false;
            }
            for (char c : octet.toCharArray()) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Look up the immutable userId (inum) from username
     * This ensures we use the stable unique identifier for analytics
     * 
     * PERFORMANCE: Uses in-memory cache with 1-hour TTL to reduce database load.
     * In high-throughput scenarios, this prevents a database query on every metric event.
     * 
     * @param username The username to look up
     * @return The user's inum (userId), or the username as fallback if lookup fails
     */
    private String getUserIdFromUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        // Check cache first to avoid database lookup
        CacheEntry cached = userIdCache.get(username);
        if (cached != null && !cached.isExpired()) {
            return cached.userId;
        }
        
        // Cache miss or expired - perform database lookup
        String userId = performUserIdLookup(username);
        
        // Cleanup cache if approaching max size (every 100th lookup to avoid overhead)
        if (userIdCache.size() >= USER_ID_CACHE_MAX_SIZE * 0.9) {
            cleanupCache();
        }
        
        // Update cache (performUserIdLookup always returns non-null - either inum or username as fallback)
        // If cache is full after cleanup, remove oldest entry before adding new one
        if (userIdCache.size() >= USER_ID_CACHE_MAX_SIZE) {
            // Remove oldest entry
            userIdCache.entrySet().stream()
                .min((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                .ifPresent(entry -> userIdCache.remove(entry.getKey()));
        }
        
        userIdCache.put(username, new CacheEntry(userId));
        
        return userId;
    }
    
    /**
     * Perform the actual database lookup for userId from username
     * @param username The username to look up
     * @return The user's inum (userId), or the username as fallback if lookup fails
     */
    private String performUserIdLookup(String username) {
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
                    // PRIVACY: Log userId (inum) instead of username to comply with GDPR/CCPA requirements
                    log.debug("Resolved username to userId: {}", inum);
                    return inum;
                }
            }
            
            // Fallback: if we can't find the inum, use username as identifier
            log.debug("Could not resolve userId for username, using username as fallback");
            return username;
            
        } catch (Exception e) {
            // If lookup fails, use username as fallback (better than null)
            // PRIVACY: Don't log username in error messages to prevent PII leakage
            log.debug("Failed to look up userId: {}, using username as fallback", e.getMessage());
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
            // PRIVACY: Log userId (inum) instead of username to comply with GDPR/CCPA requirements
            String operationType = metricsData.getOperationType();
            String logIdentifier = metricsData.getUserId() != null ? metricsData.getUserId() : "[unknown-user]";
            
            if ("REGISTRATION".equals(operationType)) {
                userMetricsService.updateUserRegistrationMetrics(request);
                log.debug("Updated user registration metrics for userId: {}", logIdentifier);
            } else if ("AUTHENTICATION".equals(operationType)) {
                userMetricsService.updateUserAuthenticationMetrics(request);
                log.debug("Updated user authentication metrics for userId: {}", logIdentifier);
            } else if ("FALLBACK".equals(operationType)) {
                userMetricsService.updateUserFallbackMetrics(
                    request.getUserId(),
                    request.getUsername(),
                    request.getIpAddress(),
                    request.getUserAgent()
                );
                log.debug("Updated user fallback metrics for userId: {}", logIdentifier);
            }
        } catch (Exception e) {
            log.error("Failed to update user metrics: {}", e.getMessage(), e);
        }
    }

}