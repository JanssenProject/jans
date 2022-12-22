/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import io.jans.fido2.model.u2f.DeviceRegistrationStatus;
import io.jans.fido2.model.u2f.exception.BadInputException;
import io.jans.fido2.model.u2f.protocol.DeviceData;
import io.jans.as.model.util.Base64Util;
import io.jans.fido2.model.u2f.exception.InvalidDeviceCounterException;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * U2F Device registration
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@DataEntry(sortBy = "creationDate")
@ObjectClass(value = "jansDeviceRegistration")
public class DeviceRegistration extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -4542931562244920585L;
    @AttributeName(name = "personInum")
    protected String userInum;
    @AttributeName(ignoreDuringUpdate = true, name = "jansId")
    private String id;
    @AttributeName
    private String displayName;
    @AttributeName
    private String description;
    @AttributeName(name = "jansNickName")
    private String nickname;
    @JsonObject
    @AttributeName(name = "jansDeviceRegistrationConf")
    private DeviceRegistrationConfiguration deviceRegistrationConfiguration;

    @JsonObject
    @AttributeName(name = "jansDeviceNotificationConf")
    private String deviceNotificationConf;

    @AttributeName(name = "jansCounter")
    private long counter;

    @AttributeName(name = "jansStatus")
    private io.jans.fido2.model.u2f.DeviceRegistrationStatus status;

    @AttributeName(name = "jansApp")
    private String application;

    @AttributeName(name = "jansDeviceKeyHandle")
    private String keyHandle;

    @AttributeName(name = "jansDeviceHashCode")
    private Integer keyHandleHashCode;

    @JsonObject
    @AttributeName(name = "jansDeviceData")
    private DeviceData deviceData;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @AttributeName(name = "jansLastAccessTime")
    private Date lastAccessTime;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "del")
    private boolean deletable = true;

    @Expiration
    private Integer ttl;

    public DeviceRegistration() {
    }

    public DeviceRegistration(String userInum, String keyHandle, String publicKey, String attestationCert, long counter, DeviceRegistrationStatus status,
                              String application, Integer keyHandleHashCode, Date creationDate) {
        this.deviceRegistrationConfiguration = new DeviceRegistrationConfiguration(publicKey, attestationCert);
        this.counter = counter;
        this.status = status;
        this.application = application;
        this.userInum = userInum;
        this.keyHandle = keyHandle;
        this.keyHandleHashCode = keyHandleHashCode;
        this.creationDate = creationDate;
    }

    public DeviceRegistration(String userInum, String keyHandle, String publicKey, X509Certificate attestationCert, long counter) throws BadInputException {
        this.userInum = userInum;
        this.keyHandle = keyHandle;
        try {
            String attestationCertDecoded = Base64Util.base64urlencode(attestationCert.getEncoded());
            this.deviceRegistrationConfiguration = new DeviceRegistrationConfiguration(publicKey, attestationCertDecoded);
        } catch (CertificateEncodingException e) {
            throw new BadInputException("Malformed attestation certificate", e);
        }

        this.counter = counter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserInum() {
        return userInum;
    }

    public void setUserInum(String userInum) {
        this.userInum = userInum;
    }

    public DeviceRegistrationConfiguration getDeviceRegistrationConfiguration() {
        return deviceRegistrationConfiguration;
    }

    public void setDeviceRegistrationConfiguration(DeviceRegistrationConfiguration deviceRegistrationConfiguration) {
        this.deviceRegistrationConfiguration = deviceRegistrationConfiguration;
    }

    public String getDeviceNotificationConf() {
        return deviceNotificationConf;
    }

    public void setDeviceNotificationConf(String deviceNotificationConf) {
        this.deviceNotificationConf = deviceNotificationConf;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public DeviceRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceRegistrationStatus status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getKeyHandle() {
        return keyHandle;
    }

    public void setKeyHandle(String keyHandle) {
        this.keyHandle = keyHandle;
    }

    public Integer getKeyHandleHashCode() {
        return keyHandleHashCode;
    }

    public void setKeyHandleHashCode(Integer keyHandleHashCode) {
        this.keyHandleHashCode = keyHandleHashCode;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void clearExpiration() {
        this.expirationDate = null;
        this.deletable = false;
        this.ttl = 0;
    }

    public void setExpiration() {
        if (creationDate != null) {
            final int expiration = 90;
            Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.setTime(creationDate);
            calendar.add(Calendar.SECOND, expiration);
            this.expirationDate = calendar.getTime();
            this.deletable = true;
            this.ttl = expiration;
        }
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(DeviceData deviceData) {
        this.deviceData = deviceData;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public boolean isCompromised() {
        return DeviceRegistrationStatus.COMPROMISED == this.status;
    }

    public void markCompromised() {
        this.status = DeviceRegistrationStatus.COMPROMISED;
    }

    public void checkAndUpdateCounter(long clientCounter) throws InvalidDeviceCounterException {
        if (clientCounter == Integer.MAX_VALUE) {
            // TODO: Remove in 6.0.It's enough period to migrate broken iOS counter
            // Handle special case when counter value is max positive integer value
            counter = -1;
        } else {
            if (clientCounter <= counter) {
                markCompromised();
                throw new InvalidDeviceCounterException(this);
            }
            counter = clientCounter;
        }
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public String toString() {
        return "DeviceRegistration{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", nickname='" + nickname + '\'' +
                ", deviceRegistrationConfiguration=" + deviceRegistrationConfiguration +
                ", deviceNotificationConf='" + deviceNotificationConf + '\'' +
                ", counter=" + counter +
                ", status=" + status +
                ", application='" + application + '\'' +
                ", keyHandle='" + keyHandle + '\'' +
                ", keyHandleHashCode=" + keyHandleHashCode +
                ", deviceData=" + deviceData +
                ", creationDate=" + creationDate +
                ", lastAccessTime=" + lastAccessTime +
                ", expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                "} " + super.toString();
    }
}
