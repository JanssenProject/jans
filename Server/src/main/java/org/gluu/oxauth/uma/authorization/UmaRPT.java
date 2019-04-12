/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.authorization;

import java.util.Date;
import java.util.List;

import org.gluu.oxauth.model.common.AbstractToken;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * Requesting Party Token.
 *
 * @author Yuriy Movchan Date: 10/16/2012
 */
@DataEntry
@ObjectClass(values = {"top", "oxAuthUmaRPT"})
public class UmaRPT extends AbstractToken {

    @DN
    private String dn;
    @AttributeName(name = "uniqueIdentifier")
    private String id;
    @AttributeName(name = "oxAuthUserId")
    private String userId;
    @AttributeName(name = "oxAuthClientId")
    private String clientId;
    @AttributeName(name = "oxUmaPermission")
    private List<String> permissions;

    public UmaRPT() {
        super(1);
    }

    public UmaRPT(String code, Date creationDate, Date expirationDate, String userId, String clientId) {
        super(code, creationDate, expirationDate);
        this.userId = userId;
        this.clientId = clientId;
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

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
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
                ", id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", permissions=" + permissions +
                "} " + super.toString();
    }
}
