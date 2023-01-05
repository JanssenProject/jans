/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Provider
@WebFilter(urlPatterns = { "*" })
public class MetricFilter implements Filter {

    private static final String METER = "meter_";
    private static final String COUNTER = "counter_";
    private static final String TIMER = "timer_";

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    private Metadata createMetaData(HttpServletRequest request, MetricType type, String prefix) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return new MetadataBuilder().withName(prefix + path).withDisplayName(prefix + path).withType(type)
                .withUnit(MetricUnits.SECONDS).build();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        metricRegistry.counter(createMetaData((HttpServletRequest) servletRequest, MetricType.COUNTER, COUNTER)).inc();
        Timer timer = metricRegistry
                .timer(createMetaData((HttpServletRequest) servletRequest, MetricType.TIMER, TIMER));
        Meter meter = metricRegistry
                .meter(createMetaData((HttpServletRequest) servletRequest, MetricType.METERED, METER));

        chain.doFilter(servletRequest, servletResponse);

        meter.mark();
        timer.time().stop();
    }

    @Override
    public void destroy() {
    }
}
