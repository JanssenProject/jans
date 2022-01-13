/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.base;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * Model for simple branch
 *
 * @author Yuriy Movchan Date: 11.01.2010
 */
@DataEntry
@ObjectClass(value = "organizationalUnit")
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
