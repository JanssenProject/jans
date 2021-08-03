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

    //@Inject
    //ConfigurationFactory configurationFactory;

    private static final Pattern COMMA_SEPARATED_SPLIT_REGEX = Pattern.compile("\\s*,\\s*");
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * The Access-Control-Allow-Headers header indicates, as part of the
     * response to a preflight request, which header field names can be used
     * during the actual request.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS =
            "Access-Control-Allow-Headers";
    
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {       
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        log.error("CorsFilter::doFilter() - Entry - corsConfiguration = "+corsConfiguration);
        Collection<String> globalAllowedOrigins = null;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        String origin = request.getHeader("Origin");     
        log.debug("CorsFilter::doFilter() - Entry - origin = " + origin);
        
        if ( (StringUtils.isNotBlank(origin)) || (corsConfiguration!=null && !corsConfiguration.isEnabled()) ) {
               
                //Method check
                final String requestedMethods = request.getMethod();
                log.debug("CorsFilter::doFilter() requestedMethods = "+requestedMethods);
                if (StringUtils.isNotBlank(requestedMethods)) {
                    processMethods(response, requestedMethods);
                }

                //Header check
                final String requestedHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS);
                log.debug("CorsFilter::doFilter() requestedHeaders = "+requestedHeaders);
                if (StringUtils.isNotBlank(requestedHeaders)) {
                    processRequestedHeaders(response, requestedHeaders);
                }

                //Origin check
                boolean allowsOrigin =  (corsConfiguration.getAllowedOrigins()==null || corsConfiguration.getAllowedOrigins().size() ==0 || corsConfiguration.isOriginAllowed(origin));
                log.debug("CorsFilter::doFilter() allowsOrigin = "+allowsOrigin);
                if (allowsOrigin) {
                    response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }

                boolean allowCredentials = ( (corsConfiguration.isSupportsCredentials()) || 
                       (corsConfiguration.getAllowedOrigins()!=null && corsConfiguration.isOriginAllowed(origin) && !corsConfiguration.getAllowedOrigins().contains("*")) );

                response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));

                final Collection<String> exposedHeaders = corsConfiguration.getExposedHeaders();

                if (exposedHeaders !=null && exposedHeaders.size()>0) {
                    response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS,
                            String.join(",", exposedHeaders));
                }
/*
                if (request.method().equals(HttpMethod.OPTIONS)) {
                    if ((requestedHeaders != null || requestedMethods != null) && corsConfiguration.accessControlMaxAge.isPresent()) {
                        response.putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                                String.valueOf(corsConfiguration.accessControlMaxAge.get().getSeconds()));
                    }
                    response.end();
                } else {
                    event.next();
                }
            */
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
    
    @Override
    public void destroy() {
    }

    private void processRequestedHeaders(HttpServletResponse response, String allowHeadersValue) {
        log.debug("CorsFilter::processRequestedHeaders() - Entry - allowHeadersValue = "+allowHeadersValue);
        if (corsConfiguration.getAllowedHttpHeaders()== null || corsConfiguration.getAllowedHttpHeaders().size()==0) {
            response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowHeadersValue);
        } else {
            List<String> requestedHeaders = new ArrayList<>();
            for (String requestedHeader : COMMA_SEPARATED_SPLIT_REGEX.split(allowHeadersValue)) {
                requestedHeaders.add(requestedHeader.toLowerCase());
            }

            List<String> validRequestedHeaders = new ArrayList<>();
            for (String configHeader : corsConfiguration.getAllowedHttpHeaders()) {
                if (requestedHeaders.contains(configHeader.toLowerCase())) {
                    validRequestedHeaders.add(configHeader);
                }
            }

            if (!validRequestedHeaders.isEmpty()) {
                response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", validRequestedHeaders));
            }
        }
    }

    private void processMethods(HttpServletResponse response, String allowMethodsValue) {
        if (corsConfiguration.getAllowedHttpMethods()==null | corsConfiguration.getAllowedHttpMethods().size()==0) {
            response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethodsValue);
        } else {
            List<String> requestedMethods = new ArrayList<>();
            for (String requestedMethod : COMMA_SEPARATED_SPLIT_REGEX.split(allowMethodsValue)) {
                requestedMethods.add(requestedMethod.toLowerCase());
            }

            List<String> validRequestedMethods = new ArrayList<>();
            for (String configMethod : corsConfiguration.getAllowedHttpMethods()) {
                if (requestedMethods.contains(configMethod.toLowerCase())) {
                    validRequestedMethods.add(configMethod);
                }
            }

            if (!validRequestedMethods.isEmpty()) {
                response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, String.join(",", validRequestedMethods));
            }
        }
    }
    
    
}
