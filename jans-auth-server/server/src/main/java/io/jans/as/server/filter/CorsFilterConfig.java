/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.filter;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.CorsConfigurationFilter;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version February 15, 2017
 */
public class CorsFilterConfig implements FilterConfig {

    private final String filterName;
    private final Map<String, String> initParameters;

    /**
     * Key to retrieve if filter enabled from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_ENABLED = "cors.enabled";

    /**
     * Key to retrieve allowed origins from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_ALLOWED_ORIGINS = "cors.allowed.origins";

    /**
     * Key to retrieve allowed methods from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_ALLOWED_METHODS = "cors.allowed.methods";

    /**
     * Key to retrieve allowed headers from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_ALLOWED_HEADERS = "cors.allowed.headers";

    /**
     * Key to retrieve exposed headers from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_EXPOSED_HEADERS = "cors.exposed.headers";

    /**
     * Key to retrieve support credentials from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_SUPPORT_CREDENTIALS = "cors.support.credentials";

    /**
     * Key to retrieve logging enabled from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_LOGGING_ENABLED = "cors.logging.enabled";

    /**
     * Key to retrieve preflight max age from {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_PREFLIGHT_MAXAGE = "cors.preflight.maxage";

    /**
     * Key to determine if request should be decorated {@link CorsConfigurationFilter}.
     */
    public static final String PARAM_CORS_REQUEST_DECORATE = "cors.request.decorate";

    public CorsFilterConfig(String filterName, AppConfiguration appConfiguration) {
        this.filterName = filterName;
        initParameters = new HashMap<String, String>();

        List<CorsConfigurationFilter> corsConfigurationFilters = appConfiguration.getCorsConfigurationFilters();
        for (CorsConfigurationFilter corsConfigurationFilter : corsConfigurationFilters) {
            if (filterName.equals(corsConfigurationFilter.getFilterName())) {
                initParameters.put(PARAM_CORS_ENABLED, corsConfigurationFilter.getCorsEnabled().toString());
                initParameters.put(PARAM_CORS_ALLOWED_ORIGINS, corsConfigurationFilter.getCorsAllowedOrigins());
                initParameters.put(PARAM_CORS_ALLOWED_METHODS, corsConfigurationFilter.getCorsAllowedMethods());
                initParameters.put(PARAM_CORS_ALLOWED_HEADERS, corsConfigurationFilter.getCorsAllowedHeaders());
                initParameters.put(PARAM_CORS_EXPOSED_HEADERS, corsConfigurationFilter.getCorsExposedHeaders());
                initParameters.put(PARAM_CORS_SUPPORT_CREDENTIALS, corsConfigurationFilter.getCorsSupportCredentials().toString());
                initParameters.put(PARAM_CORS_LOGGING_ENABLED, corsConfigurationFilter.getCorsLoggingEnabled().toString());
                initParameters.put(PARAM_CORS_PREFLIGHT_MAXAGE, corsConfigurationFilter.getCorsPreflightMaxAge().toString());
                initParameters.put(PARAM_CORS_REQUEST_DECORATE, corsConfigurationFilter.getCorsRequestDecorate().toString());
            }
        }

    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        if (initParameters == null) {
            return (null);
        }

        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
