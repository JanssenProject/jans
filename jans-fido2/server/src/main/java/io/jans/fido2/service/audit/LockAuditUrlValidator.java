/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.audit;

import java.net.URI;
import java.net.URISyntaxException;

import io.jans.fido2.exception.Fido2RuntimeException;

/**
 * Shared HTTPS enforcement for every outbound Lock audit URL — CWE-319, the client secret
 * (discovery/token endpoint) and the bearer token plus audit payload (delivery endpoint) must
 * never cross the wire unencrypted. Centralised here after CodeRabbit found the discovery and
 * token-endpoint URLs in {@link LockAuditTokenService} lacked the same check already applied to
 * {@link LockAuditClient}'s delivery endpoint — one validator both route through, rather than two
 * copies that can drift.
 */
final class LockAuditUrlValidator {

	private LockAuditUrlValidator() {
	}

	/**
	 * @param what identifies the offending value in the thrown message (e.g. "lockAuditEndpoint",
	 *             "issuer", "token_endpoint")
	 * @throws Fido2RuntimeException if {@code url} is not a valid URI, or its scheme is not
	 *         {@code https} (loopback {@code http} is permitted for local development)
	 */
	static void requireSecure(String url, String what) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new Fido2RuntimeException(what + " is not a valid URI: " + url, e);
		}

		String scheme = uri.getScheme();
		if ("https".equalsIgnoreCase(scheme)) {
			return;
		}
		if ("http".equalsIgnoreCase(scheme) && isLoopback(uri.getHost())) {
			return;
		}
		throw new Fido2RuntimeException(what + " must use https (loopback http permitted for local development): " + url);
	}

	private static boolean isLoopback(String host) {
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
	}
}
