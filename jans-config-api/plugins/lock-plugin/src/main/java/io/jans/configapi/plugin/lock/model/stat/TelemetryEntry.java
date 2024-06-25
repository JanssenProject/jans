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

    @DN
    private String dn;

    @AttributeName(name = "jansId")
    private String id;

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

    @JsonProperty("memoryusage")
    private String memUsageMb;

    @AttributeName(name = "requestCounter")
    private long evaluationRequestsCount;

    @JsonProperty("policyRequestData")
    private Map<String, Map<String, Long>> policyRequestData;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMemUsageMb() {
        return memUsageMb;
    }

    public void setMemUsageMb(String memUsageMb) {
        this.memUsageMb = memUsageMb;
    }

    public long getEvaluationRequestsCount() {
        return evaluationRequestsCount;
    }

    public void setEvaluationRequestsCount(long evaluationRequestsCount) {
        this.evaluationRequestsCount = evaluationRequestsCount;
    }

    public Map<String, Map<String, Long>> getPolicyRequestData() {
        return policyRequestData;
    }

    public void setPolicyRequestData(Map<String, Map<String, Long>> policyRequestData) {
        this.policyRequestData = policyRequestData;
    }

    @Override
    public String toString() {
        return "TelemetryEntry [dn=" + dn + ", id=" + id + ", lastPolicyLoadTime=" + lastPolicyLoadTime
                + ", lastPolicyLoadSize=" + lastPolicyLoadSize + ", status=" + status + ", policySuccessLoadCounter="
                + policySuccessLoadCounter + ", policyFailedLoadCounter=" + policyFailedLoadCounter
                + ", lastPolicyEvaluationTimeNs=" + lastPolicyEvaluationTimeNs + ", avgPolicyEvaluationTimeNs="
                + avgPolicyEvaluationTimeNs + ", memUsageMb=" + memUsageMb + ", evaluationRequestsCount="
                + evaluationRequestsCount + ", policyRequestData=" + policyRequestData + "]";
    }

}
