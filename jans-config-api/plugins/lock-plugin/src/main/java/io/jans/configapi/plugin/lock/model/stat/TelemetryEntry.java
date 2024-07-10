package io.jans.configapi.plugin.lock.model.stat;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

@DataEntry
@ObjectClass(value = "jansTelemetryEntry")
public class TelemetryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @DN
    private String dn;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansLastUpd")
    private Date lastPolicyLoadTime;

    @AttributeName(name = "size")
    private Integer lastPolicyLoadSize;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "jansCounter")
    private long policySuccessLoadCounter;

    @AttributeName(name = "jansFailCounter")
    private long policyFailedLoadCounter;

    @AttributeName(name = "evaluationTimeNs")
    private long lastPolicyEvaluationTimeNs;

    @AttributeName(name = "averageTimeNs")
    private long avgPolicyEvaluationTimeNs;

    @JsonProperty("memoryUsage")
    private String memoryUsage;

    @AttributeName(name = "requestCounter")
    private long evaluationRequestsCount;

    @AttributeName(name = "policyStats")
    @JsonObject
    private Map<String, String> policyStats;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public Date getLastPolicyLoadTime() {
        return lastPolicyLoadTime;
    }

    public void setLastPolicyLoadTime(Date lastPolicyLoadTime) {
        this.lastPolicyLoadTime = lastPolicyLoadTime;
    }

    public Integer getLastPolicyLoadSize() {
        return lastPolicyLoadSize;
    }

    public void setLastPolicyLoadSize(Integer lastPolicyLoadSize) {
        this.lastPolicyLoadSize = lastPolicyLoadSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public long getLastPolicyEvaluationTimeNs() {
        return lastPolicyEvaluationTimeNs;
    }

    public void setLastPolicyEvaluationTimeNs(long lastPolicyEvaluationTimeNs) {
        this.lastPolicyEvaluationTimeNs = lastPolicyEvaluationTimeNs;
    }

    public long getAvgPolicyEvaluationTimeNs() {
        return avgPolicyEvaluationTimeNs;
    }

    public void setAvgPolicyEvaluationTimeNs(long avgPolicyEvaluationTimeNs) {
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
        return "TelemetryEntry [dn=" + dn + ", inum=" + inum + ", lastPolicyLoadTime=" + lastPolicyLoadTime
                + ", lastPolicyLoadSize=" + lastPolicyLoadSize + ", status=" + status + ", policySuccessLoadCounter="
                + policySuccessLoadCounter + ", policyFailedLoadCounter=" + policyFailedLoadCounter
                + ", lastPolicyEvaluationTimeNs=" + lastPolicyEvaluationTimeNs + ", avgPolicyEvaluationTimeNs="
                + avgPolicyEvaluationTimeNs + ", memoryUsage=" + memoryUsage + ", evaluationRequestsCount="
                + evaluationRequestsCount + ", policyStats=" + policyStats + "]";
    }
}
