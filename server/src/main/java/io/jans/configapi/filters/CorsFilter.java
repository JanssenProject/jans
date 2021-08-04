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
import java.util.*;
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

@WebFilter(filterName = "CorsFilter", asyncSupported = true, urlPatterns = { "/*" })
public class CorsFilter extends AbstractCorsFilter {

    @Inject
    private Logger log;

    @Inject
    ConfigurationFactory configurationFactory;

    private boolean filterEnabled;

    public CorsFilter() {
        super();
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        log.debug("\n\n\n *** CorsFilter::init() - configurationFactory = " + configurationFactory + " *** \n\n\n");
        // Initialize defaults
        parseAndStore(DEFAULT_ALLOWED_ORIGINS, DEFAULT_ALLOWED_HTTP_METHODS, DEFAULT_ALLOWED_HTTP_HEADERS,
                DEFAULT_EXPOSED_HEADERS, DEFAULT_SUPPORTS_CREDENTIALS, DEFAULT_PREFLIGHT_MAXAGE,
                DEFAULT_DECORATE_REQUEST);

        ApiAppConfiguration apiAppConfiguration = configurationFactory.getApiAppConfiguration();

        log.debug("\n\n\n *** CorsFilter::init() - apiAppConfiguration = " + apiAppConfiguration + " *** \n\n\n");

        if (filterConfig != null) {
            String filterName = filterConfig.getFilterName();
            CorsFilterConfig corsFilterConfig = new CorsFilterConfig(filterName, apiAppConfiguration);

            String configEnabled = corsFilterConfig.getInitParameter(PARAM_CORS_ENABLED);
            String configAllowedOrigins = corsFilterConfig.getInitParameter(PARAM_CORS_ALLOWED_ORIGINS);
            String configAllowedHttpMethods = corsFilterConfig.getInitParameter(PARAM_CORS_ALLOWED_METHODS);
            String configAllowedHttpHeaders = corsFilterConfig.getInitParameter(PARAM_CORS_ALLOWED_HEADERS);
            String configExposedHeaders = corsFilterConfig.getInitParameter(PARAM_CORS_EXPOSED_HEADERS);
            String configSupportsCredentials = corsFilterConfig.getInitParameter(PARAM_CORS_SUPPORT_CREDENTIALS);
            String configPreflightMaxAge = corsFilterConfig.getInitParameter(PARAM_CORS_PREFLIGHT_MAXAGE);
            String configDecorateRequest = corsFilterConfig.getInitParameter(PARAM_CORS_REQUEST_DECORATE);

            if (configEnabled != null) {
                this.filterEnabled = Boolean.parseBoolean(configEnabled);
            }

            log.debug("\n\n\n *** CorsFilter::init() - configEnabled = "+configEnabled+" , configAllowedOrigins = " + configAllowedOrigins
                    + " ,configAllowedHttpMethods = " + configAllowedHttpMethods + " *** \n\n\n");
            parseAndStore(configAllowedOrigins, configAllowedHttpMethods, configAllowedHttpHeaders,
                    configExposedHeaders, configSupportsCredentials, configPreflightMaxAge, configDecorateRequest);
            
            log.debug("\n\n\n *** CorsFilter::init() - configEnabled = "+configEnabled+" , configAllowedOrigins = " + configAllowedOrigins
                    + " ,configAllowedHttpMethods = " + configAllowedHttpMethods + " *** \n\n\n");
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        Collection<String> globalAllowedOrigins = null;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String origin = request.getHeader("Origin");
        log.debug("\n\n\n *** CorsFilter::doFilter() - Entry - this.filterEnabled = "+this.filterEnabled+" ,origin = " + origin + " *** \n\n\n");
        Collection<String> allowedOrigins = null;
        if (this.filterEnabled) {
            try {
                allowedOrigins = doFilterImpl(servletRequest,origin);
                log.debug("\n\n\n *** CorsFilter::doFilter() - allowedOrigins = " + allowedOrigins
                + " *** \n\n\n");
            } catch (Exception ex) {
                log.error("Failed to process request", ex);
            }
            super.doFilter(servletRequest, servletResponse, filterChain);
            setAllowedOrigins(allowedOrigins);
        } else {
            log.debug("\n\n\n *** CorsFilter::doFilter() - filterEnabled disabled  *** \n\n\n");
            filterChain.doFilter(servletRequest, servletResponse);
        }
        
        log.debug("\n\n\n *** CorsFilter::doFilter() - 3 - getHeader(Origin) = " + request.getHeader("Origin")
                + " *** \n\n\n");
    }

    protected Collection<String> doFilterImpl(ServletRequest servletRequest,String origin )
            throws UnsupportedEncodingException, IOException, ServletException {
         
        Collection<String> globalAllowedOrigins = getAllowedOrigins();
        log.debug("\n\n\n *** CorsFilter::doFilterImpl() - New - globalAllowedOrigins = " + globalAllowedOrigins
        + " *** \n\n\n");

        if (getAllowedOrigins() == null) {
            log.debug("\n\n\n *** CorsFilter::doFilterImpl() getAllowedOrigins() - Is Null  ");
            List<String> authorizedOrigins = new ArrayList();
            authorizedOrigins.add(origin);
            setAllowedOrigins(authorizedOrigins);
        }else if (getAllowedOrigins().size() ==0 || getAllowedOrigins().contains("*")){
            log.debug("\n\n\n *** CorsFilter::doFilterImpl() getAllowedOrigins() - Empty OR Contains * ");
            globalAllowedOrigins.add(origin);
            setAllowedOrigins(globalAllowedOrigins);
        }
         
         return getAllowedOrigins();
         /*
        if (getAllowedOrigins() != null && getAllowedOrigins().size() != 0) {
            log.debug("\n\n\n *** CorsFilter::doFilterImpl() getAllowedOrigins() !=null ");
            return getAllowedOrigins();
        } else {
            log.debug("\n\n\n *** CorsFilter::doFilterImpl() getAllowedOrigins() =====null ");
            return getDefaultOrigin(servletRequest, "*");
        }
        */
    }

    private Collection<String> getDefaultOrigin(ServletRequest servletRequest, String origin)
            throws UnsupportedEncodingException, IOException, ServletException {
        Collection<String> defaultAllowedOrigins = new HashSet<String>();
        Set<String> setAllowedOrigins = parseStringToSet(origin);
        defaultAllowedOrigins.addAll(setAllowedOrigins);
        log.debug("\n\n\n *** CorsFilter::getDefaultOrigin() - defaultAllowedOrigins = "+defaultAllowedOrigins);
        return defaultAllowedOrigins;
    }

    private Collection<String> addOrigin(Collection<String> globalAllowedOrigins, String origin)
            throws UnsupportedEncodingException, IOException, ServletException {
        log.debug("\n\n\n *** CorsFilter::addOrigin() - globalAllowedOrigins = "+globalAllowedOrigins+" , origin = "+origin);
        if (globalAllowedOrigins != null && globalAllowedOrigins.contains("*")) {
            globalAllowedOrigins.add(origin);
        }
        log.debug("\n\n\n *** CorsFilter::addOrigin() - globalAllowedOrigins = "+globalAllowedOrigins);
        return globalAllowedOrigins;
    }

    private Set<String> parseStringToSet(final String data) {
        String[] splits;

        if (data != null && data.length() > 0) {
            splits = data.split(",");
        } else {
            splits = new String[] {};
        }

        Set<String> set = new HashSet<String>();
        if (splits.length > 0) {
            for (String split : splits) {
                set.add(split.trim());
            }
        }

        return set;
    }

}
