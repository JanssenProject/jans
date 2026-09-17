package io.jans.configapi.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Safety net for anything that does NOT pass through the JAX-RS pipeline —
 * e.g. static resources, container-generated error pages (401/403/404/500),
 * or other servlet-mapped paths in the config-api WAR. Ensures the same
 * headers are present on literally every response the server sends,
 * regardless of how the request was handled or which plugin (if any)
 * ultimately served it.
 *
 * urlPatterns="/*" is intentional: it is cheap (just sets a few headers)
 * and guarantees uniformity across server + plugin endpoints even if a
 * plugin adds its own raw servlets rather than JAX-RS resources.
 *
 * If your deployment prefers XML-based registration (web.xml) instead of
 * the annotation below, use the equivalent <filter>/<filter-mapping>
 * entries — do not register the same filter both ways.
 */
@WebFilter(urlPatterns = "/*", filterName = "SecurityHeadersServletFilter")
public class SecurityHeadersServletFilter implements Filter {

    private static final String CSP_VALUE =
            "default-src 'none'; "
          + "frame-ancestors 'none'; "
          + "base-uri 'none'; "
          + "form-action 'none'";

    private static final String X_FRAME_OPTIONS_VALUE = "DENY";
    private static final String REFERRER_POLICY_VALUE = "no-referrer";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            // setHeader (not addHeader) so this always wins even if the
            // JAX-RS filter, or a plugin, already set something.
            httpResponse.setHeader("Content-Security-Policy", CSP_VALUE);
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("Referrer-Policy", REFERRER_POLICY_VALUE);
            httpResponse.setHeader("X-Frame-Options", X_FRAME_OPTIONS_VALUE);
        }

        chain.doFilter(request, response);
    }
}