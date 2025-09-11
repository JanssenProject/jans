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
import jakarta.ws.rs.core.Response.Status;

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

    
    public JsonNode revokeSsa(final String accessToken, final String jti) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke SSA parameters - jti:{}", escapeLog(jti));
        }
       
        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(AUTHORIZATION, accessToken);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("jti", jti);
        
        HttpServiceResponse httpServiceResponse = configHttpService.executeDelete(getSsaEndpoint(), headers, data);
        JsonNode jsonNode = null;
        logger.debug(" Revoke SSA httpServiceResponse:{}", httpServiceResponse);
        if (httpServiceResponse != null) {
            logger.error(
                    " Revoke SSA httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode(),
                    httpServiceResponse.getHttpResponse().getEntity());
            Status status = configHttpService.getResponseStatus(httpServiceResponse);
            
        
            logger.error("\n\n\n status:{}, status.getStatusCode():{}", status, status.getStatusCode());
            
            StringBuilder stringBuilder = configHttpService.readEntity(httpServiceResponse.getHttpResponse().getEntity());
            logger.error("\n\n\n stringBuilder:{}", stringBuilder);
            
            String jsonString = configHttpService.getContent(httpServiceResponse.getHttpResponse().getEntity());
            logger.error("\n\n\n jsonString:{}", jsonString);
            
            jsonNode = configHttpService.getResponseJsonNode(httpServiceResponse);   
            logger.error(" Revoke SSA jsonNode:{}", jsonNode);
            
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

                jsonString = configHttpService.getContent(httpEntity);

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

    

}
