/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Yuriy Movchan Date: 15/1/2022
 */
public enum GrpcServerMode {

	DISABLED("disabled"), BRIDGE("bridge"), PLAIN_SERVER("plain_server"), TLS_SERVER("tls_server");

	private final String mode;

	/**
     * Create an enum constant with the specified string representation used for JSON serialization.
     *
     * @param mode the string value to use as this enum constant's JSON representation
     */
    private GrpcServerMode(String mode) {
        this.mode = mode;
    }

	/**
	 * Mode string used for JSON serialization of the enum constant.
	 *
	 * @return the enum's mode string
	 */
	@JsonValue
	public String getMode() {
		return mode;
	}
}