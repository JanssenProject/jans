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

    @AttributeName(name = "jansNodeName")
    private String nodeName;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "jansDownloadSize")
    private int lastPolicyLoadSize;

    @AttributeName(name = "jansSuccessLoadCounter")
    private long policySuccessLoadCounter;

    @AttributeName(name = "jansFailedLoadCounter")
    private long policyFailedLoadCounter;

    @AttributeName(name = "evaluationTimeNs")
    private int lastPolicyEvaluationTimeNs;

    @AttributeName(name = "averageTimeNs")
    private int avgPolicyEvaluationTimeNs;

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

	public int getLastPolicyLoadSize() {
		return lastPolicyLoadSize;
	}

	public void setLastPolicyLoadSize(int lastPolicyLoadSize) {
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

	public int getLastPolicyEvaluationTimeNs() {
		return lastPolicyEvaluationTimeNs;
	}

	public void setLastPolicyEvaluationTimeNs(int lastPolicyEvaluationTimeNs) {
		this.lastPolicyEvaluationTimeNs = lastPolicyEvaluationTimeNs;
	}

	public int getAvgPolicyEvaluationTimeNs() {
		return avgPolicyEvaluationTimeNs;
	}

	public void setAvgPolicyEvaluationTimeNs(int avgPolicyEvaluationTimeNs) {
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
				+ ", service=" + service + ", nodeName=" + nodeName + ", status=" + status + ", lastPolicyLoadSize="
				+ lastPolicyLoadSize + ", policySuccessLoadCounter=" + policySuccessLoadCounter
				+ ", policyFailedLoadCounter=" + policyFailedLoadCounter + ", lastPolicyEvaluationTimeNs="
				+ lastPolicyEvaluationTimeNs + ", avgPolicyEvaluationTimeNs=" + avgPolicyEvaluationTimeNs
				+ ", memoryUsage=" + memoryUsage + ", evaluationRequestsCount=" + evaluationRequestsCount
				+ ", policyStats=" + policyStats + ", toString()=" + super.toString() + "]";
	}
    
}
