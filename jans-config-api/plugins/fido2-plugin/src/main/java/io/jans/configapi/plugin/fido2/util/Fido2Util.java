/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.configapi.core.util.AuthUtil;
import io.jans.configapi.core.util.Jackson;
import io.jans.model.net.HttpServiceResponse;
import io.jans.configapi.plugin.fido2.model.config.Fido2ConfigSource;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;

@ApplicationScoped
public class Fido2Util {

    @Inject
    Logger logger;

    @Inject
    Fido2ConfigSource fido2ConfigSource;

    @Inject
    ConfigHttpService configHttpService;

    @Inject
    AuthUtil authUtil;

    public Map<String, String> getProperties() {
        logger.debug("   Fido2Util - fido2ConfigSource.getProperties():{}", fido2ConfigSource.getProperties());
        return fido2ConfigSource.getProperties();
    }

    public Set<String> getPropertyNames() {
        logger.debug("   Fido2Util - ido2ConfigSource.getPropertyNames():{}", fido2ConfigSource.getPropertyNames());
        return fido2ConfigSource.getPropertyNames();
    }

    public String getIssuer() {
        return authUtil.getIssuer();
    }

    public JsonNode executeGetRequest(String requestUri, Map<String, String> headers, Map<String, String> data)
            throws WebApplicationException, JsonProcessingException {
        validateFido2Url(requestUri);
        return getResponseJsonNode(configHttpService.executeGet(requestUri, headers, data));
    }

    public HttpServiceResponse executeGet(String requestUri, Map<String, String> headers, Map<String, String> data) {
        validateFido2Url(requestUri);
        return configHttpService.executeGet(requestUri, headers, data);
    }

    public JsonNode getResponseJsonNode(HttpServiceResponse serviceResponse)
            throws WebApplicationException, JsonProcessingException {
        JsonNode jsonNode = null;

        if (serviceResponse == null) {
            return jsonNode;
        }

        return getResponseJsonNode(getResponseEntityString(serviceResponse), "response");
    }

    public String getResponseEntityString(HttpServiceResponse serviceResponse) throws WebApplicationException {
        String jsonString = null;

        if (serviceResponse == null) {
            return jsonString;
        }
        HttpResponse httpResponse = serviceResponse.getHttpResponse();
        if (httpResponse != null) {
            HttpEntity entity = httpResponse.getEntity();

            int statusCode = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode()
                    : Status.INTERNAL_SERVER_ERROR.getStatusCode();
            logger.debug("entity:{}, statusCode:{}", entity, statusCode);

            if (entity == null) {
                return jsonString;
            }
            try {
                jsonString = EntityUtils.toString(entity, "UTF-8");
                logger.debug(" jsonString:{}, httpResponse.getStatusLine():{},  Status.OK.getStatusCode():{}",
                        jsonString, httpResponse.getStatusLine(), Status.OK.getStatusCode());
            } catch (Exception ex) {
                logger.error("Error while getting entity using EntityUtils is ", ex);
                throw new WebApplicationException("Unable to read upstream response entity", ex);
            }

            if (statusCode == Status.OK.getStatusCode()) {
                return jsonString;
            } else {
                throw new WebApplicationException(jsonString, statusCode);
            }

        }
        return jsonString;
    }

    public JsonNode getResponseJsonNode(String jsonSring, String nodeName) throws JsonProcessingException {
        JsonNode jsonNode = null;

        if (StringUtils.isBlank(jsonSring)) {
            return jsonNode;
        }
        jsonNode = Jackson.asJsonNode(jsonSring);
        if (StringUtils.isNotBlank(nodeName) && jsonNode != null && jsonNode.get(nodeName) != null) {
            jsonNode = jsonNode.get(nodeName);
        }
        return jsonNode;
    }

    public void validateFido2Url(String url) {
        String issuer = getIssuer();
        if (StringUtils.isBlank(issuer)) {
            throw new WebApplicationException("Fido issuer is not configured", Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (!url.startsWith(issuer)) {
            throw new WebApplicationException("Invalid target URL", Response.Status.BAD_REQUEST);
        }
    }
}
