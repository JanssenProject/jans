package io.jans.lock.model.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansLogEntry")
public class LogEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -7086771090254438974L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "eventTime")
    private Date eventTime;

    @AttributeName(name = "jansService")
    private String service;

    @AttributeName(name = "jansNodeName")
    private String nodeName;

    @AttributeName(name = "eventType")
    private String eventType;

    @AttributeName(name = "severetyLevel")
    private String severetyLevel;

    @AttributeName(name = "actionName")
    private String action;

    @AttributeName(name = "decisionResult")
    private String decisionResult;

    @AttributeName(name = "requestedResource")
    private String requestedResource;

    @AttributeName(name = "principalId")
    private String princiaplId;

    @AttributeName(name = "clientId")
    private String clientId;

    @JsonObject
    @AttributeName(name = "contextInformation")
    private Map<String, String> contextInformation;

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

	public String getSeveretyLevel() {
		return severetyLevel;
	}

	public void setSeveretyLevel(String severetyLevel) {
		this.severetyLevel = severetyLevel;
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

	public String getPrinciaplId() {
		return princiaplId;
	}

	public void setPrinciaplId(String princiaplId) {
		this.princiaplId = princiaplId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Map<String, String> getContextInformation() {
		return contextInformation;
	}

	public void setContextInformation(Map<String, String> contextInformation) {
		this.contextInformation = contextInformation;
	}

	@Override
	public String toString() {
		return "LogEntry [inum=" + inum + ", creationDate=" + creationDate + ", eventTime=" + eventTime + ", service="
				+ service + ", nodeName=" + nodeName + ", eventType=" + eventType + ", severetyLevel=" + severetyLevel
				+ ", action=" + action + ", decisionResult=" + decisionResult + ", requestedResource="
				+ requestedResource + ", princiaplId=" + princiaplId + ", clientId=" + clientId
				+ ", contextInformation=" + contextInformation + ", toString()=" + super.toString() + "]";
	}

}
