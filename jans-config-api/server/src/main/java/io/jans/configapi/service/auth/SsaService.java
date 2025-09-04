/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.security.client.AuthClientFactory;
import io.jans.configapi.util.AuthUtil;
import io.jans.model.net.HttpServiceResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

@ApplicationScoped
public class SsaService {

    @Inject
    private Logger logger;

    @Inject
    AuthUtil authUtil;

    @Inject
    ConfigHttpService configHttpService;

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";

    public JsonNode getSsa(final String accessToken, final String jti, final String orgId) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("SSA search parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
        logger.error("NEW Request jti SSA List -  jti:{} , orgId:{}", jti, orgId);

        JsonNode jsonNode = null;

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(AUTHORIZATION, accessToken);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("jti", jti);
        data.put("org_id-month", orgId);
        HttpServiceResponse httpServiceResponse = configHttpService.executeGet(getSsaEndpoint(), headers, data);

        logger.info(" stat httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.info(
                    " stat httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            jsonNode = configHttpService.getResponseJsonNode(httpServiceResponse);
        }
        logger.info(" stat jsonNode:{}", jsonNode);
        return jsonNode;
    }

    public JsonNode createSsa(final String accessToken, final String jsonNode) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("SSA Create parameters - jti:{}, orgId:{}", escapeLog(jsonNode));
        }
        logger.error("Create SSA accessToken:{},  jsonNode:{}", accessToken, jsonNode);

        HttpServiceResponse httpServiceResponse = configHttpService.executePost(getSsaEndpoint(), accessToken, jsonNode,
                ContentType.APPLICATION_JSON, null);
        JsonNode jsonNodeResponse = null;

        logger.info(" stat httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.info(
                    " stat httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            jsonNodeResponse = configHttpService.getResponseJsonNode(httpServiceResponse);
        }
        logger.info(" jsonNodeResponse:{}", jsonNodeResponse);
        return jsonNodeResponse;
    }

    public JsonNode revokeSsa(final String accessToken, final String jti, final String orgId) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke SSA parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
        logger.error("Revoke SSA -  jti:{}, orgId:{} ", jti, orgId);

        JsonNode jsonNode = null;

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(AUTHORIZATION, accessToken);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("jti", jti);
        data.put("org_id", orgId);
        HttpServiceResponse httpServiceResponse = configHttpService.executeDelete(getSsaEndpoint(), headers, data);

        logger.info(" Revoke SSA httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.info(
                    " Revoke SSA httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            jsonNode = configHttpService.getResponseJsonNode(httpServiceResponse);
        }
        logger.info(" Revoke SSA jsonNode:{}", jsonNode);
        return jsonNode;

    }

    private String getSsaEndpoint() throws JsonProcessingException {
        String configurationEndpoint = authUtil.getIssuer() + ConfigHttpService.getOpenidConfigurationUrl();
        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        logger.error(" configurationEndpoint:{}", configurationEndpoint);

        HttpServiceResponse httpServiceResponse = configHttpService.executeGet(configurationEndpoint, headers, null);
        logger.debug(" httpServiceResponse:{}", httpServiceResponse);
        String jsonString = null;
        String endpoint = null;
        if (httpServiceResponse.getHttpResponse() != null
                && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

            logger.trace(
                    " \n\n FINAL  httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());

            HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
            if (httpEntity != null) {

                jsonString = getContent(httpEntity);

                endpoint = Jackson.getElement(jsonString, "ssa_endpoint");
                logger.error("endpoint:{}", endpoint);
                if (StringUtils.isBlank(endpoint)) {
                    throw new WebApplicationException("Error while fetching ssa_endpoint from configurationEndpoint {" + configurationEndpoint
                            + "} ");
                }
            }

        }
        logger.error(" Return endpoint:{}", endpoint);
        return endpoint;

    }

    private String getContent(HttpEntity httpEntity) {
        String jsonString = null;
        InputStream inputStream = null;
        try {

            if (httpEntity == null) {
                return jsonString;
            }
            inputStream = httpEntity.getContent();
            logger.error(" \n\n getContent() - httpEntity.getContentLength():{}", httpEntity.getContentLength(),
                    httpEntity.getContent());

            jsonString = IOUtils.toString(httpEntity.getContent(), StandardCharsets.UTF_8);

            logger.info("Data jsonString:{}", jsonString);

        } catch (Exception ex) {
            throw new WebApplicationException("Failed to read data '{" + httpEntity + "}'", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return jsonString;
    }

}
