package io.jans.configapi.plugin.fido2.service;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.util.Jackson;

import io.jans.configapi.plugin.fido2.util.Fido2Util;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.fido2.model.metric.Fido2MetricsAggregation;

import io.jans.orm.model.PagedResult;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;

@Singleton
public class Fido2MetricsService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String METRICS_ENTRY_BASE_DN = "ou=fido2-metrics,o=jans";
    private static final String FIDO2_METRICS_BASE_URL = "/jans-fido2/restv1/metrics";
    private static final String FIDO2_METRICS_ENTRIES_URL = "/entries";

    @Inject
    Logger log;

    @Inject
    Fido2Util fido2Util;

    public String getFido2MetricsUrl() {
        return fido2Util.getIssuer() + FIDO2_METRICS_BASE_URL;
    }

    public String getFido2MetricsEntriesUrl() {
        return getFido2MetricsUrl() + FIDO2_METRICS_ENTRIES_URL;
    }

    public String getFido2UserMetricsEntriesUrl() {
        return getFido2MetricsUrl() + "/entries/user";
    }

    public String getMetricsEntriesByOperationUrl() {
        return getFido2MetricsUrl() + "/entries/operation";
    }

    public String getMetricsAggregationsUrl() {
        return getFido2MetricsUrl() + "/aggregations";
    }

    public String getMetricsAnalyticsUrl() {
        return getFido2MetricsUrl() + "/analytics";
    }

    public String getMetricsAnalyticsPerformanceUrl() {
        return getFido2MetricsUrl() + "/analytics/performance";
    }

    public String getDeviceAnalyticsUrl() {
        return getFido2MetricsUrl() + "/analytics/devices";
    }

    public String getErrorAnalysisUrl() {
        return getFido2MetricsUrl() + "/analytics/errors";
    }

    public String getTrendAnalysisUrl() {
        return getFido2MetricsUrl() + "/analytics/trends";
    }

    public String getPeriodOverPeriodComparisonUrl() {
        return getFido2MetricsUrl() + "/analytics/comparison";
    }

    public String getMetricsConfigUrl() {
        return getFido2MetricsUrl() + "/config";
    }

    public String getMetricsHealthUrl() {
        return getFido2MetricsUrl() + "/health";
    }

    public String getBaseDnForFido2MetricsEntry() {
        return METRICS_ENTRY_BASE_DN;
    }

    /**
     * Get raw metrics entries within a time range
     * 
     * @param startTime Start time in ISO format (e.g., 2024-01-01T00:00:00)
     * @param endTime   End time in ISO format
     * @return List of metrics entries
     */
    public PagedResult<Fido2MetricsEntry> getFido2MetricsEntries(String token, LocalDateTime startTime,
            LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get Fido2 metrics entries with startTime:{}, endTime:{}", escapeLog(startTime),
                    escapeLog(endTime));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getFido2MetricsEntriesUrl(), headers, data);

    }

    /**
     * Get metrics entries for a specific user
     * 
     * @param userId    User ID
     * @param startTime Start time in ISO format
     * @param endTime   End time in ISO format
     * @return List of user-specific metrics entries
     */
    public PagedResult<Fido2MetricsEntry> getFido2UserMetricsEntries(String token, String userId,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get Fido2 Metrics by user - userId:{}, startTime:{}, endTime:{}", escapeLog(userId),
                    escapeLog(startTime), escapeLog(endTime));
        }
        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getFido2UserMetricsEntriesUrl() + "/" + userId,
                headers, data);

    }

    /**
     * Get metrics entries by operation type (REGISTRATION or AUTHENTICATION)
     * 
     * @param operationType Operation type
     * @param startTime     Start time in ISO format
     * @param endTime       End time in ISO format
     * @return List of operation-specific metrics entries
     */
    public PagedResult<Fido2MetricsEntry> getMetricsEntriesByOperation(String token, String operationType,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info(" Get Fido2 metrics entries by operationType with - operationType:{}, startTime:{}, endTime:{}",
                    escapeLog(operationType), escapeLog(startTime), escapeLog(endTime));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class,
                this.getMetricsEntriesByOperationUrl() + "/" + operationType, headers, data);

    }

    /**
     * Get aggregated metrics data
     * 
     * @param aggregationType Aggregation type (HOURLY, DAILY, WEEKLY, MONTHLY)
     * @param startTime       Start time in ISO format
     * @param endTime         End time in ISO format
     * @return List of aggregated metrics
     */
    public PagedResult<Fido2MetricsAggregation> getFido2MetricsAggregation(String token, String aggregationType,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get Fido2 aggregated metrics - aggregationType:{}, startTime:{}, endTime:{}",
                    escapeLog(aggregationType), escapeLog(startTime), escapeLog(endTime));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getFido2MetricsPagedResult(Fido2MetricsAggregation.class,
                this.getMetricsAggregationsUrl() + "/" + aggregationType, headers, data);
    }

    /**
     * Get aggregation summary statistics
     * 
     * @param aggregationType Aggregation type (HOURLY, DAILY, WEEKLY, MONTHLY)
     * @param startTime       Start time in ISO format
     * @param endTime         End time in ISO format
     * @return Summary statistics
     */
    public JsonNode getFido2MetricsAggregationSummary(String token, String aggregationType, LocalDateTime startTime,
            LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get aggregation summary statistics - aggregationType:{}, startTime:{}, endTime:{}",
                    escapeLog(aggregationType), escapeLog(startTime), escapeLog(endTime));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getMetricsData(this.getMetricsAggregationsUrl() + "/" + aggregationType + "/summary", headers, data);
    }

    /**
     * Get user adoption metrics
     * 
     * @param startTime Start time in ISO format
     * @param endTime   End time in ISO format
     * @return User adoption statistics
     */
    public JsonNode getAdoptionMetrics(String token, LocalDateTime startTime, LocalDateTime endTime)
            throws JsonProcessingException {
        return getAnalyticsMetrics(token, this.getMetricsAnalyticsUrl() + "/adoption", startTime, endTime);
    }

    /**
     * Get performance metrics (average durations, success rates)
     * 
     * @param startTime Start time in ISO format
     * @param endTime   End time in ISO format
     * @return Performance statistics
     */
    public JsonNode getPerformanceMetrics(String token, LocalDateTime startTime, LocalDateTime endTime)
            throws JsonProcessingException {
        return getAnalyticsMetrics(token, this.getMetricsAnalyticsPerformanceUrl(), startTime, endTime);
    }

    /**
     * Get device analytics (platform distribution, authenticator types)
     * 
     * @param startTime Start time in ISO format
     * @param endTime   End time in ISO format
     * @return Device analytics data
     */
    public JsonNode getDeviceAnalytics(String token, LocalDateTime startTime, LocalDateTime endTime)
            throws JsonProcessingException {
        return getAnalyticsMetrics(token, this.getDeviceAnalyticsUrl(), startTime, endTime);
    }

    /**
     * Get error analysis (error categories, frequencies)
     * 
     * @param startTime Start time in ISO format
     * @param endTime   End time in ISO format
     * @return Error analysis data
     */
    public JsonNode getErrorAnalysis(String token, LocalDateTime startTime, LocalDateTime endTime)
            throws JsonProcessingException {
        return getAnalyticsMetrics(token, this.getErrorAnalysisUrl(), startTime, endTime);
    }

    /**
     * Get trend analysis for metrics over time
     * 
     * @param aggregationType Aggregation type for trend analysis
     * @param startTime       Start time in ISO format
     * @param endTime         End time in ISO format
     * @return Trend analysis data
     */
    public JsonNode getTrendAnalysis(String token, String aggregationType, LocalDateTime startTime,
            LocalDateTime endTime) throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get Fido2 metrics trend analysis with - aggregationType:{}, startTime:{}, endTime:{}",
                    escapeLog(aggregationType), escapeLog(startTime), escapeLog(endTime));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Request data
        Map<String, String> data = buildTimeRange(startTime, endTime);

        return getMetricsData(this.getTrendAnalysisUrl() + "/" + aggregationType, headers, data);
    }

    /**
     * Get period-over-period comparison
     * 
     * @param aggregationType Aggregation type for comparison
     * @param periods         Number of periods to compare (default: 2)
     * @return Period comparison data
     */
    public JsonNode getPeriodOverPeriodComparison(String token, String aggregationType, int periods)
            throws JsonProcessingException {
        if (log.isInfoEnabled()) {
            log.info("Get period-over-period comparison - aggregationType:{}, periods:{}", escapeLog(aggregationType),
                    escapeLog(periods));
        }

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("periods", String.valueOf(periods));

        return getMetricsData(this.getPeriodOverPeriodComparisonUrl() + "/" + aggregationType, headers, data);
    }

    /**
     * Get metrics configuration and status
     * 
     * @return Configuration information
     */
    public JsonNode getMetricsConfig(String token) throws JsonProcessingException {
        log.info("Get metrics configuration");

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        return getMetricsData(this.getMetricsConfigUrl(), headers, null);
    }

    /**
     * Health check endpoint for metrics service Verifies that the metrics service
     * is functional and can connect to the database
     * 
     * @return Health status
     */
    public JsonNode getMetricsHealth(String token) throws JsonProcessingException {
        log.info("Verify health check endpoint for metrics service");

        // Request headers
        Map<String, String> headers = buildHeaders(token);

        return getMetricsData(this.getMetricsHealthUrl(), headers, null);
    }

    // ========== HELPER METHODS ==========
    private <T> PagedResult<T> getFido2MetricsPagedResult(Class<T> type, String url, Map<String, String> headers,
            Map<String, String> data) throws JsonProcessingException {
        log.info("Fido2MetricsEntryList Data: type:{}, url:{}, headers:{}, data:{}", type, url, headers, data);

        JsonNode jsonNode = getMetricsData(url, headers, data);
        log.info("Fido2MetricsEntries Data: jsonNode:{}", jsonNode);

        PagedResult<T> pagedResultEntry = null;
        List<T> fido2MetricsList = null;

        if (jsonNode != null) {
            fido2MetricsList = Jackson.readList(jsonNode.toPrettyString(), type);
            log.debug("Fido2MetricsEntry Two fido2MetricsList:{}", fido2MetricsList);
        }

        pagedResultEntry = new PagedResult<>();
        pagedResultEntry.setEntries(fido2MetricsList);
        pagedResultEntry.setTotalEntriesCount(
                (fido2MetricsList == null || fido2MetricsList.isEmpty()) ? 0 : fido2MetricsList.size());

        log.info("Fido2MetricsEntry Three pagedResultEntry:{}", pagedResultEntry);

        return pagedResultEntry;
    }

    private JsonNode getAnalyticsMetrics(String token, String url, LocalDateTime startTime, LocalDateTime endTime)
            throws JsonProcessingException {
        Map<String, String> headers = buildHeaders(token);
        Map<String, String> data = buildTimeRange(startTime, endTime);
        return getMetricsData(url, headers, data);
    }

    private JsonNode getMetricsData(String url, Map<String, String> headers, Map<String, String> data)
            throws JsonProcessingException {
        log.debug("Fido2Metrics Data: url:{}, headers:{}, data:{}", url, headers, data);
        JsonNode jsonNode = null;

        if (StringUtils.isBlank(url)) {
            throw new WebApplicationException("Error while getting Metrics Data",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        jsonNode = fido2Util.executeGetRequest(url, headers, data);
        log.info("Fido2Metrics Data: jsonNode:{}", jsonNode);

        return jsonNode;
    }

    private Map<String, String> buildHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }
        return headers;
    }

    private Map<String, String> buildTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, String> data = new HashMap<>();
        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }
        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }
        return data;
    }

}
