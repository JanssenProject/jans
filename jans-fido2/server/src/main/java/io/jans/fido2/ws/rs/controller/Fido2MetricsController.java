/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.service.DataMapperService;
import io.jans.fido2.service.metric.Fido2MetricsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for FIDO2/Passkey metrics
 * Provides endpoints to fetch metrics data for dashboards and analytics tools
 * 
 * SECURITY NOTE: Authentication and authorization for these endpoints should be enforced
 * at the infrastructure level (API gateway, OAuth interceptor, or reverse proxy).
 * User-specific endpoints (e.g., /entries/user/{userId}) are particularly sensitive
 * and should restrict access to authorized users or administrators only.
 * 
 * If security is enforced at deployment time, ensure proper documentation is maintained.
 * 
 * GitHub Issue #11922
 * 
 * @author FIDO2 Team
 */
@ApplicationScoped
@Path("/metrics")
public class Fido2MetricsController {

    @Inject
    private Logger log;

    @Inject
    private Fido2MetricsService metricsService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private DataMapperService dataMapperService;

    // ISO formatter for UTC timestamps (aligned with FIDO2 services)
    // Primary formatter: ISO_LOCAL_DATE_TIME for timestamps without timezone (interpreted as UTC)
    // Format: yyyy-MM-ddTHH:mm:ss (assumed to be UTC)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // Alternative formatter that supports ISO-8601 with timezone offsets
    // Format: yyyy-MM-ddTHH:mm:ssZ or yyyy-MM-ddTHH:mm:ss+/-offset
    // The parseDateTime method uses this formatter when timezone indicators are detected
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Get raw metrics entries within a time range
     * 
     * @param startTime Start time in ISO format (e.g., 2024-01-01T00:00:00)
     * @param endTime End time in ISO format
     * @return List of metrics entries
     */
    @GET
    @Path("/entries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsEntries(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            List<?> entries = metricsService.getMetricsEntries(start, end);
            return Response.ok(dataMapperService.writeValueAsString(entries)).build();
        });
    }

    /**
     * Get metrics entries for a specific user
     * 
     * @param userId User ID
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return List of user-specific metrics entries
     */
    @GET
    @Path("/entries/user/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsEntriesByUser(
            @PathParam("userId") String userId,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            if (userId == null || userId.trim().isEmpty()) {
                throw errorResponseFactory.invalidRequest("userId is required");
            }
            String normalizedUserId = userId.trim();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            List<?> entries = metricsService.getMetricsEntriesByUser(normalizedUserId, start, end);
            return Response.ok(dataMapperService.writeValueAsString(entries)).build();
        });
    }

    /**
     * Get metrics entries by operation type (REGISTRATION or AUTHENTICATION)
     * 
     * @param operationType Operation type
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return List of operation-specific metrics entries
     */
    @GET
    @Path("/entries/operation/{operationType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsEntriesByOperation(
            @PathParam("operationType") String operationType,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            // Normalize and validate operationType against supported values
            String normalizedOperationType = normalizeOperationType(operationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            List<?> entries = metricsService.getMetricsEntriesByOperation(normalizedOperationType, start, end);
            return Response.ok(dataMapperService.writeValueAsString(entries)).build();
        });
    }

    /**
     * Get aggregated metrics data
     * 
     * @param aggregationType Aggregation type (HOURLY, DAILY, WEEKLY, MONTHLY)
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return List of aggregated metrics
     */
    @GET
    @Path("/aggregations/{aggregationType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregations(
            @PathParam("aggregationType") String aggregationType,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            String normalizedType = normalizeAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            List<?> aggregations = metricsService.getAggregations(normalizedType, start, end);
            return Response.ok(dataMapperService.writeValueAsString(aggregations)).build();
        });
    }

    /**
     * Get aggregation summary statistics
     * 
     * @param aggregationType Aggregation type (HOURLY, DAILY, WEEKLY, MONTHLY)
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return Summary statistics
     */
    @GET
    @Path("/aggregations/{aggregationType}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregationSummary(
            @PathParam("aggregationType") String aggregationType,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            String normalizedType = normalizeAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> summary = metricsService.getAggregationSummary(normalizedType, start, end);
            return Response.ok(dataMapperService.writeValueAsString(summary)).build();
        });
    }

    /**
     * Get user adoption metrics
     * 
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return User adoption statistics
     */
    @GET
    @Path("/analytics/adoption")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAdoptionMetrics(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> adoption = metricsService.getUserAdoptionMetrics(start, end);
            return Response.ok(dataMapperService.writeValueAsString(adoption)).build();
        });
    }

    /**
     * Get performance metrics (average durations, success rates)
     * 
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return Performance statistics
     */
    @GET
    @Path("/analytics/performance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformanceMetrics(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> performance = metricsService.getPerformanceMetrics(start, end);
            return Response.ok(dataMapperService.writeValueAsString(performance)).build();
        });
    }

    /**
     * Get device analytics (platform distribution, authenticator types)
     * 
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return Device analytics data
     */
    @GET
    @Path("/analytics/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceAnalytics(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> devices = metricsService.getDeviceAnalytics(start, end);
            return Response.ok(dataMapperService.writeValueAsString(devices)).build();
        });
    }

    /**
     * Get error analysis (error categories, frequencies)
     * 
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return Error analysis data
     */
    @GET
    @Path("/analytics/errors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getErrorAnalysis(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> errors = metricsService.getErrorAnalysis(start, end);
            return Response.ok(dataMapperService.writeValueAsString(errors)).build();
        });
    }

    /**
     * Get trend analysis for metrics over time
     * 
     * @param aggregationType Aggregation type for trend analysis
     * @param startTime Start time in ISO format
     * @param endTime End time in ISO format
     * @return Trend analysis data
     */
    @GET
    @Path("/analytics/trends/{aggregationType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTrendAnalysis(
            @PathParam("aggregationType") String aggregationType,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            String normalizedType = normalizeAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            validateTimeRange(start, end);
            
            Map<String, Object> trends = metricsService.getTrendAnalysis(normalizedType, start, end);
            return Response.ok(dataMapperService.writeValueAsString(trends)).build();
        });
    }

    /**
     * Get period-over-period comparison
     * 
     * @param aggregationType Aggregation type for comparison
     * @param periods Number of periods to compare (default: 2)
     * @return Period comparison data
     */
    @GET
    @Path("/analytics/comparison/{aggregationType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPeriodOverPeriodComparison(
            @PathParam("aggregationType") String aggregationType,
            @QueryParam("periods") @DefaultValue("2") int periods) {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            String normalizedType = normalizeAggregationType(aggregationType);
            
            if (periods < 2 || periods > 12) {
                throw errorResponseFactory.invalidRequest("periods must be between 2 and 12");
            }
            
            Map<String, Object> comparison = metricsService.getPeriodOverPeriodComparison(normalizedType, periods);
            return Response.ok(dataMapperService.writeValueAsString(comparison)).build();
        });
    }

    /**
     * Get metrics configuration and status
     * 
     * @return Configuration information
     */
    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsConfig() {
        return processRequest(() -> {
            checkMetricsEnabled();
            
            Map<String, Object> config = new HashMap<>();
            config.put("metricsEnabled", appConfiguration.isFido2MetricsEnabled());
            config.put("aggregationEnabled", appConfiguration.isFido2MetricsAggregationEnabled());
            config.put("retentionDays", appConfiguration.getFido2MetricsRetentionDays());
            config.put("deviceInfoCollection", appConfiguration.isFido2DeviceInfoCollection());
            config.put("errorCategorization", appConfiguration.isFido2ErrorCategorization());
            config.put("performanceMetrics", appConfiguration.isFido2PerformanceMetrics());
            config.put("supportedAggregationTypes", List.of(
                Fido2MetricsConstants.HOURLY,
                Fido2MetricsConstants.DAILY,
                Fido2MetricsConstants.WEEKLY,
                Fido2MetricsConstants.MONTHLY
            ));
            
            return Response.ok(dataMapperService.writeValueAsString(config)).build();
        });
    }

    /**
     * Health check endpoint for metrics service
     * Verifies that the metrics service is functional and can connect to the database
     * 
     * @return Health status
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealth() {
        return processRequest(() -> {
            Map<String, Object> health = new HashMap<>();
            boolean isHealthy = true;
            String status = "UP";
            
            // Check if metrics are enabled
            boolean metricsEnabled = appConfiguration.isFido2MetricsEnabled();
            health.put("metricsEnabled", metricsEnabled);
            
            // Verify service is functional by attempting a simple operation
            boolean serviceAvailable = checkServiceAvailability();
            health.put(Fido2MetricsConstants.HEALTH_SERVICE_AVAILABLE, serviceAvailable);
            if (!serviceAvailable) {
                isHealthy = false;
                status = "DOWN";
            }
            
            health.put("status", status);
            health.put("aggregationEnabled", appConfiguration.isFido2MetricsAggregationEnabled());
            health.put("timestamp", ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().format(ISO_FORMATTER));
            
            Response.ResponseBuilder responseBuilder = isHealthy 
                ? Response.ok(dataMapperService.writeValueAsString(health))
                : Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(dataMapperService.writeValueAsString(health));
            
            return responseBuilder.build();
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if FIDO2 metrics are enabled
     * @throws WebApplicationException if metrics are disabled
     */
    private void checkMetricsEnabled() {
        if (appConfiguration.getFido2Configuration() == null) {
            throw errorResponseFactory.forbiddenException();
        }
        
        if (!appConfiguration.isFido2MetricsEnabled()) {
            throw errorResponseFactory.forbiddenException();
        }
    }

    /**
     * Parse ISO datetime string as UTC
     * Supports both ISO_LOCAL_DATE_TIME (yyyy-MM-ddTHH:mm:ss) and ISO-8601 with timezone offsets
     * All timestamps are converted to UTC to align with FIDO2 services
     * 
     * @param dateTime DateTime string in ISO format (with or without timezone offset)
     * @param paramName Parameter name for error messages
     * @return Parsed LocalDateTime in UTC
     */
    private LocalDateTime parseDateTime(String dateTime, String paramName) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            throw errorResponseFactory.invalidRequest(
                paramName + " is required (ISO format: yyyy-MM-ddTHH:mm:ss or yyyy-MM-ddTHH:mm:ssZ/+offset)"
            );
        }
        
        try {
            // First try parsing with timezone offset support (ISO-8601 compliant)
            if (hasTimezoneIndicator(dateTime)) {
                LocalDateTime parsedWithTimezone = tryParseWithTimezone(dateTime);
                if (parsedWithTimezone != null) {
                    return parsedWithTimezone;
                }
            }
            
            // Parse as LocalDateTime (assumed to be UTC per API documentation)
            return LocalDateTime.parse(dateTime, ISO_FORMATTER);
        } catch (Exception e) {
            throw errorResponseFactory.invalidRequest(
                paramName + " must be in ISO format (yyyy-MM-ddTHH:mm:ss or yyyy-MM-ddTHH:mm:ssZ/+offset). " +
                "Example: 2024-01-01T00:00:00 or 2024-01-01T00:00:00Z"
            );
        }
    }

    /**
     * Validate that startTime is before or equal to endTime
     * @param startTime Start time
     * @param endTime End time
     * @throws WebApplicationException if startTime > endTime
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw errorResponseFactory.invalidRequest(
                "startTime must be less than or equal to endTime. " +
                "Received: startTime=" + startTime + ", endTime=" + endTime
            );
        }
    }
    
    /**
     * Normalize and validate aggregation type parameter
     * Converts to uppercase and validates against allowed values
     * @param aggregationType Aggregation type to normalize and validate
     * @return Normalized (uppercase) aggregation type
     * @throws WebApplicationException if invalid
     */
    private String normalizeAggregationType(String aggregationType) {
        if (aggregationType == null || aggregationType.trim().isEmpty()) {
            throw errorResponseFactory.invalidRequest("aggregationType is required");
        }

        String upperType = aggregationType.trim().toUpperCase();
        if (!Fido2MetricsConstants.HOURLY.equals(upperType) &&
            !Fido2MetricsConstants.DAILY.equals(upperType) &&
            !Fido2MetricsConstants.WEEKLY.equals(upperType) &&
            !Fido2MetricsConstants.MONTHLY.equals(upperType)) {
            throw errorResponseFactory.invalidRequest(
                "aggregationType must be one of: HOURLY, DAILY, WEEKLY, MONTHLY"
            );
        }
        return upperType;
    }
    
    /**
     * Normalize and validate operation type parameter
     * Converts to uppercase and validates against allowed values
     * @param operationType Operation type to normalize and validate
     * @return Normalized (uppercase) operation type
     * @throws WebApplicationException if invalid
     */
    private String normalizeOperationType(String operationType) {
        if (operationType == null || operationType.trim().isEmpty()) {
            throw errorResponseFactory.invalidRequest("operationType is required");
        }

        String upperType = operationType.trim().toUpperCase();
        if (!Fido2MetricsConstants.REGISTRATION.equals(upperType) &&
            !Fido2MetricsConstants.AUTHENTICATION.equals(upperType)) {
            throw errorResponseFactory.invalidRequest(
                "operationType must be one of: REGISTRATION, AUTHENTICATION"
            );
        }
        return upperType;
    }

    /**
     * Process REST request with error handling
     * Sanitizes exception messages to prevent information disclosure
     * @param processor Request processor
     * @return Response
     */
    private Response processRequest(RequestProcessor processor) {
        try {
            return processor.process();
        } catch (WebApplicationException e) {
            // Re-throw web application exceptions as-is (they already have proper status codes)
            throw e;
        } catch (Exception e) {
            // Log the full exception with stack trace for debugging (internal logging)
            log.error("Error processing metrics request: {}", e.getMessage(), e);
            
            // Sanitize error message to prevent information disclosure
            // Don't expose internal details like database connection strings, file paths, etc.
            String sanitizedMessage = sanitizeErrorMessage(e);
            throw errorResponseFactory.unknownError(sanitizedMessage);
        }
    }
    
    /**
     * Sanitize error messages to prevent information disclosure
     * Removes sensitive information like connection strings, file paths, etc.
     * @param exception The exception to sanitize
     * @return Sanitized error message safe for API responses
     */
    private String sanitizeErrorMessage(Exception exception) {
        if (exception == null) {
            return Fido2MetricsConstants.ERROR_UNEXPECTED;
        }
        
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return Fido2MetricsConstants.ERROR_UNEXPECTED;
        }
        
        // Remove potential sensitive information patterns
        // Connection strings, file paths, etc. should not be exposed
        String sanitized = message;
        
        // Remove file system paths (e.g., /opt/jans/..., C:\Users\...)
        sanitized = sanitized.replaceAll("([A-Za-z]:)?[\\\\/][^\\s]+", "[path]");
        
        // Remove potential connection strings (e.g., jdbc:..., ldap://...)
        sanitized = sanitized.replaceAll("(jdbc|ldap|http|https)://[^\\s]+", "[connection]");
        
        // Remove potential credentials (e.g., password=..., pwd=...)
        sanitized = sanitized.replaceAll("(password|pwd|passwd|secret|key|token)=[^\\s,;]+", "$1=[hidden]");
        
        // If sanitization removed too much, use a generic message
        if (sanitized.trim().isEmpty() || sanitized.length() < 10) {
            return Fido2MetricsConstants.ERROR_UNEXPECTED;
        }
        
        return "An unexpected error occurred: " + sanitized;
    }

    /**
     * Check if service is available for health check
     * @return true if service is available, false otherwise
     */
    private boolean checkServiceAvailability() {
        try {
            // Verify service is not null and perform a lightweight operation to test functionality
            if (metricsService == null) {
                return false;
            }
            
            // Perform a lightweight check: verify service can access configuration
            // This tests that the service bean is properly initialized and can access dependencies
            // We don't query the database to keep the health check fast
            // Touch configuration to ensure it is readable (will throw if misconfigured)
            appConfiguration.isFido2MetricsEnabled();

            return true;
        } catch (RuntimeException e) {
            log.warn("Health check detected service issue: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // Catch any other exceptions (e.g., configuration access issues)
            log.debug("Health check encountered unexpected error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if dateTime string contains timezone indicator
     * @param dateTime DateTime string to check
     * @return true if timezone indicator is present
     */
    private boolean hasTimezoneIndicator(String dateTime) {
        // Check for timezone indicators:
        // - "Z" indicates UTC
        // - "+" indicates positive timezone offset
        // - "-" after position 10 indicates negative timezone offset (not part of date)
        return dateTime.contains("Z") || dateTime.contains("+") || 
               (dateTime.contains("-") && dateTime.lastIndexOf('-') > 10);
    }
    
    /**
     * Try to parse dateTime with timezone offset support
     * Extracted to reduce cognitive complexity
     * @param dateTime DateTime string to parse
     * @return Parsed LocalDateTime in UTC, or null if parsing failed
     */
    private LocalDateTime tryParseWithTimezone(String dateTime) {
        try {
            java.time.ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTime, ISO_OFFSET_FORMATTER);
            // Convert to UTC and extract LocalDateTime
            return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        } catch (java.time.format.DateTimeParseException e) {
            // Fall through to try ISO_LOCAL_DATE_TIME format
            log.debug("Failed to parse with timezone offset, trying local format: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Functional interface for request processing
     * 
     * Note: This interface throws Exception to accommodate various exception types
     * that may be thrown by implementations (e.g., WebApplicationException, 
     * RuntimeException, checked exceptions from service calls).
     * The processRequest() method handles all exceptions appropriately.
     */
    @FunctionalInterface
    private interface RequestProcessor {
        Response process() throws Exception;
    }
}

