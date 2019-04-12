/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.persist.model.base;

import java.io.Serializable;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * Model for simple branch
 *
 * @author Yuriy Movchan Date: 11.01.2010
 */
@DataEntry
@ObjectClass(values = { "top", "organizationalUnit"})
public class SimpleBranch extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -1311006812730222719L;

    @AttributeName(name = "ou")
    private String organizationalUnitName;

    public SimpleBranch() {
    }

    public SimpleBranch(String dn) {
        setDn(dn);
    }

    public SimpleBranch(String dn, String organizationalUnitName) {
        this(dn);
        this.organizationalUnitName = organizationalUnitName;
    }

    public String getOrganizationalUnitName() {
        return organizationalUnitName;
    }

    public void setOrganizationalUnitName(String organizationalUnitName) {
        this.organizationalUnitName = organizationalUnitName;
    }

    @Override
    public String toString() {
        return String.format("SimpleBranch [organizationalUnitName=%s, toString()=%s]", organizationalUnitName, super.toString());
    }

}
