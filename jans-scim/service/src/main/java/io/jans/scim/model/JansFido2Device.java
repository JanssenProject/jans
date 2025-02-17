/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model;

import io.jans.scim.model.fido2.Fido2DeviceData;
import io.jans.scim.model.fido2.Fido2Data;
import io.jans.scim.model.fido2.Fido2Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
@DataEntry
@ObjectClass(value = "jansFido2RegistrationEntry")
public class JansFido2Device extends Fido2Entry implements Serializable {

    @JsonObject
    @AttributeName(name = "jansRegistrationData")
    private Fido2Data registrationData;

    @AttributeName(name = "jansStatus")
    private String registrationStatus;

    @AttributeName(name = "displayName")
    private String displayName;

    @JsonObject
	@AttributeName(name = "jansDeviceData")
	private Fido2DeviceData deviceData;

    public JansFido2Device() {
    }

    public JansFido2Device(String dn, String id, Date creationDate, String sessionId, String userInum,
                           Fido2Data registrationData, String challenge) {
        super(dn, id, creationDate, sessionId, userInum, challenge);
        this.registrationData = registrationData;
    }

    public Fido2Data getRegistrationData() {
        return registrationData;
    }

    public void setRegistrationData(Fido2Data registrationData) {
        this.registrationData = registrationData;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public Fido2DeviceData getDeviceData() {
		return deviceData;
	}

	public void setDeviceData(Fido2DeviceData deviceData) {
		this.deviceData = deviceData;
	}

}
