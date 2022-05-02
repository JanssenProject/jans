/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.model.configuration.CorsConfiguration;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@WebFilter(filterName = "CorsFilter", asyncSupported = true, urlPatterns = { "*" })
public class CorsFilter implements Filter {

    @Inject
    private Logger log;

    @Inject
    private CorsConfiguration corsConfiguration;

    private static final Pattern COMMA_SEPARATED_SPLIT_REGEX = Pattern.compile("\\s*,\\s*");
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /**
     * The Access-Control-Allow-Headers header indicates, as part of the response to
     * a preflight request, which header field names can be used during the actual
     * request.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.debug("CorsFilter::doFilter() - corsConfiguration:{}", corsConfiguration);

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader("Origin");
        log.debug("CorsFilter::doFilter() - origin:{}", origin);
        if (corsConfiguration == null || StringUtils.isBlank(origin) || !corsConfiguration.isEnabled()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Method check
        final String requestedMethods = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
        log.debug("CorsFilter::doFilter() - requestedMethods:{}", requestedMethods);

        // Process Methods
        processMethods(response, requestedMethods);

        // Header check
        final String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
        log.debug("CorsFilter::doFilter() -  requestedHeaders:{}", requestedHeaders);
        if (StringUtils.isNotBlank(requestedHeaders)) {
            processRequestedHeaders(response, requestedHeaders);
        }

        // Origin check
        log.debug("CorsFilter::doFilter() - corsConfiguration.getAllowedOrigins():{} ",
                corsConfiguration.getAllowedOrigins());
        boolean allowOrigin = (corsConfiguration.getAllowedOrigins() == null
                || corsConfiguration.getAllowedOrigins().isEmpty() || corsConfiguration.isOriginAllowed(origin));
        log.debug("CorsFilter::doFilter() - allowOrigin:{} ", allowOrigin);
        if (allowOrigin) {
            log.debug("CorsFilter::doFilter() - setting allowOrigin");
            response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        // allowCredentials check
        log.debug("CorsFilter::doFilter() - corsConfiguration.isSupportsCredentials():{} ",
                corsConfiguration.isSupportsCredentials());
        boolean allowCredentials = corsConfiguration.isSupportsCredentials()
                || (corsConfiguration.getAllowedOrigins() != null && corsConfiguration.isOriginAllowed(origin)
                        && !corsConfiguration.getAllowedOrigins().contains("*"));

        log.debug("CorsFilter::doFilter() - allowCredentials:{} ", allowCredentials);
        response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));

        // exposedHeaders check
        log.debug("CorsFilter::doFilter() - corsConfiguration.getExposedHeaders():{}",
                corsConfiguration.getExposedHeaders());
        final Collection<String> exposedHeaders = corsConfiguration.getExposedHeaders();
        log.debug("CorsFilter::doFilter() - exposedHeaders:{}", exposedHeaders);
        if (exposedHeaders != null && !exposedHeaders.isEmpty()) {
            log.debug("CorsFilter::doFilter() - setting exposedHeaders ");
            response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", exposedHeaders));
        }

        log.debug("CorsFilter::doFilter() - request.getMethod():{} , corsConfiguration.getPreflightMaxAge():{}",
                request.getMethod(), corsConfiguration.getPreflightMaxAge());
        if ("OPTIONS".equals(request.getMethod())) {
            log.debug("CorsFilter::doFilter() - OPTIONS method");
            if ((requestedHeaders != null || requestedMethods != null) && corsConfiguration.getPreflightMaxAge() != 0) {
                log.debug("CorsFilter::doFilter() - setting PreflightMaxAge ");
                response.addHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(corsConfiguration.getPreflightMaxAge()));
            }
        } else {
            log.debug("CorsFilter::doFilter() - chaining request ");
            filterChain.doFilter(servletRequest, servletResponse);
        }

    }

    private void processRequestedHeaders(HttpServletResponse response, String allowHeadersValue) {
        log.debug(
                " CorsFilter::processRequestedHeaders() - allowHeadersValue:{} , corsConfiguration.getAllowedHttpHeaders():{}",
                allowHeadersValue, corsConfiguration.getAllowedHttpHeaders());

        if (corsConfiguration.getAllowedHttpHeaders() == null || corsConfiguration.getAllowedHttpHeaders().isEmpty()) {
            response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            List<String> requestedHeaders = new ArrayList<>();
            for (String requestedHeader : COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue)) {
                requestedHeaders.add(requestedHeader.toLowerCase());
            }

            log.debug(
                    "CorsFilter::processRequestedHeaders() - requestedHeaders:{}, corsConfiguration.getAllowedHttpHeaders():{}",
                    requestedHeaders, corsConfiguration.getAllowedHttpHeaders());
            List<String> validRequestedHeaders = new ArrayList<>();
            for (String configHeader : corsConfiguration.getAllowedHttpHeaders()) {
                log.debug("CorsFilter::processRequestedHeaders() - configHeader:{}", configHeader);
                if (requestedHeaders.contains(configHeader.toLowerCase())) {
                    validRequestedHeaders.add(configHeader);
                }
            }
            log.debug("CorsFilter::processRequestedHeaders() - validRequestedHeaders:{}", validRequestedHeaders);
            if (!validRequestedHeaders.isEmpty()) {
                response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", validRequestedHeaders));
            }
        }
    }

    private void processMethods(HttpServletResponse response, String allowMethodsValue) {
        log.debug(
                " CorsFilter::processMethods() - allowMethodsValue:{} , corsConfiguration.getAllowedHttpMethods():{} ",
                allowMethodsValue, corsConfiguration.getAllowedHttpMethods());

        if (StringUtils.isBlank(allowMethodsValue)) {
            return;
        }

        if (corsConfiguration.getAllowedHttpMethods() == null || corsConfiguration.getAllowedHttpMethods().isEmpty()) {

            response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {

            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                log.debug(" CorsFilter::processMethods() - requestedMethod:{}", requestedMethod);
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            log.debug(
                    " CorsFilter::processMethods() - requestedMethods:{} , corsConfiguration.getAllowedHttpMethods():{}",
                    requestedMethods, corsConfiguration.getAllowedHttpMethods());

            List<String> validRequestedMethods = new ArrayList<>();
            for (String configMethod : corsConfiguration.getAllowedHttpMethods()) {
                log.debug(" CorsFilter::processMethods() - configMethod:{}", configMethod);
                if (requestedMethods.contains(configMethod.toLowerCase())) {
                    log.debug(" CorsFilter::processMethods() - validRequestedMethods");
                    validRequestedMethods.add(configMethod);
                }
            }
            log.debug(" CorsFilter::processMethods() - validRequestedMethods:{}", validRequestedMethods);

            if (!validRequestedMethods.isEmpty()) {
                response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, String.join(",", validRequestedMethods));
            }
        }
    }

}
