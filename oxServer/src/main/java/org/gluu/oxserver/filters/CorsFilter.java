package org.gluu.oxserver.filters;

import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

/**
 * CORS wrapper to support both Tomcat and Jetty
 * 
 * @author Yuriy Movchan
 * @version September 07, 2016
 */
public class CorsFilter implements Filter {

	private static final Logger LOG = Logger.getLogger(CorsFilter.class);

	private static final String CORS_FILTERS[] = { "org.apache.catalina.filters.CorsFilter",
			"org.eclipse.jetty.servlets.CrossOriginFilter" };
	
	Filter filter;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filter = getServerCorsFilter();
		
		if (this.filter != null) {
			filter.init(filterConfig);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (this.filter != null) {
			filter.doFilter(request, response, chain);
		}
	}

	@Override
	public void destroy() {
		if (this.filter != null) {
			filter.destroy();
		}
	}
	
	public Filter getServerCorsFilter() {
		Filter resultFilter = null;
		for (String filterName : CORS_FILTERS) {
			try {
		        Class<?> clazz = Class.forName(filterName);
		        Constructor<?> cons = clazz.getDeclaredConstructor();
		        resultFilter = (Filter) cons.newInstance();
			} catch (Exception ex) {
			}
		}
		
		LOG.debug("Prepared CORS filter: " + resultFilter);

		return resultFilter;
	}

}
