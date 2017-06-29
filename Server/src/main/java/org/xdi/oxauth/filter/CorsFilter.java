/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.filter;

import org.gluu.oxserver.filters.AbstractCorsFilter;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;

/**
 * CORS Filter to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version June 27, 2017
 */
@WebFilter(
        filterName = "CorsFilter",
        asyncSupported = true,
        urlPatterns = {"/.well-known/*", "/restv1/*", "/opiframe"})
public class CorsFilter extends AbstractCorsFilter {

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private AppConfiguration appConfiguration;

    public CorsFilter() {
        super();
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Initialize defaults
        parseAndStore(DEFAULT_ALLOWED_ORIGINS, DEFAULT_ALLOWED_HTTP_METHODS,
                DEFAULT_ALLOWED_HTTP_HEADERS, DEFAULT_EXPOSED_HEADERS,
                DEFAULT_SUPPORTS_CREDENTIALS, DEFAULT_PREFLIGHT_MAXAGE,
                DEFAULT_DECORATE_REQUEST);

        AppConfiguration appConfiguration = configurationFactory.getAppConfiguration();

        if (filterConfig != null) {
            String filterName = filterConfig.getFilterName();
            CorsFilterConfig corsFilterConfig = new CorsFilterConfig(filterName, appConfiguration);

            String configAllowedOrigins = corsFilterConfig
                    .getInitParameter(PARAM_CORS_ALLOWED_ORIGINS);
            String configAllowedHttpMethods = corsFilterConfig
                    .getInitParameter(PARAM_CORS_ALLOWED_METHODS);
            String configAllowedHttpHeaders = corsFilterConfig
                    .getInitParameter(PARAM_CORS_ALLOWED_HEADERS);
            String configExposedHeaders = corsFilterConfig
                    .getInitParameter(PARAM_CORS_EXPOSED_HEADERS);
            String configSupportsCredentials = corsFilterConfig
                    .getInitParameter(PARAM_CORS_SUPPORT_CREDENTIALS);
            String configPreflightMaxAge = corsFilterConfig
                    .getInitParameter(PARAM_CORS_PREFLIGHT_MAXAGE);
            String configDecorateRequest = corsFilterConfig
                    .getInitParameter(PARAM_CORS_REQUEST_DECORATE);

            parseAndStore(configAllowedOrigins, configAllowedHttpMethods,
                    configAllowedHttpHeaders, configExposedHeaders,
                    configSupportsCredentials, configPreflightMaxAge,
                    configDecorateRequest);
        }
    }
}
