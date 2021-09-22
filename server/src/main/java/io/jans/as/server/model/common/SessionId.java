/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Deletable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Named;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static io.jans.as.server.service.SessionIdService.OP_BROWSER_STATE;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version December 8, 2018
 */
@Named("sessionUser")
@DataEntry
@ObjectClass(value = "jansSessId")
public class SessionId implements Deletable, Serializable {

    public static final String OLD_SESSION_ID_ATTR_KEY = "old_session_id";

    private static final long serialVersionUID = -237476411915686378L;

    @DN
    private String dn;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "sid")
    private String outsideSid;

    @AttributeName(name = "jansLastAccessTime")
    private Date lastUsedAt;

    @AttributeName(name = "jansUsrDN")
    private String userDn;

    @AttributeName(name = "authnTime")
    private Date authenticationTime;

    @AttributeName(name = "jansState")
    private SessionIdState state;

    @AttributeName(name = "jansSessState")
    private String sessionState;

    @AttributeName(name = "jansPermissionGranted")
    private Boolean permissionGranted;

    @AttributeName(name = "jansAsJwt")
    private Boolean isJwt = false;

    @AttributeName(name = "jansJwt")
    private String jwt;

    @JsonObject
    @AttributeName(name = "jansPermissionGrantedMap")
    private SessionIdAccessMap permissionGrantedMap;

    @JsonObject
    @AttributeName(name = "jansSessAttr")
    private Map<String, String> sessionAttributes;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "del")
    private Boolean deletable = true;

    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();

    @Transient
    private transient boolean persisted;

    @Transient
    private User user;

    @Expiration
    private int ttl;

    public SessionId() {
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
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

    public SessionIdState getState() {
        return state;
    }

    public void setState(SessionIdState state) {
        this.state = state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getOPBrowserState() {
        return sessionAttributes.get(OP_BROWSER_STATE);
    }

    public String getId() {
        return id;
    }

    public void setId(String p_id) {
        id = p_id;
    }

    public Date getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Date p_lastUsedAt) {
        lastUsedAt = p_lastUsedAt;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String p_userDn) {
        userDn = p_userDn != null ? p_userDn : "";
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public Boolean getPermissionGranted() {
        return permissionGranted;
    }

    public void setPermissionGranted(Boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    public SessionIdAccessMap getPermissionGrantedMap() {
        if (permissionGrantedMap == null) {
            permissionGrantedMap = new SessionIdAccessMap();
        }
        return permissionGrantedMap;
    }

    public void setPermissionGrantedMap(SessionIdAccessMap permissionGrantedMap) {
        this.permissionGrantedMap = permissionGrantedMap;
    }

    public Boolean isPermissionGrantedForClient(String clientId) {
        return permissionGrantedMap != null && permissionGrantedMap.get(clientId);
    }

    public void addPermission(String clientId, Boolean granted) {
        if (permissionGrantedMap == null) {
            permissionGrantedMap = new SessionIdAccessMap();
        }
        permissionGrantedMap.put(clientId, granted);
    }

    @NotNull
    public Map<String, String> getSessionAttributes() {
        if (sessionAttributes == null) {
            sessionAttributes = Maps.newHashMap();
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

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean isDeletable() {
        return deletable != null ? deletable : true;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setOutsideSid(String outsideSid) {
        this.outsideSid = outsideSid;
    }

    public String getOutsideSid() {
        if (StringUtils.isBlank(outsideSid)) {
            outsideSid = UUID.randomUUID().toString();
        }
        return outsideSid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SessionId id1 = (SessionId) o;

        return !(id != null ? !id.equals(id1.id) : id1.id != null);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SessionId {");
        sb.append("dn='").append(dn).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", outsideSid='").append(outsideSid).append('\'');
        sb.append(", lastUsedAt=").append(lastUsedAt);
        sb.append(", userDn='").append(userDn).append('\'');
        sb.append(", authenticationTime=").append(authenticationTime);
        sb.append(", state=").append(state);
        sb.append(", expirationDate=").append(expirationDate);
        sb.append(", sessionState='").append(sessionState).append('\'');
        sb.append(", permissionGranted=").append(permissionGranted);
        sb.append(", isJwt=").append(isJwt);
        sb.append(", jwt=").append(jwt);
        sb.append(", permissionGrantedMap=").append(permissionGrantedMap);
        sb.append(", sessionAttributes=").append(sessionAttributes);
        sb.append(", persisted=").append(persisted);
        sb.append("}");
        return sb.toString();
    }
}
