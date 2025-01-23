package io.jans.configapi.plugin.lock.service;

import io.jans.configapi.core.util.Jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.exception.ApiApplicationException;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.configapi.plugin.lock.util.LockClientFactory;
import io.jans.model.net.HttpServiceResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class LockService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    @Inject
    Logger logger;

    @Inject
    ConfigHttpService configHttpService;

    @Inject
    LockClientFactory lockClientFactory;

    public JsonNode getStat(String url, String token, String month, String startMonth, String endMonth, String format)
            throws ApiApplicationException, JsonProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "LockStatResource::getStatistics() - url:{}, token:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                    escapeLog(format));
        }
        logger.error("LockStatResource::getStatistics() - url:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                url, month, startMonth, endMonth, format);
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
        logger.error(" stat httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.error(
                    " stat httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            jsonNode = getResponseJsonNode(httpServiceResponse);
        }
        logger.error(" stat jsonNode:{}", jsonNode);
        return jsonNode;
    }

    public JsonNode getStatData(String url, String token, String month, String startMonth, String endMonth,
            String format) throws JsonProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "LockStatResource::getStatistics() - url:{}, token:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                    escapeLog(format));
        }
        logger.error("LockStatResource::getStatistics() - url:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                url, month, startMonth, endMonth, format);
        JsonNode jsonNode = null;

        Response response = LockClientFactory.getStat(url, token, month, startMonth, endMonth, format);
        logger.error(" stat response:{}", response);
        if (response != null) {
            logger.error(" stat response.getStatus():{}, response.getEntity():{}", response.getStatus(),
                    response.getEntity());
            String jsonString = response.getEntity().toString();
            jsonNode = getResponseJsonNode(jsonString);
        }
        logger.error(" stat jsonNode:{}", jsonNode);
        return jsonNode;
    }

    public JsonNode getResponseJsonNode(HttpServiceResponse serviceResponse)
            throws ApiApplicationException, JsonProcessingException {
        JsonNode jsonNode = null;

        if (serviceResponse == null) {
            return jsonNode;
        }

        return getResponseJsonNode(getResponseEntityString(serviceResponse));
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse) throws ApiApplicationException {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }
        HttpResponse httpResponse = serviceResponse.getHttpResponse();
        if (httpResponse != null) {
            HttpEntity entity = httpResponse.getEntity();
            logger.debug("entity:{}, httpResponse.getStatusLine().getStatusCode():{}", entity,
                    httpResponse.getStatusLine().getStatusCode());
            if (entity == null) {
                return jsonString;
            }
            try {
                jsonString = entity.toString();
                logger.error(" stat jsonString:{}", jsonString);

                jsonString = EntityUtils.toString(entity, "UTF-8");
                logger.error("\n\n New jsonString:{}", jsonString);
            } catch (Exception ex) {
                logger.error("Error while getting entity using EntityUtils is ", ex);
            }
        }

        if (httpResponse.getStatusLine().getStatusCode() == Status.OK.getStatusCode()) {
            return jsonString;
        } else {
            throw new ApiApplicationException(httpResponse.getStatusLine().getStatusCode(), jsonString);
        }
    }

    public JsonNode getResponseJsonNode(String jsonSring) throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (StringUtils.isBlank(jsonSring)) {
            return jsonNode;
        }

        return Jackson.asJsonNode(jsonSring);
    }

}
