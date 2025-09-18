/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;

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

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.json.JSONObject;
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

    public JSONObject revokeSsa(final String accessToken, final String jti) throws JsonProcessingException {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke SSA parameters - jti:{}", escapeLog(jti));
        }

        if (StringUtils.isBlank(jti)) {
            throw new WebApplicationException("SSA unique identifier - JTI is required!",
                    Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        // Request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(AUTHORIZATION, accessToken);

        // Query Parameter
        Map<String, String> data = new HashMap<>();
        data.put("jti", jti);

        HttpServiceResponse httpServiceResponse = configHttpService.executeDelete(getSsaEndpoint(), headers, data);
        JSONObject jSONObject = null;

        logger.debug(" Revoke SSA httpServiceResponse:{}", httpServiceResponse);

        if (httpServiceResponse != null && httpServiceResponse.getHttpResponse() != null) {
            Status status = configHttpService.getResponseStatus(httpServiceResponse);
            jSONObject = getResponseJSONObject(httpServiceResponse);
            logger.info(" status:{}, status.getStatusCode():{}, getResponseJSONObject(httpServiceResponse):{}", status,
                    status.getStatusCode(), getResponseJSONObject(httpServiceResponse));

            if (status.getStatusCode() != Status.OK.getStatusCode()) {
                if (httpServiceResponse.getHttpResponse().getStatusLine() != null) {
                    throw new WebApplicationException(
                            httpServiceResponse.getHttpResponse().getStatusLine().getReasonPhrase(),
                            httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode());
                } else {
                    throw new WebApplicationException("Error while revoking SSA",
                            Status.INTERNAL_SERVER_ERROR.getStatusCode());

                }

            }
        }
        logger.info(" Revoke SSA response jSONObject:{}", jSONObject);
        return jSONObject;

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
                    " FINAL  httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());

            HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
            if (httpEntity != null) {

                jsonString = configHttpService.getContent(httpEntity);

                endpoint = Jackson.getElement(jsonString, "ssa_endpoint");
                logger.debug("endpoint:{}", endpoint);
                if (StringUtils.isBlank(endpoint)) {
                    throw new WebApplicationException("Error while fetching ssa_endpoint from configurationEndpoint {"
                            + configurationEndpoint + "} ");
                }
            }
        }
        logger.info(" Return endpoint:{}", endpoint);
        return endpoint;
    }

    private JSONObject getResponseJSONObject(HttpServiceResponse httpServiceResponse) {
        JSONObject jsonObj = null;

        if (httpServiceResponse == null) {
            return jsonObj;
        }
        jsonObj = new JSONObject();
        if (httpServiceResponse.getHttpResponse() != null) {

            jsonObj.put("description", httpServiceResponse.getHttpResponse().getStatusLine());
            if (httpServiceResponse.getHttpResponse().getStatusLine() != null) {
                jsonObj.put("status", httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode());
            }

        }
        logger.info(" Return jsonObj:{}", jsonObj);
        return jsonObj;
    }

}
