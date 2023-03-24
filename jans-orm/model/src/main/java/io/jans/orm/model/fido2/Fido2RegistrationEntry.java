/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import java.io.Serializable;
import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * Fido2 registration entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@ObjectClass(value = "jansFido2RegistrationEntry")
public class Fido2RegistrationEntry extends Fido2Entry implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @AttributeName(name = "displayName")
    private String displayName;

    @AttributeName(name = "jansPublicKeyId")
    protected String publicKeyId;

	@AttributeName(name = "jansPublicKeyIdHash")
	private Integer publicKeyIdHash;

    @JsonObject
    @AttributeName(name = "jansRegistrationData")
    private Fido2RegistrationData registrationData;

    @AttributeName(name = "jansCounter")
	private int counter;

    @JsonObject
    @AttributeName(name = "jansStatus")
    private Fido2RegistrationStatus registrationStatus;

    @AttributeName(name = "jansDeviceNotificationConf")
    private String deviceNotificationConf;

    @JsonObject
	@AttributeName(name = "jansDeviceData")
	private Fido2DeviceData deviceData;

    public Fido2RegistrationEntry() {
    }

    public Fido2RegistrationEntry(String dn, String id, Date creationDate, String userInum,
            Fido2RegistrationData registrationData, String challenge) {
        super(dn, id, creationDate, userInum, challenge);
        this.registrationData = registrationData;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public Fido2RegistrationData getRegistrationData() {
        return registrationData;
    }

    public void setRegistrationData(Fido2RegistrationData registrationData) {
        this.registrationData = registrationData;
    }

    public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public Fido2RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(Fido2RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

	public String getDeviceNotificationConf() {
        return deviceNotificationConf;
    }

    public void setDeviceNotificationConf(String deviceNotificationConf) {
        this.deviceNotificationConf = deviceNotificationConf;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public Integer getPublicKeyIdHash() {
		return publicKeyIdHash;
	}

	public void setPublicKeyIdHash(Integer publicKeyIdHash) {
		this.publicKeyIdHash = publicKeyIdHash;
	}

	public Fido2DeviceData getDeviceData() {
		return deviceData;
	}

	public void setDeviceData(Fido2DeviceData deviceData) {
		this.deviceData = deviceData;
	}

	@Override
	public String toString() {
		return "Fido2RegistrationEntry [displayName=" + displayName + ", publicKeyId=" + publicKeyId
				+ ", publicKeyIdHash=" + publicKeyIdHash + ", registrationData=" + registrationData + ", counter="
				+ counter + ", registrationStatus=" + registrationStatus + ", deviceNotificationConf="
				+ deviceNotificationConf + ", deviceData=" + deviceData + "]";
	}

}
