package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.jans.model.SearchRequest;
import io.jans.fido2.model.metric.Fido2MetricsEntry;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.plugin.fido2.util.ClientFactory;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String FIDO2_METRICS_BASE_URL = "https://pujavs-verified-hookworm.gluu.info/jans-fido2/restv1/metrics";

    public PagedResult<Fido2MetricsEntry> searchFido2Registration(SearchRequest searchRequest) {
        log.info("**** Search Fido2Registration with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                log.info(" **** Search Fido2Registration with assertionValue:{}", assertionValue);

                searchFilter = Filter.createORFilter(filters);
            }
        }
        log.debug("Fido2Registration pattern searchFilter:{}", searchFilter);

        return null;

    }

    private Response getMetricsEntries() throws Exception {
        try {

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(FIDO2_METRICS_BASE_URL + "/entries");
            request.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
            Response response = request.get();

            log.error("policy store request status code: {}", response.getStatus());

            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                JsonNode jsonNode = MAPPER.readValue(entity, JsonNode.class);
                log.error("MetricsEntries: entity:{}, data:{}", entity, jsonNode);
            }

            return response;

        } catch (Exception ex) {
            log.error("MetricsEntries Exception -", ex);
            throw new WebApplicationException("Error while getting MetricsEntries",
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

}
