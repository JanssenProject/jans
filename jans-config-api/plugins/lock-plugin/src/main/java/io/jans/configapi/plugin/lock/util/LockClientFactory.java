/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.util;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.client.JwkResponse;
import io.jans.as.client.RevokeSessionResponse;
import io.jans.as.client.RevokeSessionRequest;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwk.JSONWebKeySet;

import static io.jans.as.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

import io.jans.configapi.core.util.Jackson;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LockClientFactory {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static Logger log = LoggerFactory.getLogger(LockClientFactory.class);

    public static Response getStatResponse(String url, String token, String month, String startMonth, String endMonth,
            String format) {
        if (log.isDebugEnabled()) {
            log.debug("Stat Response Token - url:{}, token:{}, month:{}, startMonth:{}, endMonth:{}, format:{} ",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                    escapeLog(format));
        }
        log.error("Stat Response Token - url:{}, token:{}, month:{}, startMonth:{}, endMonth:{}, format:{} ",
                escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                escapeLog(format));
        ResteasyWebTarget webTarget = (ResteasyWebTarget) ClientBuilder.newClient().target(url);
        webTarget.property(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        webTarget.property(AUTHORIZATION, token);
        
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        if (StringUtils.isNotBlank(month)) {
            webTarget.queryParam("month", month);
        }
        if (StringUtils.isNotBlank(month)) {
            webTarget.queryParam("start-month", startMonth);
        }
        if (StringUtils.isNotBlank(month)) {
            webTarget.queryParam("end-month", endMonth);
        }
        if (StringUtils.isNotBlank(month)) {
            webTarget.queryParam("format", format);
        }

        
        Response response = webTarget.request().accept(MediaType.APPLICATION_JSON).get();
        log.error("response:{}",response);
        return response;
    }

    
    public static Response getStat(String url, String token, String month, String startMonth, String endMonth,
            String format) {
        if (log.isDebugEnabled()) {
            log.debug("Stat Response Token - url:{}, token:{}, month:{}, startMonth:{}, endMonth:{}, format:{} ",
                    escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                    escapeLog(format));
        }
        log.error("Stat Response Token - url:{}, token:{}, month:{}, startMonth:{}, endMonth:{}, format:{} ",
                escapeLog(url), escapeLog(token), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth),
                escapeLog(format));
        

        
        Map<String, String> parameters = new HashMap<>();
        if (StringUtils.isNotBlank(month)) {
            parameters.put("month", month);
        }
        if (StringUtils.isNotBlank(month)) {
            parameters.put("start-month", startMonth);
        }
        if (StringUtils.isNotBlank(month)) {
            parameters.put("end-month", endMonth);
        }
        if (StringUtils.isNotBlank(month)) {
            parameters.put("format", format);
        }

        if (!parameters.isEmpty()) {
            StringBuilder query = new StringBuilder();
            int i = 0;
            for (String key : parameters.keySet()) {

                String value = parameters.get(key);
                if (value != null && value.length() > 0) {
                    String delim = (i == 0) ? "?" : "&" + URLEncoder.encode(key, StandardCharsets.UTF_8) + "=";
                    query.append(delim);
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            }
            url = url + query.toString();
            log.error("\n\n ******** Final url:{}", url);

        }
        Builder clientRequest = getClientBuilder(url);
        clientRequest.header(CONTENT_TYPE, MediaType.APPLICATION_JSON);
        clientRequest.header(AUTHORIZATION, token);
        clientRequest.accept(CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Response response = clientRequest.get();
        log.error("response:{}",response);
        return response;
    }

   
    private static Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }
 
}
