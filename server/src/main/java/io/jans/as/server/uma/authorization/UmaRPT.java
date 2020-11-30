/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import io.jans.as.server.model.common.AbstractToken;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.util.Date;
import java.util.List;

/**
 * Requesting Party Token.
 *
 * @author Yuriy Movchan Date: 10/16/2012
 */
@DataEntry
@ObjectClass(value = "jansUmaRPT")
public class UmaRPT extends AbstractToken {

    @DN
    private String dn;
    @AttributeName(name = "usrId")
    private String userId;
    @AttributeName(name = "clnId")
    private String clientId;
    @AttributeName(name = "jansUmaPermission")
    private List<String> permissions;

    private String notHashedCode;

    public UmaRPT() {
        super(1);
    }

    public UmaRPT(String code, Date creationDate, Date expirationDate, String userId, String clientId) {
        super(code, creationDate, expirationDate);
        this.notHashedCode = getCode();
        this.userId = userId;
        this.clientId = clientId;
    }

    public String getNotHashedCode() {
        return notHashedCode;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> p_permissions) {
        permissions = p_permissions;
    }

    @Override
    public String toString() {
        return "UmaRPT{" +
                "dn='" + dn + '\'' +
                ", userId='" + userId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", permissions=" + permissions +
                "} " + super.toString();
    }
}
