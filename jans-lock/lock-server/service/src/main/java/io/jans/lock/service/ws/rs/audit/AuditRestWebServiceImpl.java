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

import java.io.IOException;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.model.AuditEndpointType;
import io.jans.lock.model.app.audit.AuditActionType;
import io.jans.lock.model.app.audit.AuditLogEntry;
import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.AuditPersistenceMode;
import io.jans.lock.service.AuditService;
import io.jans.lock.service.DataMapperService;
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
import jakarta.servlet.http.HttpServletResponse;
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

	private static final String LOG_PRINCIPAL_ID = "principalId";
	private static final String LOG_CLIENT_ID = "clientId";
	private static final String LOG_DECISION_RESULT = "decisionResult";
	private static final String LOG_ACTION = "action";

	private static final String LOG_DECISION_RESULT_ALLOW = "allow";
	private static final String LOG_DECISION_RESULT_DENY = "deny";

	@Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;
    
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
     * Processes an incoming health audit request and delegates handling to the audit processor.
     *
     * @return a Response containing the HTTP response to return for the health audit request
     */
    @Override
    public Response processHealthRequest(HttpServletRequest request, HttpServletResponse response,
            SecurityContext sec) {
        log.info("Processing Health request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_HEALTH_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.HEALTH);
    }

	/**
	 * Handles incoming bulk health audit requests.
	 *
	 * Produces a Response representing the outcome of processing the bulk health audit payload.
	 *
	 * @return the Response representing the outcome of processing the bulk health audit request
	 */
	@Override
	public Response processBulkHealthRequest(HttpServletRequest request, HttpServletResponse response,
			SecurityContext sec) {
        log.info("Processing Bulk Health request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_HEALTH_BULK_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.HEALTH_BULK);
	}

    /**
     * Handle an incoming audit log request for a single log event and process it while reporting statistics.
     *
     * @param request the HTTP servlet request containing the log JSON payload
     * @param response the HTTP servlet response (unused by this method but provided by the servlet layer)
     * @param sec the security context for the request
     * @return a JAX-RS Response containing the processing result; `400 BAD_REQUEST` on parse failure, `200 OK` on success
     */
    @Override
    public Response processLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Log request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_LOG_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.LOG, true, false);
    }

	/**
	 * Handles an incoming bulk log audit request, reports relevant statistics, and delegates processing.
	 *
	 * @param request  the HTTP request containing the bulk log payload
	 * @param response the HTTP response
	 * @param sec      the security context for the request
	 * @return the HTTP response representing the processing result; status indicates success or failure
	 */
	@Override
	public Response processBulkLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Bulk Log request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_LOG_BULK_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.LOG_BULK, true, true);
	}

    /**
     * Handle an incoming telemetry audit request.
     *
     * @param request the HTTP servlet request containing the telemetry payload
     * @param response the HTTP servlet response
     * @param sec the security context for the request
     * @return a Response representing the result of processing the telemetry audit request
     */
    @Override
    public Response processTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Telemetry request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_TELEMETRY_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.TELEMETRY);
    }

	/**
	 * Handles an incoming bulk telemetry audit request.
	 *
	 * @return a Response representing the result of processing the bulk telemetry audit request
	 */
	@Override
	public Response processBulkTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Bulk Telemetry request - request: {}", request);

        AuditLogEntry auditLogEntry = new AuditLogEntry(InetAddressUtility.getIpAddress(getHttpRequest()), AuditActionType.AUDIT_TELEMETRY_BULK_WRITE);
        applicationAuditLogger.log(auditLogEntry);

        return processAuditRequest(request, AuditEndpointType.TELEMETRY_BULK);
	}

	/**
     * Delegates processing of an audit HTTP request to the main processor using default flags
     * (do not report statistics, not bulk data).
     *
     * @param request     the incoming HTTP servlet request containing the audit JSON payload
     * @param requestType the audit endpoint type indicating which audit path to process
     * @return the JAX-RS response produced by processing the audit request
     */
    private Response processAuditRequest(HttpServletRequest request, AuditEndpointType requestType) {
    	return processAuditRequest(request, requestType, false, false);
    }

    /**
     * Process an incoming audit HTTP request: parse its JSON payload, optionally report statistics,
     * then either forward the payload to the configured audit API or persist it locally, and return
     * an HTTP response containing the operation result.
     *
     * @param request the HTTP request containing the audit JSON payload
     * @param requestType the audit endpoint type (HEALTH, LOG, TELEMETRY or their bulk variants)
     * @param reportStat when true, report usage/operation statistics extracted from the payload
     * @param bulkData when true, treat the payload as an array of entries for bulk reporting
     * @return a JAX-RS Response whose entity is the result message from forwarding or persistence;
     *         the response is marked private and no-store with a Pragma: no-cache header
     */
    private Response processAuditRequest(HttpServletRequest request, AuditEndpointType requestType, boolean reportStat, boolean bulkData) {
        log.info("Processing request - request: {}, requestType: {}", request, requestType);

        JsonNode json = getJsonNode(request);
		if (json == null) {
			throwBadRequestException("Failed to parse request");
		}

        if (reportStat) {
        	if (bulkData) {
        		reportBulkStat(json);
        	} else {
        		reportStat(json);
        	}
        }

        Response.ResponseBuilder builder = Response.ok();

        String response;
		if (AuditPersistenceMode.CONFIG_API.equals(appConfiguration.getAuditPersistenceMode())) {
	        response = auditForwarderService.post(builder, requestType, json.toString(), ContentType.APPLICATION_JSON);
		} else {
			response = persistetAuditData(builder, requestType, json.toString());
		}

        builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
        builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);
		
        builder.entity(response);
		log.debug("Response entity: {}", response);

        return builder.build();
    }

	private JsonNode getJsonNode(HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		JsonNode jsonBody = null;
		try {
			jsonBody = dataMapperService.readTree(request.getInputStream());
			log.debug("Parsed request body data: {}", jsonBody);
		} catch (Exception ex) {
			log.error("Failed to parse request", ex);
		}

		return jsonBody;
	}

	private void reportStat(JsonNode json) {
		boolean hasClientId = json.hasNonNull(LOG_CLIENT_ID);
		if (hasClientId) {
			statService.reportActiveClient(json.get(LOG_CLIENT_ID).asText());
		}

		boolean hasPrincipalId = json.hasNonNull(LOG_PRINCIPAL_ID);
		if (hasPrincipalId) {
			statService.reportActiveUser(json.get(LOG_PRINCIPAL_ID).asText());
		}

		boolean hasВecisionResult = json.hasNonNull(LOG_DECISION_RESULT);
		if (hasВecisionResult) {
			String decisionResult = json.get(LOG_DECISION_RESULT).asText();
			if (LOG_DECISION_RESULT_ALLOW.equals(decisionResult)) {
				statService.reportAllow(LOG_DECISION_RESULT);
			} else if (LOG_DECISION_RESULT_DENY.equals(decisionResult)) {
				statService.reportDeny(LOG_DECISION_RESULT);
			}
		}

		boolean hasAction = json.hasNonNull(LOG_ACTION);
		if (hasAction) {
			statService.reportOpearation(LOG_ACTION, json.get(LOG_ACTION).asText());
		}
	}

	/**
	 * Reports statistics for each element of a JSON array representing bulk audit entries.
	 *
	 * If the provided node is not a JSON array, an error is logged and the method still attempts to process its elements.
	 *
	 * @param json JSON array of audit entries whose elements will be processed to report statistics
	 */
	private void reportBulkStat(JsonNode json) {
		if (!json.isArray()) {
			log.error("Failed to calculate stat for bulk log entry: {}", json);
		}
		
		for (JsonNode jsonItem : json) {
			reportStat(jsonItem);
		}
		
	}

	/**
	 * Persist audit data for the given request type.
	 *
	 * <p>Parses the provided JSON payload into the appropriate audit entry or entries
	 * based on {@code requestType} and persists them via the audit service.</p>
	 *
	 * @param builder     response builder that will be updated to BAD_REQUEST on parse failure
	 * @param requestType the type of audit endpoint (log, health, telemetry, or their bulk variants)
	 * @param json        the JSON payload to parse and persist
	 * @return            an empty string on success, or the message "Failed to parse data" if parsing failed
	 */
	private String persistetAuditData(ResponseBuilder builder, AuditEndpointType requestType, String json) {
		try {
			switch (requestType) {
				case LOG:
					LogEntry logEntry = jsonService.jsonToObject(json, LogEntry.class);
					auditService.addLogEntry(logEntry);
					break;
				case LOG_BULK:
					List<LogEntry> logEntries = jsonService.jsonToObject(json, jsonService.getTypeFactory().constructCollectionType(List.class, LogEntry.class));
					for (LogEntry entry : logEntries) {
						auditService.addLogEntry(entry);
					}
					break;
				case HEALTH:
					HealthEntry healthEntry = jsonService.jsonToObject(json, HealthEntry.class);
					auditService.addHealthEntry(healthEntry);
					break;
				case HEALTH_BULK:
					List<HealthEntry> healthEntries = jsonService.jsonToObject(json, jsonService.getTypeFactory().constructCollectionType(List.class, HealthEntry.class));
					for (HealthEntry entry : healthEntries) {
						auditService.addHealthEntry(entry);
					}
					break;
				case TELEMETRY:
					TelemetryEntry telemetryEntry = jsonService.jsonToObject(json, TelemetryEntry.class);
					auditService.addTelemetryEntry(telemetryEntry);
					break;
				case TELEMETRY_BULK:
					List<TelemetryEntry> telemetryEntries = jsonService.jsonToObject(json, jsonService.getTypeFactory().constructCollectionType(List.class, TelemetryEntry.class));
					for (TelemetryEntry entry : telemetryEntries) {
						auditService.addTelemetryEntry(entry);
					}
					break;
			}
		} catch (IOException ex) {
			builder.status(Status.BAD_REQUEST);
			log.warn("Failed to parse data", ex);

			return "Failed to parse data";
		} catch (Exception ex) {
			builder.status(Status.INTERNAL_SERVER_ERROR);
			log.error("Failed to persist audit data", ex);

			return "Failed to persist data";
		}
		return "";
	}

}