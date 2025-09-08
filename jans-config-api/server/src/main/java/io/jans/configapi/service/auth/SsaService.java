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
import io.jans.configapi.util.AuthUtil;
import io.jans.model.net.HttpServiceResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
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

    
    public JsonNode revokeSsa(final String accessToken, final String jti, final String orgId) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke SSA parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
       
        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(AUTHORIZATION, accessToken);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("jti", jti);
        data.put("org_id", orgId);
        HttpServiceResponse httpServiceResponse = configHttpService.executeDelete(getSsaEndpoint(), headers, data);
        JsonNode jsonNode = null;
        logger.debug(" Revoke SSA httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.trace(
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
        logger.debug(" configurationEndpoint:{}", configurationEndpoint);

        HttpServiceResponse httpServiceResponse = configHttpService.executeGet(configurationEndpoint, headers, null);
        
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
                logger.debug("endpoint:{}", endpoint);
                if (StringUtils.isBlank(endpoint)) {
                    throw new WebApplicationException("Error while fetching ssa_endpoint from configurationEndpoint {" + configurationEndpoint
                            + "} ");
                }
            }
        }
        logger.info(" Return endpoint:{}", endpoint);
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
            logger.trace("  httpEntity.getContentLength():{}, httpEntity.getContent():{}", httpEntity.getContentLength(), httpEntity.getContent());

            jsonString = IOUtils.toString(httpEntity.getContent(), StandardCharsets.UTF_8);
            logger.debug("Data jsonString:{}", jsonString);

        } catch (Exception ex) {
            throw new WebApplicationException("Failed to read data '{" + httpEntity + "}'", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return jsonString;
    }

}
