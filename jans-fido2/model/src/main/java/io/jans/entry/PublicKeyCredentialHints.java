/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.entry;

/**
 * PublicKeyCredentialHints
 * https://w3c.github.io/webauthn/#enumdef-publickeycredentialhints
 *
 * @author Madhumita S. Date: 28/08/2024
 */
public enum PublicKeyCredentialHints {

	SECURITY_KEY("security-key"), CLIENT_DEVICE("client-device"), HYBRID("hybrid");

	private final String hint;

	private PublicKeyCredentialHints(String hint) {
		this.hint = hint;
	}

	public String getValue() {
		return hint;
	}

}