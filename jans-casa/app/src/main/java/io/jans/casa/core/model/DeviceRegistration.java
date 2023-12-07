package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.util.Date;

@DataEntry
@ObjectClass("jansDeviceRegistration")
public class DeviceRegistration extends Entry {

    @AttributeName
    private String jansId;

    @AttributeName
    private String jansApp;

    @AttributeName
    private Long jansCounter;

    @AttributeName
    private String displayName;

    @AttributeName
    private Date creationDate;

    @AttributeName(name = "jansLastAccessTime")
    private Date lastAccessTime;

    @AttributeName
    private String jansStatus;

    @AttributeName(name = "jansDeviceData")
    private String deviceData;

    public String getJansId() {
        return jansId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public Long getJansCounter() {
        return jansCounter;
    }

    public String getJansApp() {
        return jansApp;
    }

    public String getJansStatus() {
        return jansStatus;
    }

    public String getDeviceData() {
        return deviceData;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setJansId(String jansId) {
        this.jansId = jansId;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setJansApp(String jansApp) {
        this.jansApp = jansApp;
    }

    public void setJansStatus(String jansStatus) {
        this.jansStatus = jansStatus;
    }

    public void setJansCounter(Long jansCounter) {
        this.jansCounter = jansCounter;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setDeviceData(String deviceData) {
        this.deviceData = deviceData;
    }

}
