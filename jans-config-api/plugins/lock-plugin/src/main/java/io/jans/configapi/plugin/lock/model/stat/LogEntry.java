package io.jans.configapi.plugin.lock.model.stat;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

@DataEntry
@ObjectClass(value = "jansLogEntry")
public class LogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @DN
    private String dn;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansLastUpd")
    private Date lastUpdate;

    @AttributeName(name = "eventTime")
    private Date eventTime;

    @AttributeName(name = "eventType")
    private String eventType;

    @AttributeName(name = "severetyLevel")
    private String severetyLevel;

    @AttributeName(name = "policyResult")
    private String policyResult;

    @AttributeName(name = "userAccountId")
    private String userAccountId;

    @AttributeName(name = "clientId")
    private String clientId;

    @AttributeName(name = "sourceInformation")
    private String sourceInformation;

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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
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

    public String getPolicyResult() {
        return policyResult;
    }

    public void setPolicyResult(String policyResult) {
        this.policyResult = policyResult;
    }

    public String getUserAccountId() {
        return userAccountId;
    }

    public void setUserAccountId(String userAccountId) {
        this.userAccountId = userAccountId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSourceInformation() {
        return sourceInformation;
    }

    public void setSourceInformation(String sourceInformation) {
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String toString() {
        return "LogEntry [dn=" + dn + ", inum=" + inum + ", lastUpdate=" + lastUpdate + ", eventTime=" + eventTime
                + ", eventType=" + eventType + ", severetyLevel=" + severetyLevel + ", policyResult=" + policyResult
                + ", userAccountId=" + userAccountId + ", clientId=" + clientId + ", sourceInformation="
                + sourceInformation + "]";
    }

}
