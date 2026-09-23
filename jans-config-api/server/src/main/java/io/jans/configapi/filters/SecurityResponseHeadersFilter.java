/*
 * Janssen Project software is available under the MIT License (2008).
 * See http://opensource.org/licenses/MIT for full text.
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a fixed set of standard HTTP security response headers to every
 * Config API response, regardless of resource, status code, or exception
 * mapper output.
 *
 * Headers applied:
 *  - Content-Security-Policy: frame-ancestors 'none'  (blocks framing at the CSP level)
 *  - X-Frame-Options: DENY                            (legacy fallback for the same policy)
 *  - X-Content-Type-Options: nosniff
 *  - Referrer-Policy: no-referrer
 *
 * Registered as a JAX-RS provider so it runs on the response path for every
 * resource method in the deployment - no per-endpoint annotation required.
 * Priority is set to run late (HEADER_DECORATOR) so it executes after other
 * response filters and cannot be short-circuited by them, and it does not
 * overwrite a header a resource/filter has already set intentionally to a
 * different, more specific value.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class SecurityResponseHeadersFilter implements ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityResponseHeadersFilter.class);

    private static final String HEADER_CSP = "Content-Security-Policy";
    private static final String HEADER_XFO = "X-Frame-Options";
    private static final String HEADER_XCTO = "X-Content-Type-Options";
    private static final String HEADER_REFERRER = "Referrer-Policy";

    // Config API is a backend service with no legitimate reason to be framed,
    // so both the modern (CSP) and legacy (X-Frame-Options) controls deny framing
    // entirely and are kept in lockstep.
    private static final String CSP_VALUE = "default-src 'none'; frame-ancestors 'none'";
    private static final String XFO_VALUE = "DENY";
    private static final String XCTO_VALUE = "nosniff";
    private static final String REFERRER_VALUE = "no-referrer";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        putIfAbsent(headers, HEADER_CSP, CSP_VALUE);
        putIfAbsent(headers, HEADER_XFO, XFO_VALUE);
        putIfAbsent(headers, HEADER_XCTO, XCTO_VALUE);
        putIfAbsent(headers, HEADER_REFERRER, REFERRER_VALUE);

        log.debug("Applied security headers to response for path='{}'",
                requestContext.getUriInfo() != null ? requestContext.getUriInfo().getPath() : "unknown");
    }

    private void putIfAbsent(MultivaluedMap<String, Object> headers, String name, String value) {
        if (!headers.containsKey(name)) {
            headers.putSingle(name, value);
        } else {
            log.debug("Header '{}' already present on response; leaving existing value intact", name);
        }
    }
}
