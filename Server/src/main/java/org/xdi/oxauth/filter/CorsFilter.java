/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.filter;

import org.apache.log4j.Logger;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.xdi.oxauth.model.configuration.AppConfiguration;

import javax.servlet.*;
import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * CORS wrapper to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version March 9, 2017
 */
@AutoCreate
@Name("corsFilter")
public class CorsFilter implements Filter {

    private static final Logger log = Logger.getLogger(CorsFilter.class);

    private static final String CORS_FILTERS[] = {
            "org.apache.catalina.filters.CorsFilter",
            "org.eclipse.jetty.servlets.CrossOriginFilter"};

    private AppConfiguration appConfiguration;

    Filter filter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (!Contexts.isApplicationContextActive()) {
            Lifecycle.setupApplication();
        }

        this.appConfiguration = (AppConfiguration) Component.getInstance("appConfiguration", ScopeType.APPLICATION);
        this.filter = getServerCorsFilter();

        if (this.filter != null) {
            String filterName = filterConfig.getFilterName();

            CorsFilterConfig corsFilterConfig = new CorsFilterConfig(filterName, appConfiguration);

            filter.init(corsFilterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (this.filter != null) {
            filter.doFilter(request, response, chain);
        } else {
            // pass the request along the filter chain
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        if (this.filter != null) {
            filter.destroy();
        }
    }

    public Filter getServerCorsFilter() {
        javax.servlet.Filter resultFilter = null;
        for (String filterName : CORS_FILTERS) {
            try {
                Class<?> clazz = Class.forName(filterName);
                Constructor<?> cons = clazz.getDeclaredConstructor();
                resultFilter = (Filter) cons.newInstance();
                break;
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        if (resultFilter == null) {
            log.error("Failed to prepare CORS filter");
        } else {
            log.debug("Prepared CORS filter: " + resultFilter);
        }

        return resultFilter;
    }
}
