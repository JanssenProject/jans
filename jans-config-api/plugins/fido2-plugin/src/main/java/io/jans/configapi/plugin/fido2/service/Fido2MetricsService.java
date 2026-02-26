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
import io.jans.model.SearchRequest;
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

    @Inject
    Logger log;

    @Inject
    Fido2Util fido2Util;

    @Inject
    private PersistenceEntryManager entryManager;

    public String getFido2MetricsUrl() {
        return fido2Util.getIssuer() + FIDO2_METRICS_BASE_URL;
    }

    public String getBaseDnForFido2MetricsEntry() {
        return METRICS_ENTRY_BASE_DN;
    }

    public PagedResult<Fido2MetricsEntry> searchFido2MetricsEntries(SearchRequest searchReq, String token,
            LocalDateTime startTime, LocalDateTime endTime) throws JsonProcessingException {
        log.error("**** Search Fido2MetricsEntry with searchReq:{}, token:{}, startTime:{}, endTime:{}", searchReq,
                token, startTime, endTime);

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

        List<Fido2MetricsEntry> fido2MetricsEntryList = null;
        JsonNode jsonNode = getMetricsData(this.getFido2MetricsUrl(), headers, data);
        log.error("\n\n Fido2MetricsEntries: jsonNode:{}", jsonNode);
        if (jsonNode != null) {
            fido2MetricsEntryList = Jackson.readStringValue(jsonNode.toPrettyString(), List.class);
            log.error("Fido2MetricsEntry One fido2MetricsEntryList:{}", fido2MetricsEntryList);

            fido2MetricsEntryList = Jackson.readList(jsonNode.toPrettyString(), Fido2MetricsEntry.class);
            log.error("Fido2MetricsEntry Two fido2MetricsEntryList:{}", fido2MetricsEntryList);
        }

        pagedResultEntry = new PagedResult<Fido2MetricsEntry>();
        pagedResultEntry.setTotalEntriesCount((fido2MetricsEntryList == null || fido2MetricsEntryList.size() <= 0) ? 0
                : fido2MetricsEntryList.size());

        return pagedResultEntry;

    }

    public JsonNode getMetricsData(String url, Map<String, String> headers, Map<String, String> data) {

        log.error("\n\n Fido2Metrics Data: url:{}, headers:{}, data:{}", url, headers, data);
        JsonNode jsonNode = null;
        try {

            if (StringUtils.isBlank(url)) {
                throw new WebApplicationException("Error while getting Metrics Data",
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }

            jsonNode = fido2Util.executeGetRequest(url, headers, data);
            log.error("\n\n Fido2Metrics Data: jsonNode:{}", jsonNode);

        } catch (Exception ex) {
            log.error("Fido2Metrics Exception -", ex);
            throw new WebApplicationException("Error while getting MetricsEntries",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        return jsonNode;
    }

    public List<Fido2MetricsEntry> getMetricsEntries1(LocalDateTime startTime, LocalDateTime endTime) {

        log.error("\n\n Fido2MetricsEntries: startTime:{}, endTime:{}", startTime, endTime);
        List<Fido2MetricsEntry> fido2MetricsEntryList = null;
        try {

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(FIDO2_METRICS_BASE_URL + "/entries");
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);

            Response response = request.get();
            log.error("Fido2MetricsEntries response: {}", response);

            if (response != null && response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                JsonNode jsonNode = MAPPER.readValue(entity, JsonNode.class);
                log.error("\n\n Fido2MetricsEntries: entity:{}, jsonNode:{}", entity, jsonNode);
                fido2MetricsEntryList = Jackson.readStringValue(entity, List.class);
            }
            log.error("\n\n Fido2MetricsEntries: fido2MetricsEntryList:{}", fido2MetricsEntryList);
            return fido2MetricsEntryList;

        } catch (Exception ex) {
            log.error("MetricsEntries Exception -", ex);
            throw new WebApplicationException("Error while getting MetricsEntries",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

}
