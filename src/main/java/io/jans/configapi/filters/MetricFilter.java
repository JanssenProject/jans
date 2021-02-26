/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;
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
    MetricRegistry registry;

    private Metadata createMetaData(HttpServletRequest request, MetricType type, String prefix) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return new MetadataBuilder().withName(prefix + path).withDisplayName(prefix + path).withType(type)
                .withUnit(MetricUnits.SECONDS).build();
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        registry.counter(createMetaData((HttpServletRequest) servletRequest, MetricType.COUNTER, COUNTER)).inc();
        Timer timer = registry.timer(createMetaData((HttpServletRequest) servletRequest, MetricType.TIMER, TIMER));
        Meter meter = registry.meter(createMetaData((HttpServletRequest) servletRequest, MetricType.METERED, METER));

        chain.doFilter(servletRequest, servletResponse);

        meter.mark();
        timer.time().stop();
    }
}
