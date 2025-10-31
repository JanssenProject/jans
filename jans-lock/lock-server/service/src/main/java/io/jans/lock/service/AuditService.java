package io.jans.lock.service;

import java.util.UUID;

import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.OrganizationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Lock audit persistence services
 *
 * @author Yuriy Movchan Date: 12/19/2018
 */
@ApplicationScoped
public class AuditService {

	private static final String DN_LOG_FORMAT = "inum=%s,ou=log,%s";
	private static final String DN_TELEMETRY_FORMAT = "inum=%s,ou=telemetry,%s";
	private static final String DN_HEALTH_FORMAT = "inum=%s,ou=health,%s";

	private static final int MAX_IDGEN_TRY_COUNT = 10;

	@Inject
	private StaticConfiguration staticConfiguration;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	public void addLogEntry(LogEntry logEntry) {
		if (logEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_LOG_FORMAT, LogEntry.class);
		logEntry.setInum(inum);
		logEntry.setDn(this.getDnForLogEntry(inum));

		persistenceEntryManager.persist(logEntry);
	}

	public void addTelemetryEntry(TelemetryEntry telemetryEntry) {
		if (telemetryEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_TELEMETRY_FORMAT, TelemetryEntry.class);
		telemetryEntry.setInum(inum);
		telemetryEntry.setDn(this.getDnForTelemetryEntry(inum));

		persistenceEntryManager.persist(telemetryEntry);
	}

	public void addHealthEntry(HealthEntry healthEntry) {
		if (healthEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_HEALTH_FORMAT, HealthEntry.class);
		healthEntry.setInum(inum);

		healthEntry.setDn(this.getDnForHealthEntry(inum));
		persistenceEntryManager.persist(healthEntry);
	}

	public String getDnForLogEntry(String inum) {
		return String.format(DN_LOG_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	public String getDnForTelemetryEntry(String inum) {
		return String.format(DN_TELEMETRY_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	public String getDnForHealthEntry(String inum) {
		return String.format(DN_HEALTH_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	public String generateInumForEntry(String dnFormat, Class classObj) {
		String baseDn = organizationService.getBaseDn();
		String newInum = null;
		String newDn = null;

		int tryСount = 0;
		do {
			newInum = generateId();

			newDn = String.format(dnFormat, newInum, baseDn);
			boolean found = persistenceEntryManager.contains(newDn, classObj);
			if (!found) {
				return newInum;
			}
		} while (++tryСount < MAX_IDGEN_TRY_COUNT);

		return newInum;
	}

	private String generateId() {
		return UUID.randomUUID().toString();
	}

}
