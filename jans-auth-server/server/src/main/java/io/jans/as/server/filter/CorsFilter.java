/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.filter;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.as.server.service.ClientService;
import io.jans.server.filters.AbstractCorsFilter;
import io.jans.util.StringHelper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CORS Filter to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version March 20, 2018
 */
@WebFilter(
        filterName = "CorsFilter",
        asyncSupported = true,
        urlPatterns = {"/.well-known/*", "/restv1/*", "/opiframe"})
public class CorsFilter extends AbstractCorsFilter {

    @Inject
    private Logger log;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private ClientService clientService;

    private boolean filterEnabled;

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

            String configEnabled = corsFilterConfig
                    .getInitParameter(PARAM_CORS_ENABLED);
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

            if (configEnabled != null) {
                this.filterEnabled = Boolean.parseBoolean(configEnabled);
            }

            parseAndStore(configAllowedOrigins, configAllowedHttpMethods,
                    configAllowedHttpHeaders, configExposedHeaders,
                    configSupportsCredentials, configPreflightMaxAge,
                    configDecorateRequest);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        Collection<String> globalAllowedOrigins = new ArrayList<>(0);
        if (this.filterEnabled) {
            try {
                globalAllowedOrigins = doFilterImpl(servletRequest);
            } catch (Exception ex) {
                log.error("Failed to process request", ex);
            }
            super.doFilter(servletRequest, servletResponse, filterChain);
            setAllowedOrigins(globalAllowedOrigins);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    protected Collection<String> doFilterImpl(ServletRequest servletRequest)
            throws IOException, ServletException {
        Collection<String> globalAllowedOrigins = getAllowedOrigins();

        if (StringHelper.isNotEmpty(servletRequest.getParameter("client_id"))) {
            String clientId = servletRequest.getParameter("client_id");
            Client client = clientService.getClient(clientId);
            if (client != null) {
                String[] authorizedOriginsArray = client.getAuthorizedOrigins();
                if (authorizedOriginsArray != null && authorizedOriginsArray.length > 0) {
                    List<String> clientAuthorizedOrigins = Arrays.asList(authorizedOriginsArray);
                    setAllowedOrigins(clientAuthorizedOrigins);
                }
            }
        } else {
            final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            String header = httpRequest.getHeader("Authorization");
            if (httpRequest.getRequestURI().endsWith("/token")) {
                if (header != null && header.startsWith("Basic ")) {
                    String base64Token = header.substring(6);
                    String token = new String(Base64.decodeBase64(base64Token), StandardCharsets.UTF_8);

                    String username = "";
                    int delim = token.indexOf(":");

                    if (delim != -1) {
                        username = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
                    }

                    Client client = clientService.getClient(username);

                    if (client != null) {
                        String[] authorizedOriginsArray = client.getAuthorizedOrigins();
                        if (authorizedOriginsArray != null && authorizedOriginsArray.length > 0) {
                            List<String> clientAuthorizedOrigins = Arrays.asList(authorizedOriginsArray);
                            setAllowedOrigins(clientAuthorizedOrigins);
                        }
                    }
                }
            }
        }

        return globalAllowedOrigins;
    }
}

