/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxserver.filters.AbstractCorsFilter;
import org.gluu.util.StringHelper;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.ClientService;

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
    private ConfigurationFactory configurationFactory;

    @Inject
    private AppConfiguration appConfiguration;

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
        Collection<String> globalAllowedOrigins = null;
        if (this.filterEnabled) {
            globalAllowedOrigins = doFilterImpl(servletRequest);
            super.doFilter(servletRequest, servletResponse, filterChain);
            setAllowedOrigins(globalAllowedOrigins);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    protected Collection<String> doFilterImpl(ServletRequest servletRequest)
            throws UnsupportedEncodingException, IOException, ServletException {
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
                    String token = new String(Base64.decodeBase64(base64Token), Util.UTF8_STRING_ENCODING);

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

