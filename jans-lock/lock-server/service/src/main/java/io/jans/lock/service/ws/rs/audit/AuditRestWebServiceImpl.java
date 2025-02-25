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

import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_HEALTH;
import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_HEALTH_BULK;
import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_LOG;
import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_LOG_BULK;
import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_TELEMETRY;
import static io.jans.lock.service.audit.AuditForwarderService.AUDIT_TELEMETRY_BULK;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.DataMapperService;
import io.jans.lock.service.audit.AuditForwarderService;
import io.jans.lock.service.stat.StatService;
import io.jans.lock.util.ServerUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for audit REST web services
 *  
 * @author Yuriy Movchan Date: 06/06/2024
 */
@Dependent
@Path("/audit")
public class AuditRestWebServiceImpl implements AuditRestWebService {

	private static final String LOG_PRINCIPAL_ID = "principalId";
	private static final String LOG_CLIENT_ID = "clientId";
	private static final String LOG_DECISION_RESULT = "decisionResult";
	private static final String LOG_ACTION = "action";

	private static final String LOG_DECISION_RESULT_ALLOW = "allow";
	private static final String LOG_DECISION_RESULT_DENY = "deny";

	@Inject
    private Logger log;
    
    @Inject
    private DataMapperService dataMapperService;

    @Inject
    private AuditForwarderService auditForwarderService;
    
    @Inject
    private StatService statService;

    @Override
    public Response processHealthRequest(HttpServletRequest request, HttpServletResponse response,
            SecurityContext sec) {
        log.info("Processing Health request - request: {}", request);
        return processAuditRequest(request, AUDIT_HEALTH);
    }

	@Override
	public Response processBulkHealthRequest(HttpServletRequest request, HttpServletResponse response,
			SecurityContext sec) {
        log.info("Processing Bulk Health request - request: {}", request);
        return processAuditRequest(request, AUDIT_HEALTH_BULK);
	}

    @Override
    public Response processLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Log request - request: {}", request);
        return processAuditRequest(request, AUDIT_LOG, true, false);
    }

	@Override
	public Response processBulkLogRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Bulk Log request - request: {}", request);
        return processAuditRequest(request, AUDIT_LOG_BULK, true, true);
	}

    @Override
    public Response processTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Telemetry request - request: {}", request);
        return processAuditRequest(request, AUDIT_TELEMETRY);

    }

	@Override
	public Response processBulkTelemetryRequest(HttpServletRequest request, HttpServletResponse response, SecurityContext sec) {
        log.info("Processing Bulk Telemetry request - request: {}", request);
        return processAuditRequest(request, AUDIT_TELEMETRY_BULK);
	}

	private Response processAuditRequest(HttpServletRequest request, String requestType) {
    	return processAuditRequest(request, requestType, false, false);
    }

    private Response processAuditRequest(HttpServletRequest request, String requestType, boolean reportStat, boolean bulkData) {
        log.info("Processing request - request: {}, requestType: {}", request, requestType);

        Response.ResponseBuilder builder = Response.ok();
        builder.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate());
        builder.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE);

        JsonNode json = getJsonNode(request);
		if (json == null) {
			builder.status(Status.BAD_REQUEST);
			return builder.build();
		}

        if (reportStat) {
        	if (bulkData) {
        		reportBulkStat(json);
        	} else {
        		reportStat(json);
        	}
        }

        Response response = this.auditForwarderService.post(requestType, json.toString(), ContentType.APPLICATION_JSON);
		log.debug("Response: {}", response);

        if (response != null) {
			log.debug("Get response with status: {}, statusInfo: {}, entityClass: {}", response.getStatus(),
					response.getStatusInfo(), response.getEntity().getClass());
			
            String entity = response.readEntity(String.class);
            builder.entity(entity);
			log.debug("Response entity: {}", entity);

            if (response.getStatusInfo().equals(Status.OK)) {
				log.debug(" Status: {}, entity: {}", response.getStatus(), entity);
            } else {
				log.error("Error while saving audit data, statusInfo: {}, entity: {}", response.getStatusInfo(), entity);
                builder.status(response.getStatusInfo());
            }
        }

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

	private void reportBulkStat(JsonNode json) {
		if (!json.isArray()) {
			log.error("Failed to calculate stat for bulk log entry: {}", json);
		}
		
		for (JsonNode jsonItem : json) {
			reportStat(jsonItem);
		}
		
	}
}
