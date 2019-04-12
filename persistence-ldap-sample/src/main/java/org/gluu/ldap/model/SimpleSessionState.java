/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.ldap.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Transient;

import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.LdapDN;
import org.gluu.persist.annotation.LdapEntry;
import org.gluu.persist.annotation.LdapJsonObject;
import org.gluu.persist.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version December 15, 2015
 */
@Entry
@ObjectClass(values = { "top", "oxAuthSessionId" })
public class SimpleSessionState implements Serializable {

    private static final long serialVersionUID = -237476411915686378L;

    @DN
    private String dn;

    @Attribute(name = "uniqueIdentifier")
    private String id;

    @Attribute(name = "oxLastAccessTime")
    private Date lastUsedAt;

    @Attribute(name = "oxAuthUserDN")
    private String userDn;

    @Attribute(name = "oxAuthAuthenticationTime")
    private Date authenticationTime;

    @Attribute(name = "oxAuthSessionState")
    private Boolean permissionGranted;

    @Attribute(name = "oxAsJwt")
    private Boolean isJwt = false;

    @Attribute(name = "oxJwt")
    private String jwt;

    @JsonObject
    @Attribute(name = "oxAuthSessionAttribute")
    private Map<String, String> sessionAttributes;

    @Transient
    private transient boolean persisted;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Boolean getIsJwt() {
        return isJwt;
    }

    public void setIsJwt(Boolean isJwt) {
        this.isJwt = isJwt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getLastUsedAt() {
        return lastUsedAt != null ? new Date(lastUsedAt.getTime()) : null;
    }

    public void setLastUsedAt(Date lastUsedAt) {
        this.lastUsedAt = lastUsedAt != null ? new Date(lastUsedAt.getTime()) : null;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String userDn) {
        this.userDn = userDn != null ? userDn : "";
    }

    public Date getAuthenticationTime() {
        return authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime != null ? new Date(authenticationTime.getTime()) : null;
    }

    public Boolean getPermissionGranted() {
        return permissionGranted;
    }

    public void setPermissionGranted(Boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    public Map<String, String> getSessionAttributes() {
        if (sessionAttributes == null) {
            sessionAttributes = new HashMap<String, String>();
        }
        return sessionAttributes;
    }

    public void setSessionAttributes(Map<String, String> sessionAttributes) {
        this.sessionAttributes = sessionAttributes;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SimpleSessionState id1 = (SimpleSessionState) o;

        return !(id != null ? !id.equals(id1.id) : id1.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SessionState");
        sb.append(", dn='").append(dn).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", isJwt=").append(isJwt);
        sb.append(", lastUsedAt=").append(lastUsedAt);
        sb.append(", userDn='").append(userDn).append('\'');
        sb.append(", authenticationTime=").append(authenticationTime);
        sb.append(", permissionGranted=").append(permissionGranted);
        sb.append(", sessionAttributes=").append(sessionAttributes);
        sb.append(", persisted=").append(persisted);
        sb.append('}');
        return sb.toString();
    }

}
