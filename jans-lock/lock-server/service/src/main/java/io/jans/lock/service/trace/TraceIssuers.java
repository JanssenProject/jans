/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * The issuer-URI rule shared by {@code tokens[].issuer} (design §7.3) and
 * {@code trace.session_issuer}.
 *
 * @author Yuriy Movchan
 */
public final class TraceIssuers {

	static final String CUSTOM_ISSUER_URN_PREFIX = "urn:jans:cedarling:custom-issuer:";

	/**
	 * RFC 8141 {@code pchar} without {@code /}: unreserved, percent-encoded, sub-delims, {@code :}
	 * and {@code @}. Excludes {@code ?} and {@code #}, so the r-, q- and f-components are rejected.
	 */
	private static final Pattern CUSTOM_ISSUER_NAME = Pattern.compile("(?:[A-Za-z0-9._~!$&'()*+,;=:@-]|%[0-9A-Fa-f]{2})+");

	/**
	 * An absolute URI with scheme {@code https}, or {@code http} only when the host is
	 * {@code localhost} (documented MVP relaxation for local/dev producers).
	 */
	public static boolean isValid(String issuer) {
		URI uri;
		try {
			uri = new URI(issuer);
		} catch (URISyntaxException ex) {
			return false;
		}
		String scheme = uri.getScheme();
		if (!uri.isAbsolute() || scheme == null || uri.isOpaque() || uri.getHost() == null) {
			return false;
		}
		return "https".equalsIgnoreCase(scheme)
				|| ("http".equalsIgnoreCase(scheme) && "localhost".equalsIgnoreCase(uri.getHost()));
	}

	/**
	 * {@link #isValid(String)}, or a custom-issuer URN {@code urn:jans:cedarling:custom-issuer:<name>}
	 * for credentials without an issuer URI (opaque tokens, API keys). The prefix is matched
	 * case-sensitively and {@code <name>} is compared exactly, so there is no case folding.
	 * {@code <name>} must be non-empty; {@code ?+}, {@code ?=} and {@code #} components are rejected
	 * because the issuer is an identity key and must have one spelling.
	 */
	public static boolean isValidTokenIssuer(String issuer) {
		return isValid(issuer) || isCustomIssuerUrn(issuer);
	}

	private static boolean isCustomIssuerUrn(String issuer) {
		if (issuer == null || !issuer.startsWith(CUSTOM_ISSUER_URN_PREFIX)) {
			return false;
		}
		return CUSTOM_ISSUER_NAME.matcher(issuer.substring(CUSTOM_ISSUER_URN_PREFIX.length())).matches();
	}

	private TraceIssuers() {
	}

}
