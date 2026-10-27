/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire shape posted to the Lock Server's {@code /audit/log} and {@code /audit/log/bulk}
 * endpoints. Deliberately a plain POJO matching only the JSON fields of jans-lock's
 * {@code io.jans.lock.model.audit.LogEntry} rather than a dependency on that class: LogEntry is a
 * jans-orm-mapped persistence entity (it extends {@code BaseEntry} and carries LDAP attribute
 * annotations), which fido2 never persists locally and has no business depending on. This class is
 * fido2's own copy of the wire contract, not a client stub generated from jans-lock's model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LockAuditEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonProperty("eventTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
	private Date eventTime;
	@JsonProperty("service")
	private String service;
	@JsonProperty("nodeName")
	private String nodeName;
	@JsonProperty("eventType")
	private String eventType;
	@JsonProperty("severityLevel")
	private String severityLevel;
	@JsonProperty("action")
	private String action;
	@JsonProperty("decisionResult")
	private String decisionResult;
	@JsonProperty("requestedResource")
	private String requestedResource;
	@JsonProperty("principalId")
	private String principalId;
	@JsonProperty("clientId")
	private String clientId;
	@JsonProperty("jti")
	private String jti;
	@JsonProperty("contextInformation")
	private Map<String, String> contextInformation;

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
}
