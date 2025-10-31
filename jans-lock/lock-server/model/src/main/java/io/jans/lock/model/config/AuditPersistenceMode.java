package io.jans.lock.model.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
public enum AuditPersistenceMode {

	INTERNAL("internal"), CONFIG_API("config-api");

	private final String mode;

	private AuditPersistenceMode(String mode) {
        this.mode = mode;
    }

	@JsonValue
	public String getmode() {
		return mode;
	}
}
