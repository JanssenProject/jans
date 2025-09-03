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
import io.jans.fido2.service.util.DeviceInfoExtractor;
import io.jans.model.ApplicationType;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.service.net.NetworkService;
import io.jans.model.metric.MetricType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import com.codahale.metrics.Timer;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private DeviceInfoExtractor deviceInfoExtractor;

    @Inject
    private Logger log;

    // Dedicated executor for async metrics processing to avoid blocking main operations
    private final ExecutorService metricsExecutor = Executors.newFixedThreadPool(2);

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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                incCounter(MetricType.FIDO2_REGISTRATION_ATTEMPT);
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createRegistrationMetricsData(username, "ATTEMPT", request, startTime, null);
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey registration attempt metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long duration = System.currentTimeMillis() - startTime;
                incCounter(MetricType.FIDO2_REGISTRATION_SUCCESS);
                
                if (appConfiguration.isFido2PerformanceMetrics()) {
                    Timer timer = getTimer(MetricType.FIDO2_REGISTRATION_DURATION);
                    timer.update(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createRegistrationMetricsData(username, "SUCCESS", request, startTime, authenticatorType);
                    metricsData.setDurationMs(duration);
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey registration success metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long duration = System.currentTimeMillis() - startTime;
                incCounter(MetricType.FIDO2_REGISTRATION_FAILURE);
                
                if (appConfiguration.isFido2PerformanceMetrics()) {
                    Timer timer = getTimer(MetricType.FIDO2_REGISTRATION_DURATION);
                    timer.update(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createRegistrationMetricsData(username, "FAILURE", request, startTime, authenticatorType);
                    metricsData.setDurationMs(duration);
                    metricsData.setErrorReason(errorReason);
                    
                    if (appConfiguration.isFido2ErrorCategorization()) {
                        metricsData.setErrorCategory(categorizeError(errorReason));
                    }
                    
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey registration failure metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                incCounter(MetricType.FIDO2_AUTHENTICATION_ATTEMPT);
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createAuthenticationMetricsData(username, "ATTEMPT", request, startTime, null);
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey authentication attempt metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long duration = System.currentTimeMillis() - startTime;
                incCounter(MetricType.FIDO2_AUTHENTICATION_SUCCESS);
                
                if (appConfiguration.isFido2PerformanceMetrics()) {
                    Timer timer = getTimer(MetricType.FIDO2_AUTHENTICATION_DURATION);
                    timer.update(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createAuthenticationMetricsData(username, "SUCCESS", request, startTime, authenticatorType);
                    metricsData.setDurationMs(duration);
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey authentication success metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
        if (!isFido2MetricsEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long duration = System.currentTimeMillis() - startTime;
                incCounter(MetricType.FIDO2_AUTHENTICATION_FAILURE);
                
                if (appConfiguration.isFido2PerformanceMetrics()) {
                    Timer timer = getTimer(MetricType.FIDO2_AUTHENTICATION_DURATION);
                    timer.update(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = createAuthenticationMetricsData(username, "FAILURE", request, startTime, authenticatorType);
                    metricsData.setDurationMs(duration);
                    metricsData.setErrorReason(errorReason);
                    
                    if (appConfiguration.isFido2ErrorCategorization()) {
                        metricsData.setErrorCategory(categorizeError(errorReason));
                    }
                    
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey authentication failure metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
                incCounter(MetricType.FIDO2_FALLBACK_EVENT);
                
                if (appConfiguration.isFido2DeviceInfoCollection()) {
                    Fido2MetricsData metricsData = new Fido2MetricsData();
                    metricsData.setOperationType("FALLBACK");
                    metricsData.setOperationStatus("EVENT");
                    metricsData.setUsername(username);
                    metricsData.setFallbackMethod(fallbackMethod);
                    metricsData.setFallbackReason(reason);
                    metricsData.setStartTime(LocalDateTime.now());
                    metricsData.setEndTime(LocalDateTime.now());
                    
                    storeFido2MetricsData(metricsData);
                }
            } catch (Exception e) {
                log.warn("Failed to record passkey fallback metrics: {}", e.getMessage());
            }
        }, metricsExecutor);
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
    private Fido2MetricsData createRegistrationMetricsData(String username, String status, HttpServletRequest request, long startTime, String authenticatorType) {
        Fido2MetricsData metricsData = new Fido2MetricsData();
        metricsData.setOperationType("REGISTRATION");
        metricsData.setOperationStatus(status);
        metricsData.setUsername(username);
        metricsData.setStartTime(LocalDateTime.now());
        metricsData.setEndTime(LocalDateTime.now());
        
        if (authenticatorType != null) {
            metricsData.setAuthenticatorType(authenticatorType);
            incCounter(MetricType.FIDO2_DEVICE_TYPE_USAGE);
        }
        
        if (request != null && appConfiguration.isFido2DeviceInfoCollection()) {
            try {
                metricsData.setDeviceInfo(deviceInfoExtractor.extractDeviceInfo(request));
            } catch (Exception e) {
                log.debug("Failed to extract device info: {}", e.getMessage());
                metricsData.setDeviceInfo(deviceInfoExtractor.createMinimalDeviceInfo());
            }
        }
        
        return metricsData;
    }

    /**
     * Create authentication metrics data object
     */
    private Fido2MetricsData createAuthenticationMetricsData(String username, String status, HttpServletRequest request, long startTime, String authenticatorType) {
        Fido2MetricsData metricsData = new Fido2MetricsData();
        metricsData.setOperationType("AUTHENTICATION");
        metricsData.setOperationStatus(status);
        metricsData.setUsername(username);
        metricsData.setStartTime(LocalDateTime.now());
        metricsData.setEndTime(LocalDateTime.now());
        
        if (authenticatorType != null) {
            metricsData.setAuthenticatorType(authenticatorType);
            incCounter(MetricType.FIDO2_DEVICE_TYPE_USAGE);
        }
        
        if (request != null && appConfiguration.isFido2DeviceInfoCollection()) {
            try {
                metricsData.setDeviceInfo(deviceInfoExtractor.extractDeviceInfo(request));
            } catch (Exception e) {
                log.debug("Failed to extract device info: {}", e.getMessage());
                metricsData.setDeviceInfo(deviceInfoExtractor.createMinimalDeviceInfo());
            }
        }
        
        return metricsData;
    }

    /**
     * Categorize error reasons for analytics
     */
    private String categorizeError(String errorReason) {
        if (errorReason == null) {
            return "UNKNOWN";
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
     * Store FIDO2 metrics data (placeholder for future persistence implementation)
     */
    private void storeFido2MetricsData(Fido2MetricsData metricsData) {
        // TODO: Implement persistence to database or external metrics system
        // For now, just log the metrics data for debugging
        if (log.isDebugEnabled()) {
            log.debug("FIDO2 Metrics Data: {}", metricsData);
        }
    }

    /**
     * Cleanup method to shutdown metrics executor
     */
    public void cleanup() {
        if (metricsExecutor != null && !metricsExecutor.isShutdown()) {
            metricsExecutor.shutdown();
        }
    }
}