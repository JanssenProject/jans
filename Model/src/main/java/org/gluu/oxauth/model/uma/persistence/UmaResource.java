/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma.persistence;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Resource description.
 *
 * @author Yuriy Zabrovarnyy Date: 10/03/2012
 */
@DataEntry
@ObjectClass(values = {"top", "oxUmaResource"})
public class UmaResource {

    @DN
    private String dn;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "oxId")
    private String id;

    @NotNull(message = "Display name should be not empty")
    @AttributeName(name = "displayName")
    private String name;

    @AttributeName(name = "oxFaviconImage")
    private String iconUri;

    @AttributeName(name = "oxAuthUmaScope")
    private List<String> scopes;

    @AttributeName(name = "oxScopeExpression")
    private String scopeExpression;

    @AttributeName(name = "oxAssociatedClient")
    private List<String> clients;

    @AttributeName(name = "oxResource")
    private List<String> resources;

    @AttributeName(name = "oxRevision")
    private String rev;

    @AttributeName(name = "owner")
    private String creator;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "oxType")
    private String type;

    @AttributeName(name = "oxAuthCreation")
    private Date creationDate;

    @AttributeName(name = "oxAuthExpiration")
    private Date expirationDate;

    @AttributeName(name = "oxDeletable")
    private boolean deletable = true;

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public String getScopeExpression() {
        return scopeExpression;
    }

    public void setScopeExpression(String scopeExpression) {
        this.scopeExpression = scopeExpression;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getClients() {
        if (clients == null) {
            clients = new ArrayList<String>();
        }
        return clients;
    }

    public void setClients(List<String> p_clients) {
        clients = p_clients;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @JsonIgnore
    public boolean isExpired() {
        return expirationDate != null && new Date().after(expirationDate);
    }

    @Override
    public String toString() {
        return "UmaResource{" +
                "dn='" + dn + '\'' +
                ", inum='" + inum + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", scopes=" + scopes +
                ", scopeExpression='" + scopeExpression + '\'' +
                ", clients=" + clients +
                ", resources=" + resources +
                ", rev='" + rev + '\'' +
                ", creator='" + creator + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", creationDate=" + creationDate +
                ", expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                '}';
    }
}
