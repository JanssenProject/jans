/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.audit.debug;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.audit.debug.entity.HttpRequest;
import io.jans.as.server.audit.debug.entity.HttpResponse;
import io.jans.as.server.audit.debug.wrapper.RequestWrapper;
import io.jans.as.server.audit.debug.wrapper.ResponseWrapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Created by eugeniuparvan on 5/10/17.
 *
 * @author Yuriy Movchan Date: 06/09/2019
 */
@WebFilter(urlPatterns = {"/*"})
public class ServletLoggingFilter implements Filter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Instant start = now();

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("LoggingFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!BooleanUtils.toBoolean(appConfiguration.getHttpLoggingEnabled())) {
            chain.doFilter(httpRequest, httpResponse);
            return;
        }
        Set<String> excludedPaths = appConfiguration.getHttpLoggingExcludePaths();
        if (!CollectionUtils.isEmpty(excludedPaths)) {
            for (String excludedPath : excludedPaths) {
                String requestURI = httpRequest.getRequestURI();
                if (requestURI.startsWith(excludedPath)) {
                    chain.doFilter(httpRequest, httpResponse);
                    return;
                }
            }
        }

        RequestWrapper requestWrapper = new RequestWrapper(httpRequest);
        ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);

        chain.doFilter(httpRequest, httpResponse);

        Duration duration = duration(start);

        // yuriyz: log request and response only after filter handling.
        // #914 - we don't want to effect server functionality due to logging. Currently content can be messed if it is InputStream.
        if (log.isDebugEnabled()) {
            log.debug(getRequestDescription(requestWrapper, duration));
            log.debug(getResponseDescription(responseWrapper));
        }
    }

    @Override
    public void destroy() {

    }

    protected String getRequestDescription(RequestWrapper requestWrapper, Duration duration) {
        try {
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setSenderIP(requestWrapper.getLocalAddr());
            httpRequest.setMethod(requestWrapper.getMethod());
            httpRequest.setPath(requestWrapper.getRequestURI());
            httpRequest.setParams(requestWrapper.isFormPost() ? null : requestWrapper.getParameters());
            httpRequest.setHeaders(requestWrapper.getHeaders());
            httpRequest.setBody(requestWrapper.getContent());
            httpRequest.setDuration(duration.toString());
            return OBJECT_MAPPER.writeValueAsString(httpRequest);
        } catch (Exception e) {
            log.warn("Cannot serialize Request to JSON", e);
            return null;
        }
    }

    protected String getResponseDescription(ResponseWrapper responseWrapper) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatus(responseWrapper.getStatus());
        httpResponse.setHeaders(responseWrapper.getHeaders());
        try {
            return OBJECT_MAPPER.writeValueAsString(httpResponse);
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize Response to JSON", e);
            return null;
        }
    }

    public Instant now() {
        return Instant.now();
    }

    public Duration duration(Instant start) {
        Instant end = Instant.now();
        return Duration.between(start, end);
    }

    public Duration duration(Instant start, Instant end) {
        return Duration.between(start, end);
    }

}
