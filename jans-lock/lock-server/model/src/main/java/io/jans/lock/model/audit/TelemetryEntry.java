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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Telemetry Entry for audit logging. Represents telemetry and performance
 * metrics of a service.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Telemetry audit entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataEntry(sortByName = "creationDate")
@ObjectClass(value = "jansTelemetryEntry")
public class TelemetryEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = 3237727784024903177L;

	@JsonProperty("inum")
	@AttributeName(name = "inum", ignoreDuringUpdate = true)
	private String inum;

	@JsonProperty("creationDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
	@Schema(description = "Creation date of the entry", example = "2026-07-24T15:33:06.872Z")
	@AttributeName(name = "creationDate")
	private Date creationDate;

	@JsonProperty("service")
	@Schema(description = "Service name", example = "Lock Server")
	@AttributeName(name = "jansService")
	private String service;

	@JsonProperty("nodeName")
	@Schema(description = "Node name or identifier", example = "04bbe9ef-a853-417c-a2b8-5328b62936e2")
	@AttributeName(name = "jansNodeName")
	private String nodeName;

	@JsonProperty("status")
	@Schema(description = "Service status", example = "running")
	@AttributeName(name = "jansStatus")
	private String status;

	@JsonProperty("policyStats")
	@Schema(description = "Per-policy evaluation counters as key-value pairs")
	@JsonObject
	@AttributeName(name = "policyStats")
	private Map<String, Long> policyStats;

	@JsonProperty("errorCounters")
	@Schema(description = "Error counters broken down by error type")
	@JsonObject
	@AttributeName(name = "errorCounters")
	private Map<String, Long> errorCounters;

	@JsonProperty("operationalStats")
	@Schema(description = "Operational statistics (requests, decisions, eval times) as key-value pairs")
	@JsonObject
	@AttributeName(name = "operationalStats")
	private Map<String, Long> operationalStats;

	@JsonProperty("intervalSecs")
	@Schema(description = "Telemetry collection interval in seconds", example = "5")
	@AttributeName(name = "intervalSecs")
	private Long intervalSecs;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Map<String, Long> getPolicyStats() {
		return policyStats;
	}

	public void setPolicyStats(Map<String, Long> policyStats) {
		this.policyStats = policyStats;
	}

	public Map<String, Long> getErrorCounters() {
		return errorCounters;
	}

	public void setErrorCounters(Map<String, Long> errorCounters) {
		this.errorCounters = errorCounters;
	}

	public Map<String, Long> getOperationalStats() {
		return operationalStats;
	}

	public void setOperationalStats(Map<String, Long> operationalStats) {
		this.operationalStats = operationalStats;
	}

	public Long getIntervalSecs() {
		return intervalSecs;
	}

	public void setIntervalSecs(Long intervalSecs) {
		this.intervalSecs = intervalSecs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TelemetryEntry that = (TelemetryEntry) o;
		return Objects.equals(creationDate, that.creationDate)
				&& Objects.equals(service, that.service) && Objects.equals(nodeName, that.nodeName)
				&& Objects.equals(status, that.status) && Objects.equals(intervalSecs, that.intervalSecs)
				&& Objects.equals(policyStats, that.policyStats)
				&& Objects.equals(errorCounters, that.errorCounters)
				&& Objects.equals(operationalStats, that.operationalStats);
	}

	@Override
	public int hashCode() {
		return Objects.hash(creationDate, service, nodeName, status, intervalSecs,
				policyStats, errorCounters, operationalStats);
	}

	@Override
	public String toString() {
		return "TelemetryEntry{" + "creationDate='" + creationDate + '\''
				+ ", service='" + service + '\'' + ", nodeName='" + nodeName + '\'' + ", status='" + status + '\''
				+ ", intervalSecs=" + intervalSecs + ", policyStats=" + policyStats
				+ ", errorCounters=" + errorCounters + ", operationalStats=" + operationalStats + '}';
	}
}
