package io.jans.configapi.plugin.fido2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.plugin.fido2.util.ClientFactory;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Invocation;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIDO2_METRICS_BASE_URL = "https://pujavs-verified-hookworm.gluu.info/jans-fido2/restv1/metrics";
    
    public Response getMetricsEntries() throws Exception {
        try {
        
                Invocation.Builder request = ClientFactory.instance().getClientBuilder(FIDO2_METRICS_BASE_URL+"/entries");
                request.header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON);
                Response response = request.get();

                log.info("policy store request status code: {}", response.getStatus());

                if (response.getStatus() == 200) {
                    String entity = response.readEntity(String.class);
                    JsonNode jsonNode  = MAPPER.readValue(entity, JsonNode.class);
                    log.info("MetricsEntries: entity:{}, data:{}",  entity, jsonNode);
                }
               
                return response;
          
        } catch (Exception ex) {
            log.error("MetricsEntries Exception -", ex);
                        throw new WebApplicationException("Error while getting MetricsEntries",Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    }
