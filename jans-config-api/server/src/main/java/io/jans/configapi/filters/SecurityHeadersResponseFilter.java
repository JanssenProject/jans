/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Applies the standard security headers to EVERY JAX-RS response produced by
 * the config-api server, including endpoints contributed by plugins.
 *
 * Because plugins register their resources into the same JAX-RS
 * {@code Application}/deployment used by the core server (see
 * ConfigApiApplication / plugin ServiceLoader wiring), a single
 * {@code @Provider} filter picked up by the JAX-RS runtime is sufficient to
 * cover both — no per-plugin registration is required.
 *
 * Registered with {@code @Priority(Priorities.HEADER_DECORATOR)} so it runs
 * as one of the last response filters, minimizing the chance some other
 * filter overwrites these headers afterward. It also intentionally
 * overwrites (rather than merges into) each header so a plugin cannot
 * accidentally weaken the policy by setting its own conflicting value.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class SecurityHeadersResponseFilter implements ContainerResponseFilter {

    // Adjust to 'self' if the config-api UI genuinely needs to be framed by
    // a known-good origin. Default is the strictest, safest option.
    private static final String CSP_VALUE =
            "default-src 'none'; "
          + "frame-ancestors 'none'; "
          + "base-uri 'none'; "
          + "form-action 'none'";

    // Legacy fallback for browsers that don't honor CSP frame-ancestors.
    // Must stay consistent with the frame-ancestors directive above.
    private static final String X_FRAME_OPTIONS_VALUE = "DENY";

    private static final String REFERRER_POLICY_VALUE = "no-referrer";

    @Override
    public void filter(ContainerRequestContext requestContext,
                        ContainerResponseContext responseContext) throws IOException {

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        // Content-Security-Policy — strict frame-ancestors, no external
        // resource loading needed for a JSON API.
        headers.putSingle("Content-Security-Policy", CSP_VALUE);

        // Prevent MIME-sniffing of response bodies.
        headers.putSingle("X-Content-Type-Options", "nosniff");

        // Don't leak the full referrer (including query strings/paths) to
        // other origins.
        headers.putSingle("Referrer-Policy", REFERRER_POLICY_VALUE);

        // Legacy clickjacking protection, kept in lockstep with
        // frame-ancestors above.
        headers.putSingle("X-Frame-Options", X_FRAME_OPTIONS_VALUE);
    }
}
