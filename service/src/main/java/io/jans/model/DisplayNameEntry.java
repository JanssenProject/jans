/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model;

import java.io.Serializable;

import io.jans.orm.model.base.Entry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;

/**
 * Entry with display Name attribute
 *
 * @author Yuriy Movchan Date: 08/11/2010
 */
@DataEntry(sortBy = { "displayName" })
public class DisplayNameEntry extends Entry implements Serializable {

    private static final long serialVersionUID = 2536007777903091939L;

    public DisplayNameEntry() {
    }

    public DisplayNameEntry(String dn, String inum, String displayName) {
        super(dn);
        this.inum = inum;
        this.displayName = displayName;
    }

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName
    private String displayName;

    @AttributeName
    private String uid;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return String.format("DisplayNameEntry [displayName=%s, inum=%s, toString()=%s]", displayName, inum, super.toString());
    }

    /**
     * @param uid
     *            the uid to set
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

}
