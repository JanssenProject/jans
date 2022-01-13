package io.jans.ca.server.persistence.modal;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

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
