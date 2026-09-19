/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.model.configuration.CorsConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
    public static final String VARY = "Vary";

    /** Literal wildcard token that may appear in the configured allow list. */
    private static final String WILDCARD = "*";

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

        // The response varies based on the request's Origin header, so caches must not
        // serve one origin's CORS response to a different origin.
        response.addHeader(VARY, "Origin");

        final Collection<String> allowedOrigins = corsConfiguration.getAllowedOrigins();
        final boolean hasExplicitAllowList = allowedOrigins != null && !allowedOrigins.isEmpty();
        final boolean wildcardConfigured = hasExplicitAllowList && allowedOrigins.contains(WILDCARD);

        // Security requirement: an explicit, non-empty allow list is mandatory.
        // A missing/empty configuration must NEVER be treated as "allow all" -
        // it must instead deny CORS for every origin.
        final boolean originAllowed = hasExplicitAllowList && !wildcardConfigured
                && corsConfiguration.isOriginAllowed(origin);

        log.debug("CorsFilter::doFilter() - hasExplicitAllowList:{}, wildcardConfigured:{}, originAllowed:{}",
                hasExplicitAllowList, wildcardConfigured, originAllowed);

        if (!wildcardConfigured && !originAllowed) {
            // Origin did not match the explicit allow list: emit no CORS headers at all.
            // Browsers will then refuse to expose the response to the calling page.
            log.debug("CorsFilter::doFilter() - origin:{} not in allow list, skipping CORS headers", origin);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Method check
        final String requestedMethods = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
        log.debug("CorsFilter::doFilter() - requestedMethods:{}", requestedMethods);
        processMethods(response, requestedMethods);

        // Header check
        final String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
        log.debug("CorsFilter::doFilter() - requestedHeaders:{}", requestedHeaders);
        if (StringUtils.isNotBlank(requestedHeaders)) {
            processRequestedHeaders(response, requestedHeaders);
        }

        final boolean supportsCredentials = corsConfiguration.isSupportsCredentials();
        log.debug("CorsFilter::doFilter() - supportsCredentials:{}", supportsCredentials);

        if (wildcardConfigured) {
            // Security requirement: "*" must never be paired with Access-Control-Allow-Credentials.
            // Browsers reject that combination anyway, but the server must not emit it either -
            // a wildcard response is only ever safe for anonymous (non-credentialed) requests.
            response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD);
            log.debug("CorsFilter::doFilter() - wildcard origin configured; Allow-Credentials will not be sent");
        } else {
            // originAllowed == true here: echo back only the exact, validated origin -
            // never an unvalidated reflection of the request's Origin header.
            response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            if (supportsCredentials) {
                response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }
        }

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