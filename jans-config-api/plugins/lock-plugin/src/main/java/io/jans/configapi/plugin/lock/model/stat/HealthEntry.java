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
@ObjectClass(value = "jansHealthEntry")
public class HealthEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @DN
    private String dn;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansLastUpd")
    private Date lastPolicyLoadTime;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "cedarEngineStatus")
    private String cedarEngineStatus;

    @AttributeName(name = "cedarPolicyStatus")
    private String cedarPolicyStatus;

    @AttributeName(name = "tokenDataStatus")
    private String tokenDataStatus;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCedarEngineStatus() {
        return cedarEngineStatus;
    }

    public void setCedarEngineStatus(String cedarEngineStatus) {
        this.cedarEngineStatus = cedarEngineStatus;
    }

    public String getCedarPolicyStatus() {
        return cedarPolicyStatus;
    }

    public void setCedarPolicyStatus(String cedarPolicyStatus) {
        this.cedarPolicyStatus = cedarPolicyStatus;
    }

    public String getTokenDataStatus() {
        return tokenDataStatus;
    }

    public void setTokenDataStatus(String tokenDataStatus) {
        this.tokenDataStatus = tokenDataStatus;
    }

    @Override
    public String toString() {
        return "HealthEntry [dn=" + dn + ", inum=" + inum + ", lastPolicyLoadTime=" + lastPolicyLoadTime + ", status="
                + status + ", cedarEngineStatus=" + cedarEngineStatus + ", cedarPolicyStatus=" + cedarPolicyStatus
                + ", tokenDataStatus=" + tokenDataStatus + "]";
    }

}
