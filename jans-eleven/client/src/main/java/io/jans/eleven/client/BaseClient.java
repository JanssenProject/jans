/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

import org.apache.log4j.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.WebTarget;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public abstract class BaseClient<T extends BaseRequest, V extends BaseResponse> {

    private static final Logger LOG = Logger.getLogger(BaseClient.class);

    protected String url;
    protected BaseRequest request;
    protected BaseResponse response;
    protected WebTarget webTarget = null;
    protected Form requestForm = new Form();
    protected Response clientResponse;

    public BaseClient(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    protected void addRequestParam(String key, String value) {
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(value)) {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                requestForm.param(key, value);
            } else {
            	webTarget = webTarget.queryParam(key, value);
            }
        }
    }

    protected void addRequestParam(String key, Long value) {
        if (!Strings.isNullOrEmpty(key) && value != null) {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                requestForm.param(key, value.toString());
            } else {
            	webTarget = webTarget.queryParam(key, value);
            }
        }
    }

    protected void addRequestParam(String key, String[] value) {
        if (!Strings.isNullOrEmpty(key) && value != null) {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                requestForm.param(key, Arrays.toString(value));
            } else {
            	webTarget = webTarget.queryParam(key, Arrays.toString(value));
            }
        }
    }

    public abstract T getRequest();

    public abstract void setRequest(T request);

    public abstract V getResponse();

    public abstract void setResponse(V response);

    public abstract V exec() throws Exception;

    public String getRequestAsString() {
        StringBuilder sb = new StringBuilder();

        try {
            URL theUrl = new URL(url);

            if (getHttpMethod().equals(HttpMethod.POST)) {
                sb.append(HttpMethod.POST).append(" ").append(theUrl.getPath()).append(" HTTP/1.1");
                if (!Strings.isNullOrEmpty(request.getContentType())) {
                    sb.append("\n");
                    sb.append("Content-Type: ").append(request.getContentType());
                }
                if (!Strings.isNullOrEmpty(request.getMediaType())) {
                    sb.append("\n");
                    sb.append("Accept: ").append(request.getMediaType());
                }
                sb.append("\n");
                sb.append("Host: ").append(theUrl.getHost());

                String accessToken = request.getAccessToken();
                if (!Strings.isNullOrEmpty(accessToken)) {
                    sb.append("\n");
                    sb.append("Authorization: Bearer ").append(accessToken);
                }

                sb.append("\n");
                sb.append("\n");
                sb.append(request.getQueryString());
            } else if (getHttpMethod().equals(HttpMethod.GET)) {
                sb.append("GET ").append(theUrl.getPath());
                if (!Strings.isNullOrEmpty(request.getQueryString())) {
                    sb.append("?").append(request.getQueryString());
                }
                sb.append(" HTTP/1.1");
                sb.append("\n");
                sb.append("Host: ").append(theUrl.getHost());

                String accessToken = request.getAccessToken();
                sb.append("\n");
                sb.append("Authorization: Bearer ").append(accessToken);
            }
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
        }

        return sb.toString();
    }

    public String getResponseAsString() {
        StringBuilder sb = new StringBuilder();

        if (response != null) {
            sb.append("HTTP/1.1 ").append(response.getStatus());
            if (response.getHeaders() != null) {
                for (String key : response.getHeaders().keySet()) {
                    sb.append("\n")
                            .append(key)
                            .append(": ")
                            .append(response.getHeaders().get(key).get(0));
                }
            }
            if (response.getEntity() != null) {
                sb.append("\n");
                sb.append("\n");
                sb.append(response.getEntity());
            }
        }
        return sb.toString();
    }

    public abstract String getHttpMethod();

	public static String toPrettyJson(Object object) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}

}
