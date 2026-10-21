/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Log audit entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansLogEntry")
public class LogEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -7086771090254438974L;

	public static final String CONTEXT_REQUEST_ID = "request_id";
	public static final String CONTEXT_POLICY_STORE_ID = "policystore_id";
	public static final String CONTEXT_POLICY_STORE_VERSION = "policystore_version";

	public static final int MAX_REQUEST_ID_LENGTH = 64;
	public static final int MAX_POLICY_STORE_FIELD_LENGTH = 255;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @JsonProperty("creationDate")
    @JsonAlias("creation_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @Schema(description = "Creation date of the entry", example = "2026-07-24T15:33:02.937Z")
    @AttributeName(name = "creationDate")
    private Date creationDate;

    @JsonProperty("eventTime")
    @JsonAlias("event_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @Schema(description = "Time when the event occurred", example = "2026-07-24T15:33:02.937Z")
    @AttributeName(name = "eventTime")
    private Date eventTime;

    @JsonProperty("service")
    @Schema(description = "Service name", example = "Lock Server")
    @AttributeName(name = "jansService")
    private String service;

    @JsonProperty("nodeName")
    @JsonAlias("node_name")
    @Schema(description = "Node name or identifier", example = "04bbe9ef-a853-417c-a2b8-5328b62936e2")
    @AttributeName(name = "jansNodeName")
    private String nodeName;

    @JsonProperty("eventType")
    @JsonAlias("event_type")
    @Schema(description = "Type of event", example = "Decision")
    @AttributeName(name = "eventType")
    private String eventType;

    @JsonProperty("severityLevel")
    @JsonAlias("severity_level")
    @Schema(description = "Severity level", example = "warning", allowableValues = {"info", "warning", "error", "critical"})
    @AttributeName(name = "severityLevel")
    private String severityLevel;

    @JsonProperty("action")
    @Schema(description = "Action performed", example = "Jans::Action::\"POST\"")
    @AttributeName(name = "actionName")
    private String action;

    @JsonProperty("decisionResult")
    @JsonAlias("decision_result")
    @Schema(description = "Decision result", example = "ALLOW", allowableValues = {"ALLOW", "DENY"})
    @AttributeName(name = "decisionResult")
    private String decisionResult;

    @JsonProperty("requestedResource")
    @JsonAlias("requested_resource")
    @Schema(description = "Requested resource identifier", example = "Jans::HTTP_Request::\"lock_audit_log_write\"")
    @AttributeName(name = "requestedResource")
    private String requestedResource;

    @JsonProperty("principalId")
    @JsonAlias("principal_id")
    @Schema(description = "Principal (user) identifier", example = "ACC0001")
    @AttributeName(name = "principalId")
    private String principalId;

    @JsonProperty("clientId")
    @JsonAlias("client_id")
    @Schema(description = "Client identifier", example = "CLI001")
    @AttributeName(name = "clientId")
    private String clientId;

    @JsonProperty("jti")
    @Schema(description = "JWT ID - unique identifier for the token", example = "550e8400-e29b-41d4-a716-446655440000")
    @AttributeName(name = "jti")
    private String jti;

    @JsonProperty("contextInformation")
    @JsonAlias("context_information")
    @JsonDeserialize(using = ContextInformationDeserializer.class)
    @Schema(description = "Additional context information as key-value pairs")
    @JsonObject
    @AttributeName(name = "contextInformation")
    private Map<String, String> contextInformation;

    @JsonProperty("requestId")
    @Schema(description = "Cedarling request_id of the decision; contextInformation request_id wins when present", example = "0198a5e2-7c1f-7b3e-9d2a-3f4e5a6b7c8d")
    @AttributeName(name = "jansRequestId")
    private String requestId;

    @JsonProperty("policyStoreId")
    @Schema(description = "Policy store id used for the decision; contextInformation policystore_id wins when present", example = "a1bf63d5a5e4a0b7d0d5c9e3")
    @AttributeName(name = "jansPolicyStoreId")
    private String policyStoreId;

    @JsonProperty("policyStoreVersion")
    @Schema(description = "Policy store version used for the decision; contextInformation policystore_version wins when present", example = "1.2.3")
    @AttributeName(name = "jansPolicyStoreVersion")
    private String policyStoreVersion;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getSeverityLevel() {
		return severityLevel;
	}

	public void setSeverityLevel(String severityLevel) {
		this.severityLevel = severityLevel;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDecisionResult() {
		return decisionResult;
	}

	public void setDecisionResult(String decisionResult) {
		this.decisionResult = decisionResult;
	}

	public String getRequestedResource() {
		return requestedResource;
	}

	public void setRequestedResource(String requestedResource) {
		this.requestedResource = requestedResource;
	}

	public String getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(String principalId) {
		this.principalId = principalId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

	public Map<String, String> getContextInformation() {
		return contextInformation;
	}

	public void setContextInformation(Map<String, String> contextInformation) {
		this.contextInformation = contextInformation;
	}

	/**
	 * @return {@code request_id} from {@code contextInformation} when present and at most
	 *         {@value #MAX_REQUEST_ID_LENGTH} characters, else the explicit value under the same
	 *         limit, else {@code null}. Persistence and JSON output read this getter.
	 */
	public String getRequestId() {
		return indexed(CONTEXT_REQUEST_ID, requestId, MAX_REQUEST_ID_LENGTH);
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @return {@code policystore_id} from {@code contextInformation}, else the explicit value;
	 *         each only when at most {@value #MAX_POLICY_STORE_FIELD_LENGTH} characters
	 */
	public String getPolicyStoreId() {
		return indexed(CONTEXT_POLICY_STORE_ID, policyStoreId, MAX_POLICY_STORE_FIELD_LENGTH);
	}

	public void setPolicyStoreId(String policyStoreId) {
		this.policyStoreId = policyStoreId;
	}

	/**
	 * @return {@code policystore_version} from {@code contextInformation}, else the explicit
	 *         value; each only when at most {@value #MAX_POLICY_STORE_FIELD_LENGTH} characters
	 */
	public String getPolicyStoreVersion() {
		return indexed(CONTEXT_POLICY_STORE_VERSION, policyStoreVersion, MAX_POLICY_STORE_FIELD_LENGTH);
	}

	public void setPolicyStoreVersion(String policyStoreVersion) {
		this.policyStoreVersion = policyStoreVersion;
	}

	private String indexed(String contextKey, String explicit, int maxLength) {
		String value = contextInformation == null ? null : indexable(contextInformation.get(contextKey), maxLength);
		return value != null ? value : indexable(explicit, maxLength);
	}

	private static String indexable(String value, int maxLength) {
		return value != null && !value.isEmpty() && value.length() <= maxLength ? value : null;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(creationDate, logEntry.creationDate) &&
                Objects.equals(eventTime, logEntry.eventTime) &&
                Objects.equals(service, logEntry.service) &&
                Objects.equals(nodeName, logEntry.nodeName) &&
                Objects.equals(eventType, logEntry.eventType) &&
                Objects.equals(severityLevel, logEntry.severityLevel) &&
                Objects.equals(action, logEntry.action) &&
                Objects.equals(decisionResult, logEntry.decisionResult) &&
                Objects.equals(requestedResource, logEntry.requestedResource) &&
                Objects.equals(principalId, logEntry.principalId) &&
                Objects.equals(clientId, logEntry.clientId) &&
                Objects.equals(jti, logEntry.jti) &&
                Objects.equals(contextInformation, logEntry.contextInformation) &&
                Objects.equals(getRequestId(), logEntry.getRequestId()) &&
                Objects.equals(getPolicyStoreId(), logEntry.getPolicyStoreId()) &&
                Objects.equals(getPolicyStoreVersion(), logEntry.getPolicyStoreVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, eventTime, service, nodeName, eventType, 
                severityLevel, action, decisionResult, requestedResource, 
                principalId, clientId, jti, contextInformation, getRequestId(), getPolicyStoreId(), getPolicyStoreVersion());
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "creationDate='" + creationDate + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", service='" + service + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", eventType='" + eventType + '\'' +
                ", severityLevel='" + severityLevel + '\'' +
                ", action='" + action + '\'' +
                ", decisionResult='" + decisionResult + '\'' +
                ", requestedResource='" + requestedResource + '\'' +
                ", principalId='" + principalId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", jti='" + jti + '\'' +
                ", contextInformation=" + contextInformation +
                ", requestId='" + getRequestId() + '\'' +
                ", policyStoreId='" + getPolicyStoreId() + '\'' +
                ", policyStoreVersion='" + getPolicyStoreVersion() + '\'' +
                '}';
    }
}
