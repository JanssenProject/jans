/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
public enum LockProtectionMode {

	OAUTH("oauth"), CEDARLING("cedarling");

	private final String mode;

	/**
     * Creates a LockProtectionMode with the given string representation.
     *
     * @param mode the string value used to represent this enum constant (for JSON serialization)
     */
    private LockProtectionMode(String mode) {
        this.mode = mode;
    }

	/**
	 * The value used when this enum is serialized to JSON.
	 *
	 * @return the enum constant's mode string, e.g. "oauth" or "cedarling"
	 */
	@JsonValue
	public String getMode() {
		return mode;
	}
}