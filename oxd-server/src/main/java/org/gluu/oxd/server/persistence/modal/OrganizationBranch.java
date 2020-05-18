package org.gluu.oxd.server.persistence.modal;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;
import org.gluu.persist.model.base.BaseEntry;

import java.io.Serializable;

@DataEntry
@ObjectClass("organization")
public class OrganizationBranch extends BaseEntry implements Serializable {
    private static final long serialVersionUID = -1311006812730222719L;

    @AttributeName(
            name = "o"
    )
    private String organizationName;

    public OrganizationBranch() {
    }

    public OrganizationBranch(String dn) {
        this.setDn(dn);
    }

    public OrganizationBranch(String dn, String organizationName) {
        this(dn);
        this.organizationName = organizationName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String toString() {
        return String.format("OrganizationBranch [organizationName=%s, toString()=%s]", this.organizationName, super.toString());
    }
}
