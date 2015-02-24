/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.xdi.util.ArrayHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version 0.9, 05/28/2013
 */
@Name("sessionUser")
@AutoCreate
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthSessionId"})
public class SessionId implements Serializable {

    @LdapDN
    private String dn;

    @LdapAttribute(name = "uniqueIdentifier")
    private String id;

    @LdapAttribute(name = "lastModifiedTime")
    private Date lastUsedAt;

    @LdapAttribute(name = "oxAuthUserDN")
    private String userDn;

    @LdapAttribute(name = "oxAuthAuthenticationTime")
    private Date authenticationTime;

    @LdapAttribute(name = "oxState")
    private String state = SessionIdState.UNAUTHENTICATED.getValue();

    @LdapAttribute(name = "oxAuthSessionState")
    private Boolean permissionGranted;

    @LdapJsonObject
    @LdapAttribute(name = "oxAuthPermissionGrantedMap")
    private SessionIdAccessMap permissionGrantedMap;

    @LdapJsonObject
    @LdapAttribute(name = "oxAuthSessionAttribute")
    private SessionIdAttribute[] sessionIdAttributes;

    public SessionId() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public SessionIdState state() {
        return SessionIdState.fromValue(state);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public Date getLastUsedAt() {
        return lastUsedAt != null ? new Date(lastUsedAt.getTime()) : null;
    }

    public void setLastUsedAt(Date p_lastUsedAt) {
        lastUsedAt = p_lastUsedAt != null ? new Date(p_lastUsedAt.getTime()) : null;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String p_userDn) {
        userDn = p_userDn;
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

    public SessionIdAccessMap getPermissionGrantedMap() {
        return permissionGrantedMap;
    }

    public void setPermissionGrantedMap(SessionIdAccessMap permissionGrantedMap) {
        this.permissionGrantedMap = permissionGrantedMap;
    }

    public Boolean isPermissionGrantedForClient(String clientId) {
        if (permissionGrantedMap != null) {
            return permissionGrantedMap.get(clientId);
        }
        return false;
    }

    public void addPermission(String clientId, Boolean granted) {
        if (permissionGrantedMap == null) {
            permissionGrantedMap = new SessionIdAccessMap();
        }
        permissionGrantedMap.put(clientId, granted);
    }

    public SessionIdAttribute[] getSessionIdAttributes() {
        return sessionIdAttributes;
    }

    public void setSessionIdAttributes(SessionIdAttribute[] sessionIdAttributes) {
        this.sessionIdAttributes = sessionIdAttributes;
    }

    public Map<String, String> attributes() {
        Map<String, String> map = new HashMap<String, String>();
        if (sessionIdAttributes != null) {
            for (SessionIdAttribute attr : sessionIdAttributes) {
                map.put(attr.getName(), attr.getValue());
            }
        }
        return map;
    }

    public void overrideAttributes(Map<String, String> maps) {
        if (maps != null && !maps.isEmpty()) {
            List<SessionIdAttribute> attributes = new ArrayList<SessionIdAttribute>();
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                attributes.add(new SessionIdAttribute(entry.getKey(), entry.getValue()));
            }
            setSessionIdAttributes(attributes.toArray(new SessionIdAttribute[attributes.size()]));
        }
    }

    public void addAttribute(String name, String value) {
        addAttribute(new SessionIdAttribute(name, value));
    }

    public void addAttribute(SessionIdAttribute attribute) {
        addAttribute(new SessionIdAttribute[]{attribute});
    }

    public void addAttribute(SessionIdAttribute[] attributes) {
        SessionIdAttribute[] sessionIdAttributes = getSessionIdAttributes();
        SessionIdAttribute[] newSessionIdAttributes;
        if (ArrayHelper.isEmpty(sessionIdAttributes)) {
            newSessionIdAttributes = attributes;
        } else {
            newSessionIdAttributes = ArrayHelper.arrayMerge(sessionIdAttributes, attributes);
        }

        setSessionIdAttributes(newSessionIdAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SessionId id1 = (SessionId) o;

        if (id != null ? !id.equals(id1.id) : id1.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SessionId [dn=").append(dn).append(", id=").append(id).append(", lastUsedAt=").append(lastUsedAt)
                .append(", userDn=").append(userDn).append(", authenticationTime=").append(authenticationTime)
                .append(", permissionGranted=").append(permissionGranted).append("]");
        return builder.toString();
    }

}
