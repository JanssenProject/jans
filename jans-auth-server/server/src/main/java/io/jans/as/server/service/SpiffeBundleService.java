/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.SpiffeTrustDomainConfiguration;
import io.jans.as.model.util.CertUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Fetches and caches SPIFFE Bundle Endpoint documents (a JWKS keyed by trust domain, per
 * draft-ietf-oauth-spiffe-client-auth section 6), used to validate X.509-SVID and JWT-SVID
 * client credentials during SPIFFE-based client authentication.
 * <p>
 * Trust domains and their bundle endpoint URLs come exclusively from the admin-configured
 * {@code spiffeTrustDomains} list in {@link AppConfiguration} - a client-supplied
 * {@code spiffe_bundle_endpoint} is never used as a trust anchor source, since trusting it would
 * let a client vouch for itself.
 *
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class SpiffeBundleService {

    private static final String USE_X509_SVID = "x509-svid";
    private static final String USE_JWT_SVID = "jwt-svid";

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Returns the X.509 trust anchors (CA certificates tagged {@code use: x509-svid}) configured
     * for the given trust domain, for path-validating a presented X.509-SVID. Returns an empty
     * set (fail closed) if the trust domain is not configured or its bundle cannot be obtained.
     */
    public Set<TrustAnchor> getX509TrustAnchors(String trustDomain) {
        final JSONObject bundle = getBundle(trustDomain);
        final Set<TrustAnchor> anchors = new HashSet<>();
        if (bundle == null) {
            return anchors;
        }

        final JSONArray keys = bundle.optJSONArray("keys");
        if (keys == null) {
            return anchors;
        }
        for (int i = 0; i < keys.length(); i++) {
            final JSONObject key = keys.optJSONObject(i);
            if (key == null || !USE_X509_SVID.equals(key.optString("use"))) {
                continue;
            }
            final JSONArray x5c = key.optJSONArray("x5c");
            if (x5c == null || x5c.isEmpty()) {
                continue;
            }
            final X509Certificate cert = CertUtils.x509CertificateFromPem(x5c.optString(0));
            if (cert != null) {
                anchors.add(new TrustAnchor(cert, null));
            }
        }
        return anchors;
    }

    /**
     * Returns a JWKS (as a {@link JSONObject}, ready to feed into the crypto provider's signature
     * verification) containing only the keys tagged {@code use: jwt-svid} for the given trust
     * domain, used to verify a JWT-SVID's signature. Returns null (fail closed) if the trust
     * domain is not configured or its bundle cannot be obtained.
     */
    public JSONObject getJwtSvidJwks(String trustDomain) {
        final JSONObject bundle = getBundle(trustDomain);
        if (bundle == null) {
            return null;
        }

        final JSONArray filtered = new JSONArray();
        final JSONArray keys = bundle.optJSONArray("keys");
        if (keys != null) {
            for (int i = 0; i < keys.length(); i++) {
                final JSONObject key = keys.optJSONObject(i);
                if (key != null && USE_JWT_SVID.equals(key.optString("use"))) {
                    filtered.put(key);
                }
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        return new JSONObject().put("keys", filtered);
    }

    private SpiffeTrustDomainConfiguration findConfiguration(String trustDomain) {
        if (StringUtils.isBlank(trustDomain)) {
            return null;
        }
        for (SpiffeTrustDomainConfiguration config : appConfiguration.getSpiffeTrustDomains()) {
            if (trustDomain.equalsIgnoreCase(config.getTrustDomain())) {
                return config;
            }
        }
        return null;
    }

    private JSONObject getBundle(String trustDomain) {
        final SpiffeTrustDomainConfiguration config = findConfiguration(trustDomain);
        if (config == null || StringUtils.isBlank(config.getBundleEndpointUrl())) {
            log.debug("No SPIFFE trust bundle configured for trust domain: {}", trustDomain);
            return null;
        }

        final CacheEntry cached = cache.get(trustDomain);
        if (cached != null && !cached.isExpired()) {
            return cached.jwks;
        }

        try {
            final JSONObject fetched = fetchBundle(config.getBundleEndpointUrl());
            cache.put(trustDomain, new CacheEntry(fetched, System.currentTimeMillis() + config.getBundleCacheLifetimeInMinutes() * 60_000L));
            return fetched;
        } catch (RuntimeException e) {
            log.error("Failed to fetch SPIFFE bundle for trust domain: {} from: {}", trustDomain, config.getBundleEndpointUrl(), e);
            if (cached != null) {
                log.warn("Serving stale cached SPIFFE bundle for trust domain: {} after fetch failure.", trustDomain);
                return cached.jwks;
            }
            return null;
        }
    }

    protected JSONObject fetchBundle(String bundleEndpointUrl) {
        try (Client httpClient = ClientBuilder.newBuilder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build()) {

            final Response response = httpClient.target(bundleEndpointUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed to fetch SPIFFE bundle: HTTP " + response.getStatus() + " from " + bundleEndpointUrl);
            }
            return new JSONObject(response.readEntity(String.class));
        }
    }

    private static class CacheEntry {
        final JSONObject jwks;
        final long expiresAtMillis;

        CacheEntry(JSONObject jwks, long expiresAtMillis) {
            this.jwks = jwks;
            this.expiresAtMillis = expiresAtMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
