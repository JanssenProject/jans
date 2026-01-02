/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service.ws.rs.audit;

import java.util.List;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import io.jans.lock.model.AuditEndpointType;
import io.jans.lock.model.app.audit.AuditActionType;
import io.jans.lock.model.app.audit.AuditLogEntry;
import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.AuditPersistenceMode;
import io.jans.lock.service.AuditService;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.audit.AuditForwarderService;
import io.jans.lock.service.stat.StatService;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.jans.lock.util.ServerUtil;
import io.jans.net.InetAddressUtility;
import io.jans.service.JsonService;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for audit REST web services
 * 
 * @author Yuriy Movchan Date: 06/06/2024
 */
@Dependent
public class AuditRestWebServiceImpl extends BaseResource implements AuditRestWebService {

	private static final String LOG_DECISION_RESULT = "decisionResult";
	private static final String LOG_ACTION = "action";

	private static final String LOG_DECISION_RESULT_ALLOW = "allow";
	private static final String LOG_DECISION_RESULT_DENY = "deny";

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private JsonService jsonService;

	@Inject
	private AuditForwarderService auditForwarderService;

	@Inject
	private AuditService auditService;

	@Inject
	private StatService statService;

	@Inject
	private ApplicationAuditLogger applicationAuditLogger;

	/**
	 * Processes an incoming health audit request with typed HealthEntry bean.
	 *
	 * @param healthEntry the health entry bean
	 * @param request the HTTP servlet request
	 * @param sec the security context
	 * @return a Response containing the HTTP response to return for the health audit request
	 */
	@Override
	public Response processHealthRequest(HealthEntry healthEntry, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Health request - healthEntry: {}", healthEntry);

		if (healthEntry == null) {
			throwBadRequestException("Health entry is required");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_HEALTH_WRITE);

		Response response = null;
		try {
			response = processAuditRequest(healthEntry, AuditEndpointType.HEALTH);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Handles incoming bulk health audit requests with typed list of HealthEntry beans.
	 *
	 * @param healthEntries list of health entry beans
	 * @param request the HTTP servlet request
	 * @param sec the security context
	 * @return the Response representing the outcome of processing the bulk health audit request
	 */
	@Override
	public Response processBulkHealthRequest(List<HealthEntry> healthEntries, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Bulk Health request - entries count: {}", healthEntries != null ? healthEntries.size() : 0);

		if (healthEntries == null || healthEntries.isEmpty()) {
			throwBadRequestException("Health entries list is required and cannot be empty");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_HEALTH_BULK_WRITE);

		Response response = null;
		try {
			response = processBulkAuditRequest(healthEntries, AuditEndpointType.HEALTH_BULK);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Handle an incoming audit log request with typed LogEntry bean.
	 *
	 * @param logEntry the log entry bean
	 * @param request the HTTP servlet request
	 * @param sec the security context
	 * @return a JAX-RS Response containing the processing result
	 */
	@Override
	public Response processLogRequest(LogEntry logEntry, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Log request - logEntry: {}", logEntry);

		if (logEntry == null) {
			throwBadRequestException("Log entry is required");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_LOG_WRITE);

		Response response = null;
		try {
			// Report statistics for single log entry
			reportLogStat(logEntry);
			response = processAuditRequest(logEntry, AuditEndpointType.LOG);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Handles an incoming bulk log audit request with typed list of LogEntry beans.
	 *
	 * @param logEntries list of log entry beans
	 * @param request the HTTP request
	 * @param sec the security context for the request
	 * @return the HTTP response representing the processing result
	 */
	@Override
	public Response processBulkLogRequest(List<LogEntry> logEntries, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Bulk Log request - entries count: {}", logEntries != null ? logEntries.size() : 0);

		if (logEntries == null || logEntries.isEmpty()) {
			throwBadRequestException("Log entries list is required and cannot be empty");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_LOG_BULK_WRITE);

		Response response = null;
		try {
			// Report statistics for bulk log entries
			for (LogEntry entry : logEntries) {
				reportLogStat(entry);
			}
			response = processBulkAuditRequest(logEntries, AuditEndpointType.LOG_BULK);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Handle an incoming telemetry audit request with typed TelemetryEntry bean.
	 *
	 * @param telemetryEntry the telemetry entry bean
	 * @param request the HTTP servlet request
	 * @param sec the security context for the request
	 * @return a Response representing the result of processing the telemetry audit request
	 */
	@Override
	public Response processTelemetryRequest(TelemetryEntry telemetryEntry, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Telemetry request - telemetryEntry: {}", telemetryEntry);

		if (telemetryEntry == null) {
			throwBadRequestException("Telemetry entry is required");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_TELEMETRY_WRITE);

		Response response = null;
		try {
			response = processAuditRequest(telemetryEntry, AuditEndpointType.TELEMETRY);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Handles an incoming bulk telemetry audit request with typed list of TelemetryEntry beans.
	 *
	 * @param telemetryEntries list of telemetry entry beans
	 * @param request the HTTP servlet request
	 * @param sec the security context
	 * @return a Response representing the result of processing the bulk telemetry audit request
	 */
	@Override
	public Response processBulkTelemetryRequest(List<TelemetryEntry> telemetryEntries, HttpServletRequest request, SecurityContext sec) {
		log.info("Processing Bulk Telemetry request - entries count: {}", telemetryEntries != null ? telemetryEntries.size() : 0);

		if (telemetryEntries == null || telemetryEntries.isEmpty()) {
			throwBadRequestException("Telemetry entries list is required and cannot be empty");
		}

		AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(request),
				AuditActionType.AUDIT_TELEMETRY_BULK_WRITE);

		Response response = null;
		try {
			response = processBulkAuditRequest(telemetryEntries, AuditEndpointType.TELEMETRY_BULK);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}

		return response;
	}

	/**
	 * Process a single audit request with typed entry object.
	 *
	 * @param entry the audit entry object (HealthEntry, LogEntry, or TelemetryEntry)
	 * @param requestType the audit endpoint type
	 * @return a JAX-RS Response
	 */
	private Response processAuditRequest(Object entry, AuditEndpointType requestType) {
		log.info("Processing single audit request - requestType: {}", requestType);

		Response.ResponseBuilder builder = Response.ok();

		try {
			String response;
			if (AuditPersistenceMode.CONFIG_API.equals(appConfiguration.getAuditPersistenceMode())) {
				String json = jsonService.objectToJson(entry);
				response = auditForwarderService.post(builder, requestType, json, ContentType.APPLICATION_JSON);
			} else {
				response = persistAuditData(builder, requestType, entry);
			}

			builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
			builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
			builder.entity(response);
			
			log.debug("Response entity: {}", response);
		} catch (Exception ex) {
			builder.status(Status.INTERNAL_SERVER_ERROR);
			log.error("Failed to process audit request", ex);
			builder.entity("Failed to process audit request");
		}

		return builder.build();
	}

	/**
	 * Process bulk audit requests with typed list of entry objects.
	 *
	 * @param entries list of audit entry objects
	 * @param requestType the audit endpoint type
	 * @return a JAX-RS Response
	 */
	private Response processBulkAuditRequest(List<?> entries, AuditEndpointType requestType) {
		log.info("Processing bulk audit request - requestType: {}, count: {}", requestType, entries.size());

		Response.ResponseBuilder builder = Response.ok();

		try {
			String response;

			if (AuditPersistenceMode.CONFIG_API.equals(appConfiguration.getAuditPersistenceMode())) {
				String json = jsonService.objectToJson(entries);
				response = auditForwarderService.post(builder, requestType, json, ContentType.APPLICATION_JSON);
			} else {
				response = persistBulkAuditData(builder, requestType, entries);
			}

			builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
			builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
			builder.entity(response);
			
			log.debug("Response entity: {}", response);
		} catch (Exception ex) {
			builder.status(Status.INTERNAL_SERVER_ERROR);
			log.error("Failed to process bulk audit request", ex);
			builder.entity("Failed to process bulk audit request");
		}

		return builder.build();
	}

	/**
	 * Report statistics for a single log entry.
	 *
	 * @param logEntry the log entry to report statistics for
	 */
	private void reportLogStat(LogEntry logEntry) {
		if (logEntry.getClientId() != null) {
			statService.reportActiveClient(logEntry.getClientId());
		}

		if (logEntry.getPrincipalId() != null) {
			statService.reportActiveUser(logEntry.getPrincipalId());
		}

		if (logEntry.getDecisionResult() != null) {
			String decisionResult = logEntry.getDecisionResult();
			if (LOG_DECISION_RESULT_ALLOW.equals(decisionResult)) {
				statService.reportAllow(LOG_DECISION_RESULT);
			} else if (LOG_DECISION_RESULT_DENY.equals(decisionResult)) {
				statService.reportDeny(LOG_DECISION_RESULT);
			}
		}

		if (logEntry.getAction() != null) {
			statService.reportOpearation(LOG_ACTION, logEntry.getAction());
		}
	}

	/**
	 * Persist a single audit entry.
	 *
	 * @param builder response builder
	 * @param requestType the audit endpoint type
	 * @param entry the audit entry object
	 * @return empty string on success, error message on failure
	 */
	private String persistAuditData(ResponseBuilder builder, AuditEndpointType requestType, Object entry) {
		try {
			switch (requestType) {
			case LOG:
				auditService.addLogEntry((LogEntry) entry);
				break;
			case HEALTH:
				auditService.addHealthEntry((HealthEntry) entry);
				break;
			case TELEMETRY:
				auditService.addTelemetryEntry((TelemetryEntry) entry);
				break;
			default:
				builder.status(Status.BAD_REQUEST);
				return "Invalid request type for single entry";
			}
		} catch (Exception ex) {
			builder.status(Status.INTERNAL_SERVER_ERROR);
			log.error("Failed to persist audit data", ex);
			return "Failed to persist data";
		}
		return "";
	}

	/**
	 * Persist bulk audit entries.
	 *
	 * @param builder response builder
	 * @param requestType the audit endpoint type
	 * @param entries list of audit entry objects
	 * @return empty string on success, error message on failure
	 */
	@SuppressWarnings("unchecked")
	private String persistBulkAuditData(ResponseBuilder builder, AuditEndpointType requestType, List<?> entries) {
		try {
			switch (requestType) {
			case LOG_BULK:
				List<LogEntry> logEntries = (List<LogEntry>) entries;
				for (LogEntry entry : logEntries) {
					auditService.addLogEntry(entry);
				}
				break;
			case HEALTH_BULK:
				List<HealthEntry> healthEntries = (List<HealthEntry>) entries;
				for (HealthEntry entry : healthEntries) {
					auditService.addHealthEntry(entry);
				}
				break;
			case TELEMETRY_BULK:
				List<TelemetryEntry> telemetryEntries = (List<TelemetryEntry>) entries;
				for (TelemetryEntry entry : telemetryEntries) {
					auditService.addTelemetryEntry(entry);
				}
				break;
			default:
				builder.status(Status.BAD_REQUEST);
				return "Invalid request type for bulk entries";
			}
		} catch (Exception ex) {
			builder.status(Status.INTERNAL_SERVER_ERROR);
			log.error("Failed to persist bulk audit data", ex);
			return "Failed to persist bulk data";
		}
		return "";
	}

}