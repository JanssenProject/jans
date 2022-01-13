/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.server.filters.AbstractCorsFilter;
import io.jans.util.StringHelper;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.CorsConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
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
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.debug("CorsFilter::doFilter() - Entry - corsConfiguration = " + corsConfiguration);
        Collection<String> globalAllowedOrigins = null;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader("Origin");
        log.debug("CorsFilter::doFilter() - 1 - origin = " + origin + " ,corsConfiguration.isEnabled() = "
                + corsConfiguration.isEnabled());

        if ((StringUtils.isBlank(origin)) || (corsConfiguration != null && !corsConfiguration.isEnabled())) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {

            // Method check
            // final String requestedMethods = request.getMethod();
            final String requestedMethods = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD);
            log.debug("CorsFilter::doFilter() - 2 - requestedMethods = " + requestedMethods);
            if (StringUtils.isNotBlank(requestedMethods)) {
                processMethods(response, requestedMethods);
            }

            // Header check
            final String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
            log.debug("CorsFilter::doFilter() - 3 - requestedHeaders = " + requestedHeaders);
            if (StringUtils.isNotBlank(requestedHeaders)) {
                processRequestedHeaders(response, requestedHeaders);
            }

            // Origin check
            log.debug("CorsFilter::doFilter() - 4 - corsConfiguration.getAllowedOrigins() = "
                    + corsConfiguration.getAllowedOrigins());
            boolean allowOrigin = (corsConfiguration.getAllowedOrigins() == null
                    || corsConfiguration.getAllowedOrigins().size() == 0 || corsConfiguration.isOriginAllowed(origin));
            log.debug("CorsFilter::doFilter() - 4.1 - allowOrigin = " + allowOrigin);
            if (allowOrigin) {
                log.debug("CorsFilter::doFilter() - 4.2 - ");
                response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }

            // allowCredentials check
            log.debug("CorsFilter::doFilter() - 5 - corsConfiguration.isSupportsCredentials()  = "
                    + corsConfiguration.isSupportsCredentials() + " , ");
            boolean allowCredentials = corsConfiguration.isSupportsCredentials()
                    || (corsConfiguration.getAllowedOrigins() != null && corsConfiguration.isOriginAllowed(origin)
                            && !corsConfiguration.getAllowedOrigins().contains("*"));

            log.debug("CorsFilter::doFilter() - 5.1 - allowCredentials = " + allowCredentials);
            response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));

            // exposedHeaders check
            log.debug("CorsFilter::doFilter() - 6 - corsConfiguration.getExposedHeaders() = "
                    + corsConfiguration.getExposedHeaders());
            final Collection<String> exposedHeaders = corsConfiguration.getExposedHeaders();
            log.debug("CorsFilter::doFilter() - 6.1 - exposedHeaders = " + exposedHeaders);
            if (exposedHeaders != null && exposedHeaders.size() > 0) {
                log.debug("CorsFilter::doFilter() - 6.2 - ");
                response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", exposedHeaders));
            }

            log.debug("CorsFilter::doFilter() - 7 - request.getMethod() = " + request.getMethod()
                    + " , corsConfiguration.getPreflightMaxAge() = " + corsConfiguration.getPreflightMaxAge());
            if ("OPTIONS".equals(request.getMethod())) {
                log.debug("CorsFilter::doFilter() - 7.1 - ");
                if ((requestedHeaders != null || requestedMethods != null)
                        && corsConfiguration.getPreflightMaxAge() != 0) {
                    log.debug("CorsFilter::doFilter() - 7.2 - ");
                    response.addHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(corsConfiguration.getPreflightMaxAge()));
                }
                log.debug("CorsFilter::doFilter() - 7.3 - ");
            } else {
                log.debug("CorsFilter::doFilter() - 7.4 - ");
                filterChain.doFilter(servletRequest, servletResponse);
            }

        }
    }

    @Override
    public void destroy() {
    }

    private void processRequestedHeaders(HttpServletResponse response, String allowHeadersValue) {
        log.debug("\n\n CorsFilter::processRequestedHeaders() - Entry - allowHeadersValue = " + allowHeadersValue
                + " , corsConfiguration.getAllowedHttpHeaders() = " + corsConfiguration.getAllowedHttpHeaders());
        if (corsConfiguration.getAllowedHttpHeaders() == null
                || corsConfiguration.getAllowedHttpHeaders().size() == 0) {
            log.debug("CorsFilter::processRequestedHeaders() - 1 ");
            response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            log.debug("CorsFilter::processRequestedHeaders() - 2 ");
            List<String> requestedHeaders = new ArrayList<>();
            for (String requestedHeader : COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue)) {
                log.debug("CorsFilter::processRequestedHeaders() - 2.1 - requestedHeader =" + requestedHeader);
                requestedHeaders.add(requestedHeader.toLowerCase());
            }
            log.debug("CorsFilter::processRequestedHeaders() - 3 - requestedHeaders =" + requestedHeaders);
            log.debug("CorsFilter::processRequestedHeaders() - 4 - corsConfiguration.getAllowedHttpHeaders() ="
                    + corsConfiguration.getAllowedHttpHeaders());
            List<String> validRequestedHeaders = new ArrayList<>();
            for (String configHeader : corsConfiguration.getAllowedHttpHeaders()) {
                log.debug("CorsFilter::processRequestedHeaders() - 4.1 - configHeader =" + configHeader);
                if (requestedHeaders.contains(configHeader.toLowerCase())) {
                    validRequestedHeaders.add(configHeader);
                }
            }
            log.debug("CorsFilter::processRequestedHeaders() - 5 - validRequestedHeaders =" + validRequestedHeaders);
            if (!validRequestedHeaders.isEmpty()) {
                log.debug(
                        "CorsFilter::processRequestedHeaders() - 6 - validRequestedHeaders =" + validRequestedHeaders);
                response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", validRequestedHeaders));
            }
        }
    }

    private void processMethods(HttpServletResponse response, String allowMethodsValue) {
        log.debug("\n\n CorsFilter::processMethods() - Entry - allowMethodsValue = " + allowMethodsValue
                + " , corsConfiguration.getAllowedHttpMethods() = " + corsConfiguration.getAllowedHttpMethods());
        if (corsConfiguration.getAllowedHttpMethods() == null | corsConfiguration.getAllowedHttpMethods().size() == 0) {
            log.debug("\n\n CorsFilter::processMethods() - 1 - ");
            response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            log.debug("\n\n CorsFilter::processMethods() - 2 - ");
            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                log.debug("\n\n CorsFilter::processMethods() - 2.1 - requestedMethod = " + requestedMethod);
                requestedMethods.add(requestedMethod.toLowerCase());
            }
            log.debug("\n\n CorsFilter::processMethods() - 3 - requestedMethods = " + requestedMethods);
            log.debug("\n\n CorsFilter::processMethods() - 4 - corsConfiguration.getAllowedHttpMethods() = "
                    + corsConfiguration.getAllowedHttpMethods());

            List<String> validRequestedMethods = new ArrayList<>();
            for (String configMethod : corsConfiguration.getAllowedHttpMethods()) {
                log.debug("\n\n CorsFilter::processMethods() - 4.1 - configMethod = " + configMethod);
                if (requestedMethods.contains(configMethod.toLowerCase())) {
                    log.debug("\n\n CorsFilter::processMethods() - 4.2 - ");
                    validRequestedMethods.add(configMethod);
                }
            }
            log.debug("\n\n CorsFilter::processMethods() - 5 - validRequestedMethods = " + validRequestedMethods);

            if (!validRequestedMethods.isEmpty()) {
                log.debug("\n\n CorsFilter::processMethods() - 6 - validRequestedMethods = " + validRequestedMethods);
                response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, String.join(",", validRequestedMethods));
            }
        }
    }

}
