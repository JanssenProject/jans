/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.test.service;

import io.jans.util.StringHelper;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResteasyService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    protected transient Logger logger = LogManager.getLogger(getClass());

    public Builder getClientBuilder(String url) {
        return ClientBuilder.newClient().target(url).request();
    }

    public Response executeGet(final String url, final Map<String, String> headers,
            final Map<String, String> parameters) {
        logger.error("\n\n\n *** Execut GET - url:{}, headers:{}, parameters:{}", url, headers, parameters);
        StringBuilder query = null;
        if (parameters != null && !parameters.isEmpty()) {
            query = new StringBuilder("");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String value = entry.getValue();
                if (value != null && value.length() > 0) {
                    String delim = "&" + URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=";
                    query.append(delim.substring(1));
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                }
            }
        }

        Builder request = getClientBuilder(url + query);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.header(header.getKey(), header.getValue());
            }
        }

        Response response = request.get();
        logger.error(" response:{}", response);

        return response;
    }

    public Response executeGet(final String url, final String accessToken) {
        logger.error("\n\n\n *** Execut GET - url:{}, accessToken:{}", url, accessToken);

        Builder request = getClientBuilder(url);
        if (StringUtils.isNotBlank(accessToken)) {
            request.header(AUTHORIZATION, "Bearer " + accessToken);
        }

        Response response = request.get();
        logger.error("\n\n\n response:{}", response);

        return response;
    }

    public Response executeGet(final String url, final String clientId, final String clientSecret,
            final String authType, final String authCode, final Map<String, String> parameters,
            ContentType contentType) {
        logger.info(
                "Data for executing GET request -  url:{}, clientId:{}, clientSecret:{} , authType:{}, authCode:{} , parameters:{}, contentType:{}",
                url, clientId, clientSecret, authType, authCode, parameters, contentType);

        StringBuilder query = null;
        if (parameters != null && !parameters.isEmpty()) {
            query = new StringBuilder("");
            int i = 0;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                 if (value != null && value.length() > 0) {
                    String delim = (i==0) ? "?" : "&" ;
                    query.append(delim + URLEncoder.encode(key, StandardCharsets.UTF_8) + "=");
                    query.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                    i++;
                }
            }
        }

        Builder request = getClientBuilder(url + query);
        if (contentType == null) {
            contentType = ContentType.APPLICATION_JSON;
        }
        request.header(CONTENT_TYPE, contentType);

        if (StringHelper.isNotEmpty(authCode)) {
            request.header(AUTHORIZATION, authType + authCode);
        }

        Response response = request.get();
        logger.info(" response:{}", response);

        return response;
    }
}
