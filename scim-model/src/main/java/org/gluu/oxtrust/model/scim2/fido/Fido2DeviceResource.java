package org.gluu.oxtrust.model.scim2.fido;

import org.gluu.oxtrust.model.scim2.AttributeDefinition;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.annotations.Attribute;
import org.gluu.oxtrust.model.scim2.annotations.Schema;
import org.gluu.oxtrust.model.scim2.annotations.StoreReference;

/**
 * Fido 2.0 device SCIM resource. See the <i>oxFido2RegistrationEntry</i> objectclass of your Gluu's LDAP.
 */
@Schema(id = "urn:ietf:params:scim:schemas:core:2.0:Fido2Device", name = "Fido2Device", description = "Fido 2 Device")
public class Fido2DeviceResource extends BaseScimResource {

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

    @Attribute(description = "A counter aimed at being used by the FIDO endpoint",
            isRequired = true,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            type = AttributeDefinition.Type.INTEGER)
    @StoreReference(ref = "oxCounter")
    private int counter;

    @Attribute(isRequired = true,
            canonicalValues = {"registered", "pending"})
    @StoreReference(ref = "oxStatus")
    private String status;

    @Attribute
    @StoreReference(ref = "displayName")
    private String displayName;

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

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
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

}
