/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.util.Base64;

/**
 * Strict base64url (RFC 4648 §5) codec without padding, as required by JWK {@code x} members
 * (RFC 7517 / RFC 8037) and JWS signatures (RFC 7515 §2, "Base64url Encoding").
 *
 * <p>{@link #decode(String)} accepts only the 64-character URL-safe alphabet, no padding, no
 * whitespace, a length of 0, 2 or 3 (mod 4), and only canonical encodings (the unused trailing
 * bits of the last character must be zero). Everything else is rejected with
 * {@link IllegalArgumentException}, unlike the lenient commons-codec based
 * {@code io.jans.as.model.util.Base64Util}.
 *
 * @author Yuriy Movchan
 */
public final class StrictBase64Url {

	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	private StrictBase64Url() {
	}

	/**
	 * @return the unpadded base64url encoding of {@code bytes}
	 * @throws IllegalArgumentException if {@code bytes} is null
	 */
	public static String encode(byte[] bytes) {
		if (bytes == null) {
			throw new IllegalArgumentException("bytes must not be null");
		}
		return ENCODER.encodeToString(bytes);
	}

	/**
	 * @return the decoded bytes; an empty string decodes to an empty array
	 * @throws IllegalArgumentException if {@code s} is null, contains a character outside
	 *         {@code [A-Za-z0-9_-]} (this includes padding and whitespace), has length 1 (mod 4),
	 *         or is not the canonical encoding of its decoded value
	 */
	public static byte[] decode(String s) {
		if (s == null) {
			throw new IllegalArgumentException("base64url input must not be null");
		}
		int length = s.length();
		if ((length % 4) == 1) {
			throw new IllegalArgumentException("base64url input has invalid length " + length);
		}
		for (int i = 0; i < length; i++) {
			if (!isAlphabet(s.charAt(i))) {
				throw new IllegalArgumentException("base64url input contains an invalid character at index " + i);
			}
		}

		// The JDK decoder tolerates non-zero trailing bits in the final character; the RFC 4648
		// §3.5 canonical check is done by re-encoding and comparing.
		byte[] decoded = DECODER.decode(s);
		if (!s.equals(ENCODER.encodeToString(decoded))) {
			throw new IllegalArgumentException("base64url input is not canonical (non-zero trailing bits)");
		}
		return decoded;
	}

	private static boolean isAlphabet(char c) {
		return ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '-')
				|| (c == '_');
	}

}
