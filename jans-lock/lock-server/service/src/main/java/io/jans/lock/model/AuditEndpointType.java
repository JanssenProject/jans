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

	AuditEndpointType(String type, String path, String configPath, String ... scopes) {
		this.type = type;
		this.configPath = configPath;
		this.path = path;
		this.scopes = scopes;
	}

	public String getType() {
		return type;
	}

	public String getConfigPath() {
		return configPath;
	}

	public String getPath() {
		return path;
	}

	public String[] getScopes() {
		return scopes;
	}

}
