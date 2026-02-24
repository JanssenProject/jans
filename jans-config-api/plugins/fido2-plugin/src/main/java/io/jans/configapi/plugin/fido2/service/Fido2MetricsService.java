package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.jans.model.SearchRequest;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.plugin.fido2.util.ClientFactory;
import io.jans.configapi.core.util.Jackson;

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
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class Fido2MetricsService {

    @Inject
    Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIDO2_METRICS_BASE_URL = "https://pujavs-cunning-bug.gluu.info/jans-fido2/restv1/metrics";

    
    private static final String METRICS_ENTRY_BASE_DN = "ou=fido2-metrics,o=jans";
    private static final String METRICS_AGGREGATION_BASE_DN = "ou=fido2-aggregations,o=jans";

    public String getBaseDnForFido2MetricsEntry() {
        return METRICS_ENTRY_BASE_DN;
    }
    
    public PagedResult<Fido2MetricsEntry> searchFido2MetricsEntries(SearchRequest searchReq, LocalDateTime startTime, LocalDateTime endTime) {
        log.error("**** Search Fido2MetricsEntry with searchReq:{}, startTime:{}, endTime:{}", searchReq, startTime, endTime);

        
        List<Fido2MetricsEntry> fido2MetricsEntryList = getMetricsEntries(startTime, endTime);
        PagedResult<Fido2MetricsEntry> pagedResultEntry = new PagedResult<Fido2MetricsEntry>();
        pagedResultEntry.setTotalEntriesCount((fido2MetricsEntryList==null || fido2MetricsEntryList.size() <=0)?0:fido2MetricsEntryList.size());
        
        log.error("Fido2MetricsEntry pattern fido2MetricsEntryList:{}", fido2MetricsEntryList);
        return pagedResultEntry;

    }

    public List<Fido2MetricsEntry> getMetricsEntries(LocalDateTime startTime, LocalDateTime endTime) {
        
        log.error("\n\n Fido2MetricsEntries: startTime:{}, endTime:{}", startTime, endTime);
        List<Fido2MetricsEntry> fido2MetricsEntryList = null;
        try {

            MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
           // body.add("startTime", startTime);
           // body.add("endTime", endTime);
            
            Invocation.Builder request = ClientFactory.instance().getClientBuilder(FIDO2_METRICS_BASE_URL + "/entries");
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
            request.buildGet().property(AUTHORIZATION, request)
            Response response = request.get();
            log.error("Fido2MetricsEntries response: {}", response);

            if (response!=null && response.getStatus() == 200) {
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
