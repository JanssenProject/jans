package org.gluu.oxtrust.model;

import java.io.Serializable;
import java.util.Date;

import org.gluu.oxtrust.model.fido2.Fido2Data;
import org.gluu.oxtrust.model.fido2.Fido2Entry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;

@SuppressWarnings("serial")
@DataEntry
@ObjectClass(value = "oxFido2RegistrationEntry")
public class GluuFido2Device extends Fido2Entry implements Serializable {

    @JsonObject
    @AttributeName(name = "oxRegistrationData")
    private Fido2Data registrationData;

    @AttributeName(name = "oxStatus")
    private String registrationStatus;

    @AttributeName(name = "displayName")
    private String displayName;

    public GluuFido2Device() {
    }

    public GluuFido2Device(String dn, String id, Date creationDate, String sessionId, String userInum,
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

}
