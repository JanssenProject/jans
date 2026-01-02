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

/**
 * Telemetry Entry for audit logging. Represents telemetry and performance
 * metrics of a service.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Telemetry audit entry")
@JsonIgnoreProperties(ignoreUnknown = true)
@DataEntry(sortByName = "eventTime")
@ObjectClass(value = "jansTelemetryEntry")
public class TelemetryEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = 3237727784024903177L;

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

	@JsonProperty("status")
	@Schema(description = "Service status", example = "ok", allowableValues = { "ok", "warning", "error" })
	@AttributeName(name = "jansStatus")
	private String status;

	@JsonProperty("lastPolicyLoadSize")
	@Schema(description = "Size of the last policy load in bytes", example = "1024")
	@AttributeName(name = "jansDownloadSize")
	private Long lastPolicyLoadSize;

	@JsonProperty("policySuccessLoadCounter")
	@Schema(description = "Number of successful policy loads", example = "100")
	@AttributeName(name = "jansSuccessLoadCounter")
	private Long policySuccessLoadCounter;

	@JsonProperty("policyFailedLoadCounter")
	@Schema(description = "Number of failed policy loads", example = "3")
	@AttributeName(name = "jansFailedLoadCounter")
	private Long policyFailedLoadCounter;

	@JsonProperty("lastPolicyEvaluationTimeNs")
	@Schema(description = "Last policy evaluation time in nanoseconds", example = "100")
	@AttributeName(name = "evaluationTimeNs")
	private Long lastPolicyEvaluationTimeNs;

	@JsonProperty("avgPolicyEvaluationTimeNs")
	@Schema(description = "Average policy evaluation time in nanoseconds", example = "75")
	@AttributeName(name = "averageTimeNs")
	private Long avgPolicyEvaluationTimeNs;

	@JsonProperty("memoryUsage")
	@Schema(description = "Memory usage in bytes", example = "2097152")
	@AttributeName(name = "memoryUsage")
	private Long memoryUsage;

	@JsonProperty("evaluationRequestsCount")
	@Schema(description = "Total number of evaluation requests", example = "100")
	@AttributeName(name = "requestCounter")
	private Long evaluationRequestsCount;

	@JsonProperty("policyStats")
	@Schema(description = "Additional policy statistics as key-value pairs")
	@JsonObject
	@AttributeName(name = "policyStats")
	private Map<String, Long> policyStats;

	public TelemetryEntry() {
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getLastPolicyLoadSize() {
		return lastPolicyLoadSize;
	}

	public void setLastPolicyLoadSize(Long lastPolicyLoadSize) {
		this.lastPolicyLoadSize = lastPolicyLoadSize;
	}

	public Long getPolicySuccessLoadCounter() {
		return policySuccessLoadCounter;
	}

	public void setPolicySuccessLoadCounter(Long policySuccessLoadCounter) {
		this.policySuccessLoadCounter = policySuccessLoadCounter;
	}

	public Long getPolicyFailedLoadCounter() {
		return policyFailedLoadCounter;
	}

	public void setPolicyFailedLoadCounter(Long policyFailedLoadCounter) {
		this.policyFailedLoadCounter = policyFailedLoadCounter;
	}

	public Long getLastPolicyEvaluationTimeNs() {
		return lastPolicyEvaluationTimeNs;
	}

	public void setLastPolicyEvaluationTimeNs(Long lastPolicyEvaluationTimeNs) {
		this.lastPolicyEvaluationTimeNs = lastPolicyEvaluationTimeNs;
	}

	public Long getAvgPolicyEvaluationTimeNs() {
		return avgPolicyEvaluationTimeNs;
	}

	public void setAvgPolicyEvaluationTimeNs(Long avgPolicyEvaluationTimeNs) {
		this.avgPolicyEvaluationTimeNs = avgPolicyEvaluationTimeNs;
	}

	public Long getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(Long memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public Long getEvaluationRequestsCount() {
		return evaluationRequestsCount;
	}

	public void setEvaluationRequestsCount(Long evaluationRequestsCount) {
		this.evaluationRequestsCount = evaluationRequestsCount;
	}

	public Map<String, Long> getPolicyStats() {
		return policyStats;
	}

	public void setPolicyStats(Map<String, Long> policyStats) {
		this.policyStats = policyStats;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TelemetryEntry that = (TelemetryEntry) o;
		return Objects.equals(creationDate, that.creationDate) && Objects.equals(eventTime, that.eventTime)
				&& Objects.equals(service, that.service) && Objects.equals(nodeName, that.nodeName)
				&& Objects.equals(status, that.status) && Objects.equals(lastPolicyLoadSize, that.lastPolicyLoadSize)
				&& Objects.equals(policySuccessLoadCounter, that.policySuccessLoadCounter)
				&& Objects.equals(policyFailedLoadCounter, that.policyFailedLoadCounter)
				&& Objects.equals(lastPolicyEvaluationTimeNs, that.lastPolicyEvaluationTimeNs)
				&& Objects.equals(avgPolicyEvaluationTimeNs, that.avgPolicyEvaluationTimeNs)
				&& Objects.equals(memoryUsage, that.memoryUsage)
				&& Objects.equals(evaluationRequestsCount, that.evaluationRequestsCount)
				&& Objects.equals(policyStats, that.policyStats);
	}

	@Override
	public int hashCode() {
		return Objects.hash(creationDate, eventTime, service, nodeName, status, lastPolicyLoadSize,
				policySuccessLoadCounter, policyFailedLoadCounter, lastPolicyEvaluationTimeNs,
				avgPolicyEvaluationTimeNs, memoryUsage, evaluationRequestsCount, policyStats);
	}

	@Override
	public String toString() {
		return "TelemetryEntry{" + "creationDate='" + creationDate + '\'' + ", eventTime='" + eventTime + '\''
				+ ", service='" + service + '\'' + ", nodeName='" + nodeName + '\'' + ", status='" + status + '\''
				+ ", lastPolicyLoadSize=" + lastPolicyLoadSize + ", policySuccessLoadCounter="
				+ policySuccessLoadCounter + ", policyFailedLoadCounter=" + policyFailedLoadCounter
				+ ", lastPolicyEvaluationTimeNs=" + lastPolicyEvaluationTimeNs + ", avgPolicyEvaluationTimeNs="
				+ avgPolicyEvaluationTimeNs + ", memoryUsage=" + memoryUsage + ", evaluationRequestsCount="
				+ evaluationRequestsCount + ", policyStats=" + policyStats + '}';
	}
}
