package org.gluu.oxtrust.model.scim2.fido;

import org.gluu.oxtrust.model.scim2.AttributeDefinition;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.annotations.*;

/**
 * Fido device SCIM resource. An object of this class is used to represent a device like an U2F security key or a
 * Super Gluu device. See the <i>oxDeviceRegistration</i> objectclass of your Gluu's LDAP.
 */
/*
 * Created by jgomer on 2017-10-09.
 * Based on former Val Pecaoco's org.gluu.oxtrust.model.scim2.fido.FidoDevice
 *
 * Notes: Other classes may depend on this one via reflection. Do not add members whose names are already at parent
 * org.gluu.oxtrust.model.scim2.BaseScimResource
 */
@Schema(id = "urn:ietf:params:scim:schemas:core:2.0:FidoDevice", name = "FidoDevice", description = "Fido Device")
public class FidoDeviceResource extends BaseScimResource {

    @Attribute(description = "Username of device owner",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE)
    @StoreReference(ref = "personInum")
    private String userId;

    @Attribute(description = "Date of enrollment",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            type = AttributeDefinition.Type.DATETIME)
    @StoreReference(ref = "creationDate")
    private String creationDate;

    @Attribute(description = "Application ID that enrolled the device",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE)
    @StoreReference(ref = "oxApplication")
    private String application;

    @Attribute(description = "A counter aimed at being used by the FIDO endpoint",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            type = AttributeDefinition.Type.INTEGER)
    @StoreReference(ref = "oxCounter")
    private String counter;

    @Attribute(description = "A Json representation of low-level attributes of this device",
            mutability = AttributeDefinition.Mutability.IMMUTABLE)
    @StoreReference(ref = "oxDeviceData")
    private String deviceData;

    @Attribute(description = "",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            type = AttributeDefinition.Type.INTEGER)
    @StoreReference(ref = "oxDeviceHashCode")
    private String deviceHashCode;

    @Attribute(description = "",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE)
    @StoreReference(ref = "oxDeviceKeyHandle")
    private String deviceKeyHandle;

    @Attribute(description = "",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE)
    @StoreReference(ref = "oxDeviceRegistrationConf")
    private String deviceRegistrationConf;

    @Attribute(description = "The most recent dateTime when this device was used for authentication",
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            type = AttributeDefinition.Type.DATETIME)
    @StoreReference(ref = "oxLastAccessTime")
    private String lastAccessTime;

    @Attribute(isRequired = true,
            canonicalValues = {"active", "compromised"})
    @StoreReference(ref = "oxStatus")
    private String status;

    @Attribute
    @StoreReference(ref = "displayName")
    private String displayName;

    @Attribute
    @StoreReference(ref = "description")
    private String description;

    @Attribute
    @StoreReference(ref = "oxNickName")
    private String nickname;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getCounter() {
        return counter;
    }

    public void setCounter(String counter) {
        this.counter = counter;
    }

    public String getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(String deviceData) {
        this.deviceData = deviceData;
    }

    public String getDeviceHashCode() {
        return deviceHashCode;
    }

    public void setDeviceHashCode(String deviceHashCode) {
        this.deviceHashCode = deviceHashCode;
    }

    public String getDeviceKeyHandle() {
        return deviceKeyHandle;
    }

    public void setDeviceKeyHandle(String deviceKeyHandle) {
        this.deviceKeyHandle = deviceKeyHandle;
    }

    public String getDeviceRegistrationConf() {
        return deviceRegistrationConf;
    }

    public void setDeviceRegistrationConf(String deviceRegistrationConf) {
        this.deviceRegistrationConf = deviceRegistrationConf;
    }

    public String getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

}
