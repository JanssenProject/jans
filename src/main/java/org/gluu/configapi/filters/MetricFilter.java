/**
 * 
 */
package org.gluu.configapi.filters;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

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
        timer.time();
    }
}
