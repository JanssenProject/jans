/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.core.cedarling.service;

import static io.jans.core.cedarling.service.CedarlingProtection.simpleResponse;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

/**
 * Test-only {@link CedarlingProtectionService} implementation.
 *
 * <p>
 * Production subclasses (e.g. the Lock Server one) validate the bearer token as a JWT (issuer, expiration, signature against the OIDC provider's JWKS) before
 * asking Cedarling for an authorization decision. Tests have no OIDC provider available and initialise Cedarling with {@code jwtStatusValidation(false)} /
 * {@code jwtSigValidation(false)}, so this implementation skips the JWT validation step entirely.
 *
 * <p>
 * The bearer token itself is <strong>not</strong> ignored: it is still passed to {@link #isValid(String, ResourceInfo)} and therefore to Cedarling, which
 * makes the real scope-based ALLOW/DENY decision from the token's claims.
 *
 * @author Author Date: 12/05/2026
 */
public class DummyCedarlingProtectionService extends CedarlingProtectionService {

    @Override
    public Response processAuthorization(String bearerToken, ResourceInfo resourceInfo) {
        try {
            boolean authFound = (bearerToken != null) && !bearerToken.isEmpty();
            log.info("Authorization header {} found", authFound ? "" : "not");

            if (!authFound) {
                log.info("Request is missing authorization header");
                // See section 3.12 RFC 7644
                return simpleResponse(UNAUTHORIZED, "No authorization header found");
            }

            bearerToken = bearerToken.replaceFirst("Bearer\\s+", "");
            log.debug("Skipping JWT validation, authorizing token {} via Cedarling", bearerToken);

            return isValid(bearerToken, resourceInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return simpleResponse(INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
