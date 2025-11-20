package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
public enum AuditPersistenceMode {

	INTERNAL("internal"), CONFIG_API("config-api");

	private final String mode;

	/**
     * Create an enum constant with the specified string representation used for JSON serialization.
     *
     * @param mode the string value to use as this enum constant's JSON representation
     */
    private AuditPersistenceMode(String mode) {
        this.mode = mode;
    }

	/**
	 * Mode string used for JSON serialization of the enum constant.
	 *
	 * @return the enum's mode string ("internal" or "config-api")
	 */
	@JsonValue
	public String getMode() {
		return mode;
	}
}