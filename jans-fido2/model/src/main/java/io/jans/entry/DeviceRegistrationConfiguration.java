/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.entry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * U2F Device registration key
 *
 * @author Yuriy Movchan Date: 05/29/2015
 */
public class DeviceRegistrationConfiguration {

	@JsonProperty
	public final String publicKey;

	@JsonProperty
	public final String attestationCert;

	public DeviceRegistrationConfiguration(@JsonProperty("publicKey") String publicKey,
			@JsonProperty("attestationCert") String attestationCert) {
		this.publicKey = publicKey;
		this.attestationCert = attestationCert;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getAttestationCert() {
		return attestationCert;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeviceRegistrationConfiguration [publicKey=").append(publicKey).append(", attestationCert=").append(attestationCert).append("]");
		return builder.toString();
	}

}