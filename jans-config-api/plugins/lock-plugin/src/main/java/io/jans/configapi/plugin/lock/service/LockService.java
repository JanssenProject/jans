package io.jans.configapi.plugin.lock.service;

import io.jans.configapi.core.util.Jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.model.net.HttpServiceResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;

@ApplicationScoped
public class LockService {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    @Inject
    Logger logger;

    @Inject
    ConfigHttpService configHttpService;

    public JsonNode getStat(String url, String token, String month, String startMonth, String endMonth, String format)
            throws JsonProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "LockStatResource::getStatistics() - url:{}, token:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth), escapeLog(format));
        }
        JsonNode jsonNode = null;
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token)) {
            headers.put(AUTHORIZATION, token);
        }

        // Query Param
        Map<String, String> data = new HashMap<>();
        data.put("month", month);
        data.put("start-month", startMonth);
        data.put("end-month", endMonth);
        data.put("format", format);
        HttpServiceResponse httpServiceResponse = configHttpService.executeGet(url, headers, data);
        logger.info(" stat httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            jsonNode = getResponseJsonNode(httpServiceResponse, Status.OK);
        }
        logger.info(" stat jsonNode:{}", jsonNode);
        return jsonNode;
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse, Status status) {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }

        if (serviceResponse.getHttpResponse() != null && serviceResponse.getHttpResponse().getStatusLine() != null
                && serviceResponse.getHttpResponse().getStatusLine().getStatusCode() == status.getStatusCode()) {
            HttpEntity entity = serviceResponse.getHttpResponse().getEntity();
            if (entity == null) {
                return jsonString;
            }
            jsonString = entity.toString();

        }
        logger.info(" stat jsonString:{}", jsonString);
        return jsonString;
    }

    public JsonNode getResponseJsonNode(HttpServiceResponse serviceResponse, Status status)
            throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (serviceResponse == null) {
            return jsonNode;
        }

        return getResponseJsonNode(getResponseEntityString(serviceResponse, status));
    }

    public JsonNode getResponseJsonNode(String jsonSring) throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (StringUtils.isNotBlank(jsonSring)) {
            return jsonNode;
        }

        return Jackson.asJsonNode(jsonSring);
    }

}
