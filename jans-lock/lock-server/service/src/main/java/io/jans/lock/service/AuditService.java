package io.jans.lock.service;

import java.util.UUID;

import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
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
	private PersistenceEntryManager persistenceEntryManager;

	/**
	 * Assigns a unique inum and corresponding DN to the provided log entry and persists it.
	 *
	 * If `logEntry` is null, the method performs no action.
	 *
	 * @param logEntry the log entry to assign identifiers to and persist; may be null
	 */
	public void addLogEntry(LogEntry logEntry) {
		if (logEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_LOG_FORMAT, LogEntry.class);
		logEntry.setInum(inum);
		logEntry.setDn(this.getDnForLogEntry(inum));

		persistenceEntryManager.persist(logEntry);
	}

	/**
	 * Persists a TelemetryEntry after assigning it a unique inum and DN.
	 *
	 * Generates a unique inum for the entry, sets the inum and computed DN on the provided TelemetryEntry, and persists it. If the provided entry is null, no action is taken.
	 *
	 * @param telemetryEntry the telemetry entry to assign identifiers to and persist; ignored if null
	 */
	public void addTelemetryEntry(TelemetryEntry telemetryEntry) {
		if (telemetryEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_TELEMETRY_FORMAT, TelemetryEntry.class);
		telemetryEntry.setInum(inum);
		telemetryEntry.setDn(this.getDnForTelemetryEntry(inum));

		persistenceEntryManager.persist(telemetryEntry);
	}

	/**
	 * Persists a HealthEntry after assigning it a new unique inum and corresponding DN.
	 *
	 * @param healthEntry the HealthEntry to assign identifiers and persist; ignored if null
	 */
	public void addHealthEntry(HealthEntry healthEntry) {
		if (healthEntry == null) {
			return;
		}

		String inum = this.generateInumForEntry(DN_HEALTH_FORMAT, HealthEntry.class);
		healthEntry.setInum(inum);

		healthEntry.setDn(this.getDnForHealthEntry(inum));
		persistenceEntryManager.persist(healthEntry);
	}

	/**
	 * Builds the Distinguished Name (DN) for a log entry using the provided inum.
	 *
	 * @param inum the unique inum assigned to the log entry
	 * @return the DN for the log entry constructed with the provided inum and the audit base DN
	 */
	public String getDnForLogEntry(String inum) {
		return String.format(DN_LOG_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	/**
	 * Builds the distinguished name (DN) for a telemetry entry.
	 *
	 * @param inum the inum (unique identifier) of the telemetry entry
	 * @return the DN string corresponding to the telemetry entry
	 */
	public String getDnForTelemetryEntry(String inum) {
		return String.format(DN_TELEMETRY_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	/**
	 * Builds the distinguished name for a health audit entry.
	 *
	 * @param inum the entry's inum (unique identifier)
	 * @return the distinguished name (DN) for the health entry under the audit base DN
	 */
	public String getDnForHealthEntry(String inum) {
		return String.format(DN_HEALTH_FORMAT, inum, staticConfiguration.getBaseDn().getAudit());
	}

	/**
	 * Generate a candidate inum for an audit entry type, attempting to avoid DN collisions.
	 *
	 * Attempts up to MAX_IDGEN_TRY_COUNT times to produce an inum whose DN (dnFormat formatted with
	 * the generated inum and the organization's base DN) does not already exist. If a unique DN is
	 * found within the limit, that inum is returned; otherwise the last generated inum is returned.
	 *
	 * @param dnFormat a String format with two placeholders: the first for the inum and the second for the base DN
	 * @param classObj the entry class used when checking whether the constructed DN already exists
	 * @return the generated inum; if uniqueness could not be guaranteed within the retry limit, the last generated candidate
	 */
	public String generateInumForEntry(String dnFormat, Class classObj) {
		String baseDn = staticConfiguration.getBaseDn().getAudit();
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

	/**
	 * Generate a new random UUID string.
	 *
	 * @return a UUID string in standard 36-character format (hexadecimal with hyphens)
	 */
	private String generateId() {
		return UUID.randomUUID().toString();
	}

}