package io.jans.lock.model;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
public enum AuditEndpointType {

	TELEMETRY("telemetry", "telemetry", "jans-config-api/lock/audit/telemetry",
			"https://jans.io/oauth/lock/telemetry.write"),
	TELEMETRY_BULK("telemetry_bulk", "telemetry/bulk", "jans-config-api/lock/audit/telemetry/bulk",
			"https://jans.io/oauth/lock/telemetry.write"),
	LOG("log", "log", "jans-config-api/lock/audit/log",
			"https://jans.io/oauth/lock/log.write"),
	LOG_BULK("log_bulk", "log/bulk", "jans-config-api/lock/audit/log/bulk",
			"https://jans.io/oauth/lock/log.write"),
	HEALTH("health", "health", "jans-config-api/lock/audit/health",
			"https://jans.io/oauth/lock/health.write"),
	HEALTH_BULK("health_bulk", "health/bulk", "jans-config-api/lock/audit/health/bulk",
			"https://jans.io/oauth/lock/health.write");

	private String type;
	private String configPath;
	private String path;
	private String[] scopes;

	/**
	 * Create an AuditEndpointType with its identifier, endpoint path, configuration path, and allowed OAuth scopes.
	 *
	 * @param type       the logical type identifier for the endpoint (e.g., "telemetry", "log")
	 * @param path       the HTTP endpoint path segment (e.g., "telemetry", "log/bulk")
	 * @param configPath the configuration store path for this endpoint
	 * @param scopes     zero or more OAuth scope strings required to access this endpoint
	 */
	AuditEndpointType(String type, String path, String configPath, String ... scopes) {
		this.type = type;
		this.configPath = configPath;
		this.path = path;
		this.scopes = scopes;
	}

	/**
	 * The endpoint type identifier for this enum constant.
	 *
	 * @return the type identifier (for example, "telemetry")
	 */
	public String getType() {
		return type;
	}

	/**
	 * Configuration path for this audit endpoint.
	 *
	 * @return the configuration path associated with this enum constant
	 */
	public String getConfigPath() {
		return configPath;
	}

	/**
	 * Endpoint path used to reach this audit endpoint.
	 *
	 * @return the path string for this audit endpoint
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Gets the OAuth scopes associated with this audit endpoint.
	 *
	 * @return an array of OAuth scope strings associated with this endpoint
	 */
	public String[] getScopes() {
		return scopes;
	}

}