/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.BinaryData;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * Sample entry with binary attributes. jansData column should has binary type
 * (bytea/BLOB) and jansDataStr column should has string type to check base64
 * fallback conversion.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
@DataEntry
@ObjectClass(value = "jansBinEntry")
public class SimpleBinaryEntry implements Serializable {

    private static final long serialVersionUID = -1634191420188575734L;

    @DN
    private String dn;

    @AttributeName(name = "displayName")
    private String displayName;

    @BinaryData
    @AttributeName(name = "jansData")
    private byte[] data;

    @BinaryData
    @AttributeName(name = "jansDataStr")
    private byte[] dataStr;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getDataStr() {
        return dataStr;
    }

    public void setDataStr(byte[] dataStr) {
        this.dataStr = dataStr;
    }

    @Override
    public String toString() {
        return String.format("SimpleBinaryEntry [dn=%s, displayName=%s, data=%s bytes, dataStr=%s bytes]", dn, displayName,
                (data == null) ? null : data.length, (dataStr == null) ? null : dataStr.length);
    }

}
