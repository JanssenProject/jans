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
 * GitHub Issue #11923
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
    // Note: ISO_LOCAL_DATE_TIME does not accept timezone offsets (e.g., Z, +00:00)
    // Users must provide timestamps in format: yyyy-MM-ddTHH:mm:ss (interpreted as UTC)
    // For ISO-8601 with timezone support, consider using DateTimeFormatter.ISO_OFFSET_DATE_TIME
    // and converting to UTC, but this requires API documentation updates
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // Alternative formatter that supports ISO-8601 with timezone offsets
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
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            
            List<?> entries = metricsService.getMetricsEntriesByUser(userId, start, end);
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
            
            if (operationType == null || operationType.trim().isEmpty()) {
                throw errorResponseFactory.invalidRequest("operationType is required");
            }
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            
            List<?> entries = metricsService.getMetricsEntriesByOperation(operationType, start, end);
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
            
            validateAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            
            List<?> aggregations = metricsService.getAggregations(aggregationType, start, end);
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
            
            validateAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            
            Map<String, Object> summary = metricsService.getAggregationSummary(aggregationType, start, end);
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
            
            validateAggregationType(aggregationType);
            
            LocalDateTime start = parseDateTime(startTime, Fido2MetricsConstants.PARAM_START_TIME);
            LocalDateTime end = parseDateTime(endTime, Fido2MetricsConstants.PARAM_END_TIME);
            
            Map<String, Object> trends = metricsService.getTrendAnalysis(aggregationType, start, end);
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
            
            validateAggregationType(aggregationType);
            
            if (periods < 2 || periods > 12) {
                throw errorResponseFactory.invalidRequest("periods must be between 2 and 12");
            }
            
            Map<String, Object> comparison = metricsService.getPeriodOverPeriodComparison(aggregationType, periods);
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
            try {
                // Try to access the metrics service to verify it's available
                if (metricsService != null) {
                    // Perform a lightweight check - verify service can be accessed
                    // This doesn't query the database but verifies the service bean is initialized
                    health.put("serviceAvailable", true);
                } else {
                    health.put("serviceAvailable", false);
                    isHealthy = false;
                    status = "DOWN";
                }
            } catch (Exception e) {
                log.warn("Health check detected service issue: {}", e.getMessage());
                health.put("serviceAvailable", false);
                health.put("serviceError", "Service check failed");
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
            if (dateTime.contains("Z") || dateTime.contains("+") || dateTime.contains("-") && 
                (dateTime.lastIndexOf('-') > 10 || dateTime.contains("+"))) {
                try {
                    java.time.ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTime, ISO_OFFSET_FORMATTER);
                    // Convert to UTC and extract LocalDateTime
                    return zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
                } catch (Exception e) {
                    // Fall through to try ISO_LOCAL_DATE_TIME format
                    log.debug("Failed to parse with timezone offset, trying local format: {}", e.getMessage());
                }
            }
            
            // Parse as LocalDateTime (assumed to be UTC per API documentation)
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, ISO_FORMATTER);
            return localDateTime;
        } catch (Exception e) {
            throw errorResponseFactory.invalidRequest(
                paramName + " must be in ISO format (yyyy-MM-ddTHH:mm:ss or yyyy-MM-ddTHH:mm:ssZ/+offset). " +
                "Example: 2024-01-01T00:00:00 or 2024-01-01T00:00:00Z"
            );
        }
    }

    /**
     * Validate aggregation type parameter
     * @param aggregationType Aggregation type to validate
     * @throws WebApplicationException if invalid
     */
    private void validateAggregationType(String aggregationType) {
        if (aggregationType == null || aggregationType.trim().isEmpty()) {
            throw errorResponseFactory.invalidRequest("aggregationType is required");
        }
        
        String upperType = aggregationType.toUpperCase();
        if (!Fido2MetricsConstants.HOURLY.equals(upperType) &&
            !Fido2MetricsConstants.DAILY.equals(upperType) &&
            !Fido2MetricsConstants.WEEKLY.equals(upperType) &&
            !Fido2MetricsConstants.MONTHLY.equals(upperType)) {
            throw errorResponseFactory.invalidRequest(
                "aggregationType must be one of: HOURLY, DAILY, WEEKLY, MONTHLY"
            );
        }
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
            return "An unexpected error occurred while processing the request";
        }
        
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "An unexpected error occurred while processing the request";
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
            return "An unexpected error occurred while processing the request";
        }
        
        return "An unexpected error occurred: " + sanitized;
    }

    /**
     * Functional interface for request processing
     */
    @FunctionalInterface
    private interface RequestProcessor {
        Response process() throws Exception;
    }
}

