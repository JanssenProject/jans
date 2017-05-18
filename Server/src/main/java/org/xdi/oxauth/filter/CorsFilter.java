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
 * CORS wrapper to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version May 16, 2017
 */
@WebFilter(
        filterName = "CorsFilter",
        asyncSupported = true,
        urlPatterns = {"/.well-known/*", "/seam/resource/restv1/oxauth/*", "/opiframe"})
public class CorsFilter extends AbstractCorsFilter {

    @Inject
    private ConfigurationFactory configurationFactory;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        AppConfiguration appConfiguration = configurationFactory.getAppConfiguration();
        this.filter = getServerCorsFilter();

        if (this.filter != null) {
            String filterName = filterConfig.getFilterName();

            CorsFilterConfig corsFilterConfig = new CorsFilterConfig(filterName, appConfiguration);

            filter.init(corsFilterConfig);
        }
    }
}
