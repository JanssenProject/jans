/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility for parsing and matching SPIFFE IDs (spiffe://trust-domain/path), used by SPIFFE-based
 * client authentication (draft-ietf-oauth-spiffe-client-auth).
 *
 * @author Yuriy Zabrovarnyy
 */
public class SpiffeIdUtil {

    public static final String SCHEME = "spiffe";
    public static final String WILDCARD_SUFFIX = "/*";

    private SpiffeIdUtil() {
    }

    /**
     * Validates the syntax of a SPIFFE ID as presented in a credential (X.509-SVID URI SAN or
     * JWT-SVID `sub` claim). Presented SPIFFE IDs must be concrete: they must not carry the
     * "/*" wildcard suffix, which is only meaningful in a *registered* client's `spiffe_id`
     * metadata.
     */
    public static boolean isValidPresentedSpiffeId(String value) {
        return isValidSpiffeId(value, false);
    }

    /**
     * Validates the syntax of a `spiffe_id` value as registered in client metadata, which may
     * carry a trailing "/*" for path-segment prefix matching against presented SVIDs.
     */
    public static boolean isValidRegisteredSpiffeId(String value) {
        return isValidSpiffeId(value, true);
    }

    private static boolean isValidSpiffeId(String value, boolean allowWildcard) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        String toParse = value;
        if (value.endsWith(WILDCARD_SUFFIX)) {
            if (!allowWildcard) {
                return false;
            }
            toParse = value.substring(0, value.length() - WILDCARD_SUFFIX.length());
            if (toParse.isEmpty()) {
                return false;
            }
        }

        final URI uri;
        try {
            uri = new URI(toParse);
        } catch (URISyntaxException e) {
            return false;
        }

        if (!SCHEME.equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        if (StringUtils.isBlank(uri.getHost())) {
            return false;
        }
        if (uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
            return false;
        }

        final String path = uri.getPath();
        if (path != null) {
            for (String segment : path.split("/")) {
                if (".".equals(segment) || "..".equals(segment)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the trust domain (authority component, lower-cased) of a SPIFFE ID, e.g.
     * "example.org" for "spiffe://example.org/my-workload". Returns null if the value is not a
     * syntactically valid SPIFFE ID (wildcard suffix, if present, is ignored for this purpose).
     */
    public static String trustDomainOf(String spiffeId) {
        if (StringUtils.isBlank(spiffeId)) {
            return null;
        }
        String toParse = spiffeId.endsWith(WILDCARD_SUFFIX)
                ? spiffeId.substring(0, spiffeId.length() - WILDCARD_SUFFIX.length())
                : spiffeId;
        try {
            final URI uri = new URI(toParse);
            final String host = uri.getHost();
            return host != null ? host.toLowerCase() : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Matches a presented (concrete) SPIFFE ID against a registered SPIFFE ID pattern.
     * <p>
     * A registered pattern with a trailing "/*" matches any presented ID that shares its trust
     * domain and whose path starts with the pattern's path as a full path-segment prefix, e.g.
     * "spiffe://example.org/client/*" matches "spiffe://example.org/client/123" but not
     * "spiffe://example.org/client123". A registered pattern without a wildcard requires an
     * exact match.
     */
    public static boolean matches(String registeredSpiffeId, String presentedSpiffeId) {
        if (StringUtils.isBlank(registeredSpiffeId) || StringUtils.isBlank(presentedSpiffeId)) {
            return false;
        }

        if (!registeredSpiffeId.endsWith(WILDCARD_SUFFIX)) {
            return registeredSpiffeId.equals(presentedSpiffeId);
        }

        final String prefix = registeredSpiffeId.substring(0, registeredSpiffeId.length() - WILDCARD_SUFFIX.length());
        if (!presentedSpiffeId.startsWith(prefix + "/")) {
            return false;
        }

        // require an exact trust domain match, not merely a string-prefix coincidence
        final String registeredTrustDomain = trustDomainOf(registeredSpiffeId);
        final String presentedTrustDomain = trustDomainOf(presentedSpiffeId);
        return registeredTrustDomain != null && registeredTrustDomain.equals(presentedTrustDomain);
    }

    /**
     * True if the registered `spiffe_id` metadata value is a wildcard pattern (ends with "/*"),
     * meaning multiple concrete SVIDs under that prefix can authenticate as the same client.
     */
    public static boolean isWildcard(String registeredSpiffeId) {
        return registeredSpiffeId != null && registeredSpiffeId.endsWith(WILDCARD_SUFFIX);
    }
}
