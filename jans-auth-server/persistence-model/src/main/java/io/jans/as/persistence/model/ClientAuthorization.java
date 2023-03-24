/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Javier Rojas Blum
 * @version October 16, 2015
 */
@DataEntry
@ObjectClass(value = "jansClntAuthz")
public class ClientAuthorization implements Serializable {

    @DN
    private String dn;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "jansClntId", consistency = true)
    private String clientId;

    @AttributeName(name = "jansUsrId", consistency = true)
    private String userId;

    @AttributeName(name = "jansScope")
    private String[] scopes;

    @AttributeName(name = "exp")
    private Date expirationDate;

    @AttributeName(name = "del")
    private boolean deletable = true;

    @Expiration
    private Integer ttl;

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientAuthorization that = (ClientAuthorization) o;

        if (!dn.equals(that.dn)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = dn.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
