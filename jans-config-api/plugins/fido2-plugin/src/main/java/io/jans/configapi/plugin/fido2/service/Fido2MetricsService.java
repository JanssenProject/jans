package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.ClientFactory;
import io.jans.configapi.plugin.fido2.util.Fido2Util;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.fido2.model.metric.Fido2MetricsAggregation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import static io.jans.as.model.util.Util.escapeLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class Fido2MetricsService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String METRICS_ENTRY_BASE_DN = "ou=fido2-metrics,o=jans";
    private static final String METRICS_AGGREGATION_BASE_DN = "ou=fido2-aggregations,o=jans";
    private static final String FIDO2_METRICS_BASE_URL = "/jans-fido2/restv1/metrics";
    private static final String FIDO2_METRICS_ENTRIES_URL = "/entries";

    @Inject
    Logger log;

    @Inject
    Fido2Util fido2Util;

    @Inject
    private PersistenceEntryManager entryManager;

    public String getFido2MetricsUrl() {
        return fido2Util.getIssuer() + FIDO2_METRICS_BASE_URL;
    }

    public String getFido2MetricsEntriesUrl() {
        return getFido2MetricsUrl() + FIDO2_METRICS_ENTRIES_URL;
    }

    public String getFido2UserMetricsEntriesUrl() {
        return getFido2MetricsUrl() + "/entries/user/";
    }

    public String getMetricsEntriesByOperationUrl() {
        return getFido2MetricsUrl() + "/entries/operation/";
    }
    
    public String getMetricsAggregationsUrl() {
        return getFido2MetricsUrl() + "/aggregations/";
    }
    
    public String getMetricsAnalyticsUrl() {
        return getFido2MetricsUrl() + "/analytics/";
    }

    public String getBaseDnForFido2MetricsEntry() {
        return METRICS_ENTRY_BASE_DN;
    }

    public PagedResult<Fido2MetricsEntry> searchFido2MetricsEntries(String token, LocalDateTime startTime,
            LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2MetricsEntry with token:{}, startTime:{}, endTime:{}", token, startTime, endTime);

        PagedResult<Fido2MetricsEntry> pagedResultEntry = null;

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getFido2MetricsEntriesUrl(), headers, data);

    }

    public PagedResult<Fido2MetricsEntry> searchFido2UserMetricsEntries(String token, String userId,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2UserMetricsEntry with - token:{}, userId:{}, startTime:{}, endTime:{}", token,
                userId, startTime, endTime);

        PagedResult<Fido2MetricsEntry> pagedResultEntry = null;

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (StringUtils.isNotBlank(userId)) {
            data.put("userId", userId);
        }

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getFido2UserMetricsEntriesUrl(), headers, data);

    }

    public PagedResult<Fido2MetricsEntry> searchMetricsEntriesByOperation(String token, String operationType,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2UserMetricsEntry with - token:{}, operationType:{}, startTime:{}, endTime:{}",
                token, operationType, startTime, endTime);

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (StringUtils.isNotBlank(operationType)) {
            data.put("operationType", operationType);
        }

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getFido2UserMetricsEntriesUrl(), headers, data);

    }
    
    public PagedResult<Fido2MetricsAggregation> searchFido2MetricsAggregation(String token, String aggregationType,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2UserMetricsEntry with - token:{}, aggregationType:{}, startTime:{}, endTime:{}",
                token, aggregationType, startTime, endTime);

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (StringUtils.isNotBlank(aggregationType)) {
            data.put("aggregationType", aggregationType);
        }

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsAggregation.class, this.getMetricsAggregationsUrl(), headers, data);
    }
    
    public PagedResult<Fido2MetricsEntry> searchFido2MetricsAggregationSummary(String token, String aggregationType,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2UserAggregation with - token:{}, aggregationType:{}, startTime:{}, endTime:{}",
                token, aggregationType, startTime, endTime);

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (StringUtils.isNotBlank(aggregationType)) {
            data.put("aggregationType", aggregationType);
        }

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getMetricsAggregationsUrl()+"/aggregationType/summary", headers, data);
    }
    
    public Map<String, Object> getUserAdoptionMetrics(String token, LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search UserAdoptionMetrics with - token:{}, startTime:{}, endTime:{}",
                token, startTime, endTime);

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        //TO_DO
        //return getFido2MetricsPagedResult(this.getMetricsAnalyticsUrl()+"/adoption", headers, data);
        return null;
    }
    
    public PagedResult<Fido2MetricsEntry> getPerformanceMetrics(String token, LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search UserAdoptionMetrics with - token:{}, startTime:{}, endTime:{}",
                token, startTime, endTime);

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Parameter
        Map<String, String> data = new HashMap<>();

        if (startTime != null) {
            data.put("startTime", startTime.toString());
        }

        if (endTime != null) {
            data.put("endTime", endTime.toString());
        }

        return getFido2MetricsPagedResult(Fido2MetricsEntry.class, this.getMetricsAnalyticsUrl()+"/adoption", headers, data);
    }


    private <T> PagedResult<T> getFido2MetricsPagedResult(Class<T> type, String url, Map<String, String> headers,
            Map<String, String> data) throws JsonProcessingException {
        log.error("\n\n Fido2MetricsEntryList Data: type:{}, url:{}, headers:{}, data:{}", type, url, headers, data);
        JsonNode jsonNode = getMetricsData(url, headers, data);
        log.error("\n\n Fido2MetricsEntries Data: jsonNode:{}", jsonNode);

        PagedResult<T> pagedResultEntry = null;
        List<T> fido2MetricsList = null;

        if (jsonNode != null) {
            fido2MetricsList = Jackson.readStringValue(jsonNode.toPrettyString(), List.class);
            log.error("\n\n ** Fido2MetricsEntry One fido2MetricsList:{}", fido2MetricsList);

            fido2MetricsList = Jackson.readList(jsonNode.toPrettyString(),type);
            log.error("\n\n ** Fido2MetricsEntry Two fido2MetricsList:{}", fido2MetricsList);
        }

        pagedResultEntry = new PagedResult<>();
        pagedResultEntry.setEntries(fido2MetricsList);
        pagedResultEntry.setTotalEntriesCount((fido2MetricsList == null || fido2MetricsList.size() <= 0) ? 0
                : fido2MetricsList.size());

        log.error("\n\n **Fido2MetricsEntry Three pagedResultEntry:{}", pagedResultEntry);

        return pagedResultEntry;
    }

    private JsonNode getMetricsData(String url, Map<String, String> headers, Map<String, String> data)
            throws JsonProcessingException {

        log.error("\n\n Fido2Metrics Data: url:{}, headers:{}, data:{}", url, headers, data);
        JsonNode jsonNode = null;

        if (StringUtils.isBlank(url)) {
            throw new WebApplicationException("Error while getting Metrics Data",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        jsonNode = fido2Util.executeGetRequest(url, headers, data);
        log.error("\n\n Fido2Metrics Data: jsonNode:{}", jsonNode);

        return jsonNode;
    }

}
