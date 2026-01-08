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

@Schema(description = "Log audit entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansLogEntry")
public class LogEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = -7086771090254438974L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @JsonProperty("creationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Schema(description = "Creation date of the entry", example = "2024-04-21T18:25:43-05:00")
    @AttributeName(name = "creationDate")
    private Date creationDate;

    @JsonProperty("eventTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @Schema(description = "Time when the event occurred", example = "2024-04-21T18:25:43-05:00")
    @AttributeName(name = "eventTime")
    private Date eventTime;

    @JsonProperty("service")
    @Schema(description = "Service name", example = "jans-auth")
    @AttributeName(name = "jansService")
    private String service;

    @JsonProperty("nodeName")
    @Schema(description = "Node name or identifier", example = "1")
    @AttributeName(name = "jansNodeName")
    private String nodeName;

    @JsonProperty("eventType")
    @Schema(description = "Type of event", example = "registration")
    @AttributeName(name = "eventType")
    private String eventType;

    @JsonProperty("severityLevel")
    @Schema(description = "Severity level", example = "warning", allowableValues = {"info", "warning", "error", "critical"})
    @AttributeName(name = "severetyLevel")
    private String severityLevel;

    @JsonProperty("action")
    @Schema(description = "Action performed", example = "ACTION_NAME_3")
    @AttributeName(name = "actionName")
    private String action;

    @JsonProperty("decisionResult")
    @Schema(description = "Decision result", example = "allow", allowableValues = {"allow", "deny"})
    @AttributeName(name = "decisionResult")
    private String decisionResult;

    @JsonProperty("requestedResource")
    @Schema(description = "Requested resource as JSON string", example = "{\"t1\":\"value1\",\"t2\":\"value2\"}")
    @AttributeName(name = "requestedResource")
    private String requestedResource;

    @JsonProperty("principalId")
    @Schema(description = "Principal (user) identifier", example = "ACC0001")
    @AttributeName(name = "principalId")
    private String principalId;

    @JsonProperty("clientId")
    @Schema(description = "Client identifier", example = "CLI001")
    @AttributeName(name = "clientId")
    private String clientId;

    @JsonProperty("jti")
    @Schema(description = "JWT ID - unique identifier for the token", example = "550e8400-e29b-41d4-a716-446655440000")
    @AttributeName(name = "jti")
    private String jti;

    @JsonProperty("contextInformation")
    @Schema(description = "Additional context information as key-value pairs")
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
                Objects.equals(contextInformation, logEntry.contextInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, eventTime, service, nodeName, eventType, 
                severityLevel, action, decisionResult, requestedResource, 
                principalId, clientId, jti, contextInformation);
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
                '}';
    }
}
