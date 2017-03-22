/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.filter;

import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.apache.log4j.Logger;
import org.gluu.oxserver.filters.AbstractCorsFilter;
import org.xdi.oxauth.model.configuration.AppConfiguration;

/**
 * CORS wrapper to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version March 9, 2017
 */
@WebFilter(initParams = { @WebInitParam(name = "cors.allowed.origins", value = "*") }, urlPatterns = { "/.well-known/*",
		"/seam/resource/restv1/oxauth/userinfo", "/seam/resource/restv1/oxauth/clientinfo" })
public class CorsFilter extends AbstractCorsFilter {

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filter = getServerCorsFilter();

		if (this.filter != null) {
			String filterName = filterConfig.getFilterName();

			CorsFilterConfig corsFilterConfig = new CorsFilterConfig(filterName, appConfiguration);

			filter.init(corsFilterConfig);
		}
	}
}
