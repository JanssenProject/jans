package io.jans.configapi.plugin.lock.model.stat;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

@DataEntry
@ObjectClass(value = "jansTelemetryEntry")
public class TelemetryEntry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = 3237727784024903177L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "eventTime")
    private Date eventTime;

    @AttributeName(name = "jansService")
    private String service;

    @AttributeName(name = "jansNodeId")
    private String nodeId;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "jansDownloadSize")
    private Integer lastPolicyLoadSize;

    @AttributeName(name = "jansSuccessLoadCounter")
    private long policySuccessLoadCounter;

    @AttributeName(name = "jansFaiedlLoadCounter")
    private long policyFailedLoadCounter;

    @AttributeName(name = "evaluationTimeNs")
    private Date lastPolicyEvaluationTimeNs;

    @AttributeName(name = "averageTimeNs")
    private Date avgPolicyEvaluationTimeNs;

    @JsonProperty("memoryUsage")
    private String memoryUsage;

    @AttributeName(name = "requestCounter")
    private long evaluationRequestsCount;

    @JsonObject
    @AttributeName(name = "policyStats")
    private Map<String, String> policyStats;

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

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getLastPolicyLoadSize() {
		return lastPolicyLoadSize;
	}

	public void setLastPolicyLoadSize(Integer lastPolicyLoadSize) {
		this.lastPolicyLoadSize = lastPolicyLoadSize;
	}

	public long getPolicySuccessLoadCounter() {
		return policySuccessLoadCounter;
	}

	public void setPolicySuccessLoadCounter(long policySuccessLoadCounter) {
		this.policySuccessLoadCounter = policySuccessLoadCounter;
	}

	public long getPolicyFailedLoadCounter() {
		return policyFailedLoadCounter;
	}

	public void setPolicyFailedLoadCounter(long policyFailedLoadCounter) {
		this.policyFailedLoadCounter = policyFailedLoadCounter;
	}

	public Date getLastPolicyEvaluationTimeNs() {
		return lastPolicyEvaluationTimeNs;
	}

	public void setLastPolicyEvaluationTimeNs(Date lastPolicyEvaluationTimeNs) {
		this.lastPolicyEvaluationTimeNs = lastPolicyEvaluationTimeNs;
	}

	public Date getAvgPolicyEvaluationTimeNs() {
		return avgPolicyEvaluationTimeNs;
	}

	public void setAvgPolicyEvaluationTimeNs(Date avgPolicyEvaluationTimeNs) {
		this.avgPolicyEvaluationTimeNs = avgPolicyEvaluationTimeNs;
	}

	public String getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(String memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public long getEvaluationRequestsCount() {
		return evaluationRequestsCount;
	}

	public void setEvaluationRequestsCount(long evaluationRequestsCount) {
		this.evaluationRequestsCount = evaluationRequestsCount;
	}

	public Map<String, String> getPolicyStats() {
		return policyStats;
	}

	public void setPolicyStats(Map<String, String> policyStats) {
		this.policyStats = policyStats;
	}

	@Override
	public String toString() {
		return "TelemetryEntry [inum=" + inum + ", creationDate=" + creationDate + ", eventTime=" + eventTime
				+ ", service=" + service + ", nodeId=" + nodeId + ", status=" + status + ", lastPolicyLoadSize="
				+ lastPolicyLoadSize + ", policySuccessLoadCounter=" + policySuccessLoadCounter
				+ ", policyFailedLoadCounter=" + policyFailedLoadCounter + ", lastPolicyEvaluationTimeNs="
				+ lastPolicyEvaluationTimeNs + ", avgPolicyEvaluationTimeNs=" + avgPolicyEvaluationTimeNs
				+ ", memoryUsage=" + memoryUsage + ", evaluationRequestsCount=" + evaluationRequestsCount
				+ ", policyStats=" + policyStats + ", toString()=" + super.toString() + "]";
	}
    
}
