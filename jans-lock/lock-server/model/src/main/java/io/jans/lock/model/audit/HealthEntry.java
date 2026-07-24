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
 * Health Entry for audit logging. Represents health status information of a
 * service node.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Health audit entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansHealthEntry")
public class HealthEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -5080284664895065766L;

	@JsonProperty("inum")
	@AttributeName(name = "inum", ignoreDuringUpdate = true)
	private String inum;

	@JsonProperty("creationDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
	@Schema(description = "Creation date of the entry", example = "2026-07-24T15:33:06.875Z")
	@AttributeName(name = "creationDate")
	private Date creationDate;

	@JsonProperty("eventTime")
	@JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
	@Schema(description = "Time when the event occurred", example = "2026-07-24T15:33:06.875Z")
	@AttributeName(name = "eventTime")
	private Date eventTime;

	@JsonProperty("service")
	@Schema(description = "Service name", example = "Lock Server")
	@AttributeName(name = "jansService")
	private String service;

	@JsonProperty("nodeName")
	@Schema(description = "Node name or identifier", example = "04bbe9ef-a853-417c-a2b8-5328b62936e2")
	@AttributeName(name = "jansNodeName")
	private String nodeName;

	@JsonProperty("status")
	@Schema(description = "Health status", example = "running")
	@AttributeName(name = "jansStatus")
	private String status;

	// Details: policy_load, core, etc..
	@JsonProperty("engineStatus")
	@JsonObject
	@AttributeName(name = "engineStatus")
	private Map<String, String> engineStatus;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
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

	public Map<String, String> getEngineStatus() {
		return engineStatus;
	}

	public void setEngineStatus(Map<String, String> engineStatus) {
		this.engineStatus = engineStatus;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HealthEntry that = (HealthEntry) o;
		return Objects.equals(creationDate, that.creationDate) && Objects.equals(eventTime, that.eventTime)
				&& Objects.equals(service, that.service) && Objects.equals(nodeName, that.nodeName)
				&& Objects.equals(status, that.status) && Objects.equals(engineStatus, that.engineStatus);

	}

	@Override
	public int hashCode() {
		return Objects.hash(creationDate, eventTime, service, nodeName, status, engineStatus);
	}

	@Override
	public String toString() {
		return "HealthEntry{" + "creationDate='" + creationDate + '\'' + ", eventTime='" + eventTime + '\''
				+ ", service='" + service + '\'' + ", nodeName='" + nodeName + '\'' + ", status='" + status + '\''
				+ ", engineStatus='" + engineStatus + '\'' + '}';
	}

}
