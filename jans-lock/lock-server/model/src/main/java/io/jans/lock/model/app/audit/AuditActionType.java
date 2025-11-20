package io.jans.lock.model.app.audit;

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/**
 * @version November 03, 2025
 */
public enum AuditActionType {
	CEDARLING_AUTHZ_FILTER("CEDARLING_AUTHZ_FILTER"),
	OPENID_AUTHZ_FILTER("OPENID_AUTHZ_FILTER"),

	AUDIT_HEALTH_WRITE("AUDIT_HEALTH_WRITE"),
	AUDIT_HEALTH_BULK_WRITE("AUDIT_HEALTH_BULK_WRITE"),
	AUDIT_LOG_WRITE("AUDIT_LOG_WRITE"),
	AUDIT_LOG_BULK_WRITE("AUDIT_LOG_BULK_WRITE"),
	AUDIT_TELEMETRY_WRITE("AUDIT_TELEMETRY_WRITE"),
	AUDIT_TELEMETRY_BULK_WRITE("AUDIT_TELEMETRY_BULK_WRITE"),

	POLICIES_URI_LIST_READ("POLICIES_URI_LIST_READ"),
	POLICY_BY_URI_READ("POLICY_BY_URI_READ"),

	CONFIGURATION_READ("CONFIGURATION_READ"),
    SSA_READ("SSA_READ")
    ;

    private final String value;

    AuditActionType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
