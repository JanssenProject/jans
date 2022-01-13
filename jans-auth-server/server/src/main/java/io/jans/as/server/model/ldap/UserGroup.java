/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.ldap;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2012
 */

@DataEntry
public class UserGroup {
    @DN
    private String dn;
    @AttributeName(name = "displayName")
    private String displayName;
    @AttributeName(name = "member")
    private String[] member;
    @AttributeName(name = "jansGrpTyp")
    private String groupType;
    @AttributeName(name = "jansStatus")
    private String status;
    @AttributeName(name = "inum")
    private String inum;
    @AttributeName(name = "owner")
    private String owner;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String p_displayName) {
        displayName = p_displayName;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String p_groupType) {
        groupType = p_groupType;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String p_inum) {
        inum = p_inum;
    }

    public String[] getMember() {
        return member;
    }

    public void setMember(String[] p_member) {
        member = p_member;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String p_owner) {
        owner = p_owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String p_status) {
        status = p_status;
    }
}
