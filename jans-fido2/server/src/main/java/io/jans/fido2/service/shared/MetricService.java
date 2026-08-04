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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    private static final String SESSION_ID_COOKIE = "session_id";
    
    // Cache for username-to-userId mapping to reduce database load
    // TTL: 1 hour (3600000 ms) - balances performance with data freshness
    // Max size: 10000 entries - prevents unbounded memory growth in high-cardinality scenarios
    private static final long USER_ID_CACHE_TTL_MS = 3600000L; // 1 hour
    private static final int USER_ID_CACHE_MAX_SIZE = 10000; // Maximum cache entries
    private final java.util.Map<String, CacheEntry> userIdCache = new java.util.concurrent.ConcurrentHashMap<>();
    
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

        // The request is request-scoped and unreachable from the async thread below,
        // so all of its data has to be read here, while we are still on the request thread.
        RequestSnapshot requestSnapshot = snapshotRequest(request);
        MetricEvent event = new MetricEvent("REGISTRATION", metricType, username, status, authenticatorType,
                                            errorReason, startTime);

        CompletableFuture.runAsync(() -> {
            try {
                recordBasicMetrics(metricType, startTime, status, Fido2MetricType.FIDO2_REGISTRATION_DURATION);
                recordDetailedMetrics(event, requestSnapshot);
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

        // The request is request-scoped and unreachable from the async thread below,
        // so all of its data has to be read here, while we are still on the request thread.
        RequestSnapshot requestSnapshot = snapshotRequest(request);
        MetricEvent event = new MetricEvent("AUTHENTICATION", metricType, username, status, authenticatorType,
                                            errorReason, startTime);

        CompletableFuture.runAsync(() -> {
            try {
                recordBasicMetrics(metricType, startTime, status, Fido2MetricType.FIDO2_AUTHENTICATION_DURATION);
                recordDetailedMetrics(event, requestSnapshot);
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
     * Record the detailed entry for a passkey event.
     *
     * Persistence is governed by fido2MetricsEnabled alone. fido2DeviceInfoCollection
     * only decides whether the parsed device info rides along - it is not a second
     * master switch, so every other field is recorded either way.
     */
    private void recordDetailedMetrics(MetricEvent event, RequestSnapshot requestSnapshot) {
        Fido2MetricsData metricsData = createMetricsData(event, requestSnapshot);
        boolean completed = !ATTEMPT_STATUS.equals(event.status);

        if (completed) {
            metricsData.setDurationMs(System.currentTimeMillis() - event.startTime);
        }

        if (event.errorReason != null) {
            metricsData.setErrorReason(event.errorReason);
            if (appConfiguration.isFido2ErrorCategorization()) {
                metricsData.setErrorCategory(categorizeError(event.errorReason));
            }
        }

        storeFido2MetricsData(metricsData);

        // Update user-level metrics (skip for ATTEMPT status)
        if (completed) {
            updateUserMetrics(metricsData);
        }
    }
    
    /**
     * Everything known about a single passkey event before the request is consumed.
     */
    private static final class MetricEvent {

        private final String operationType;
        private final Fido2MetricType metricType;
        private final String username;
        private final String status;
        private final String authenticatorType;
        private final String errorReason;
        private final long startTime;

        private MetricEvent(String operationType, Fido2MetricType metricType, String username, String status,
                            String authenticatorType, String errorReason, long startTime) {
            this.operationType = operationType;
            this.metricType = metricType;
            this.username = username;
            this.status = status;
            this.authenticatorType = authenticatorType;
            this.errorReason = errorReason;
            this.startTime = startTime;
        }
    }

    /**
     * Immutable copy of the per-request values used by the FIDO2 metrics.
     *
     * Metrics are persisted on a background thread, where the request-scoped
     * {@code HttpServletRequest} is no longer reachable. Reading it there yields
     * either a null field or a scope-not-active failure, so the values are read
     * once on the request thread and carried across in this holder instead.
     */
    private static final class RequestSnapshot {

        private static final RequestSnapshot EMPTY = new RequestSnapshot(null, null, null, null);

        private final String ipAddress;
        private final String userAgent;
        private final String sessionId;
        private final Fido2MetricsData.DeviceInfo deviceInfo;

        private RequestSnapshot(String ipAddress, String userAgent, String sessionId,
                                Fido2MetricsData.DeviceInfo deviceInfo) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
            this.deviceInfo = deviceInfo;
        }
    }

    /**
     * Read every request-derived metric value while still on the request thread.
     *
     * Must not be called from an asynchronous task - see {@link RequestSnapshot}.
     *
     * @param request HTTP request being served, may be null or an inactive proxy
     * @return snapshot of the request, never null
     */
    private RequestSnapshot snapshotRequest(HttpServletRequest request) {
        if (request == null) {
            return RequestSnapshot.EMPTY;
        }

        String ipAddress = null;
        String userAgent = null;
        String sessionId = null;
        Fido2MetricsData.DeviceInfo deviceInfo = null;

        try {
            ipAddress = extractIpAddress(request);
            userAgent = request.getHeader("User-Agent");
            sessionId = extractSessionId(request);
        } catch (Exception e) {
            // A request-scoped proxy outside of an active request lands here
            log.debug("Failed to extract request details: {}", e.getMessage());
            return RequestSnapshot.EMPTY;
        }

        if (appConfiguration.isFido2DeviceInfoCollection()) {
            try {
                deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);
            } catch (Exception e) {
                log.debug("Failed to extract device info: {}", e.getMessage());
                deviceInfo = deviceInfoExtractor.createMinimalDeviceInfo();
            }
        }

        return new RequestSnapshot(ipAddress, userAgent, sessionId, deviceInfo);
    }

    /**
     * Resolve the session this FIDO2 operation belongs to.
     *
     * FIDO2 endpoints are stateless, so the authoritative value is the Jans
     * {@code session_id} cookie set by the auth server; the servlet session is
     * only a fallback for deployments that do create one.
     *
     * @param request HTTP servlet request
     * @return session identifier, or null when the request carries none
     */
    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_ID_COOKIE.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            }
        }

        HttpSession session = request.getSession(false);
        return session == null ? null : session.getId();
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

                // A fallback event carries no device info, so it is not gated on
                // fido2DeviceInfoCollection - fido2MetricsEnabled above is the switch.
                Fido2MetricsData metricsData = new Fido2MetricsData();
                metricsData.setMetricType(Fido2MetricType.FIDO2_FALLBACK_EVENT.getMetricName());
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
            } catch (Exception e) {
                log.warn("Failed to record passkey fallback metrics: {}", e.getMessage());
            }
        });
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Check if FIDO2 metrics collection is enabled.
     *
     * Deliberately not gated on isMetricReporterEnabled(): metricReporter* belongs to the
     * legacy jans-core reporter and is a separate feature from passkey telemetry. Writes
     * go through Fido2MetricsService, which has always gated on fido2MetricsEnabled alone,
     * so including the reporter flag here left the two halves disagreeing - aggregations
     * ran while the raw entries they summarise were silently dropped.
     */
    private boolean isFido2MetricsEnabled() {
        return appConfiguration.isFido2MetricsEnabled();
    }

    /**
     * Create the metrics data object for a passkey event
     */
    private Fido2MetricsData createMetricsData(MetricEvent event, RequestSnapshot requestSnapshot) {
        Fido2MetricsData metricsData = new Fido2MetricsData();
        metricsData.setMetricType(event.metricType.getMetricName());
        metricsData.setOperationType(event.operationType);
        metricsData.setOperationStatus(event.status);
        metricsData.setUsername(event.username);

        // Look up the real userId (inum) from username
        // This is the immutable unique identifier that should be used for analytics
        String userId = getUserIdFromUsername(event.username);
        metricsData.setUserId(userId);

        // Use UTC timezone to align with FIDO2 services
        LocalDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
        metricsData.setStartTime(utcNow);
        metricsData.setEndTime(utcNow);

        if (event.authenticatorType != null) {
            metricsData.setAuthenticatorType(event.authenticatorType);
            incrementFido2Counter(Fido2MetricType.FIDO2_DEVICE_TYPE_USAGE);
        }

        // HTTP request details, captured on the request thread by snapshotRequest()
        metricsData.setIpAddress(requestSnapshot.ipAddress);
        metricsData.setUserAgent(requestSnapshot.userAgent);
        metricsData.setSessionId(requestSnapshot.sessionId);
        metricsData.setDeviceInfo(requestSnapshot.deviceInfo);

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
     * SECURITY NOTE: This method trusts proxy headers without validation. In production,
     * ensure the application is behind a trusted reverse proxy (e.g., nginx, Apache, load balancer)
     * that strips or validates these headers. If the application is directly exposed to the internet,
     * clients can spoof these headers to mask their real IP address.
     * 
     * For enhanced security, consider:
     * 1. Only trusting proxy headers when behind a known reverse proxy
     * 2. Validating the source IP is from a trusted proxy before trusting forwarded headers
     * 3. Making proxy header trust configurable via application configuration
     * 
     * @param request HTTP servlet request
     * @return Client IP address (may be spoofed if not behind trusted proxy)
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // Get the direct remote address first (most trustworthy)
        String directRemoteAddr = request.getRemoteAddr();
        
        // List of proxy headers to check (in order of preference)
        // Only check these if we're behind a trusted proxy (validation should be added in production)
        String[] proxyHeadersToTry = {
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
        
        // Check proxy headers. These are only meaningful behind a reverse proxy that
        // overwrites them; see the method javadoc for the trust caveat that implies.
        for (String header : proxyHeadersToTry) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs; take the first one
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex).trim();
                }
                // Basic validation: check if it looks like a valid IP
                if (isValidIpAddress(ip)) {
                    return ip;
                }
            }
        }
        
        // Fallback to direct remote address (most secure)
        return directRemoteAddr;
    }
    
    /**
     * Basic validation for IP address format
     *
     * Package-private so the parsing can be exercised directly - it guards a value taken
     * from caller-controlled proxy headers.
     *
     * @param ip IP address string to validate
     * @return true if format appears valid, false otherwise
     */
    boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // Check for loopback addresses first
        if (ip.equals("localhost") || ip.equals("127.0.0.1") || ip.equals("::1")) {
            return true;
        }
        
        if (isValidIpv4Address(ip)) {
            return true;
        }

        // Validate IPv6: simplified check for colons (full validation would be more complex)
        // The pattern admits no letters beyond hex digits, so getByName cannot trigger a DNS lookup
        if (ip.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$")) {
            // Additional validation: try parsing with InetAddress for more robust check
            try {
                java.net.InetAddress.getByName(ip);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Validate dotted-quad IPv4 form, each octet being 0-255.
     *
     * Parsed rather than matched against a regex: the equivalent expression needs four
     * alternation groups and trips complexity limits without being any clearer.
     *
     * @param ip candidate address, already known to be non-blank
     * @return true if the value is a well-formed IPv4 address
     */
    private boolean isValidIpv4Address(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) {
                return false;
            }
            for (int i = 0; i < octet.length(); i++) {
                // Deliberately not Character.isDigit: that accepts non-ASCII Unicode digits
                // (e.g. Arabic-Indic), which Integer.parseInt would then happily convert.
                char digit = octet.charAt(i);
                if (digit < '0' || digit > '9') {
                    return false;
                }
            }
            if (Integer.parseInt(octet) > 255) {
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