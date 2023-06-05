/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.external.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalScriptContext.class);

    private NoLogWebApplicationException webApplicationException;

    private final Map<String, Object> contextVariables;

    protected HttpServletRequest httpRequest;
    protected final HttpServletResponse httpResponse;

    public ExternalScriptContext(HttpServletRequest httpRequest) {
        this(httpRequest, null);
    }

    public ExternalScriptContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        this.contextVariables = new HashMap();
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    public Logger getLog() {
        return log;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public String getIpAddress() {
        return httpRequest != null ? httpRequest.getRemoteAddr() : "";
    }

    public Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    public NoLogWebApplicationException getWebApplicationException() {
        return webApplicationException;
    }

    public void setWebApplicationException(NoLogWebApplicationException webApplicationException) {
        this.webApplicationException = webApplicationException;
    }

    public NoLogWebApplicationException createWebApplicationException(Response response) {
        return new NoLogWebApplicationException(response);
    }

    public NoLogWebApplicationException createWebApplicationException(int status, String entity) {
        this.webApplicationException = new NoLogWebApplicationException(Response
                .status(status)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
        return this.webApplicationException;
    }

    public void throwWebApplicationExceptionIfSet() {
        if (webApplicationException != null)
            throw webApplicationException;
    }
}
