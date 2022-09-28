/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.ssa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@DataEntry(sortBy = {"creationDate"})
@ObjectClass(value = "jansSsa")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ssa extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -6832496019942067971L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "orgId")
    private Long orgId;

    @AttributeName(name = "expiration")
    private Date expiration;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "softwareId")
    private String softwareId;

    @AttributeName(name = "softwareRoles")
    private List<String> softwareRoles;

    @AttributeName(name = "grantTypes")
    private List<String> grantTypes;

    @JsonObject
    @AttributeName(name = "customAttributes")
    private Map<String, String> customAttributes;

    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();

    @AttributeName(name = "clientDn")
    private String clientDn;

    @AttributeName(name = "oneTimeUse")
    private Boolean oneTimeUse;

    @AttributeName(name = "rotateSsa")
    private Boolean rotateSsa;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public List<String> getSoftwareRoles() {
        return softwareRoles;
    }

    public void setSoftwareRoles(List<String> softwareRoles) {
        this.softwareRoles = softwareRoles;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getClientDn() {
        return clientDn;
    }

    public void setClientDn(String clientDn) {
        this.clientDn = clientDn;
    }

    public Boolean getOneTimeUse() {
        return oneTimeUse;
    }

    public void setOneTimeUse(Boolean oneTimeUse) {
        this.oneTimeUse = oneTimeUse;
    }

    public Boolean getRotateSsa() {
        return rotateSsa;
    }

    public void setRotateSsa(Boolean rotateSsa) {
        this.rotateSsa = rotateSsa;
    }

    @Override
    public String toString() {
        return "Ssa{" +
                "id='" + id + '\'' +
                ", orgId=" + orgId +
                ", expiration=" + expiration +
                ", description='" + description + '\'' +
                ", softwareId='" + softwareId + '\'' +
                ", softwareRoles=" + softwareRoles +
                ", grantTypes=" + grantTypes +
                ", customAttributes=" + customAttributes +
                ", creationDate=" + creationDate +
                ", clientDn='" + clientDn + '\'' +
                ", oneTimeUse=" + oneTimeUse +
                ", rotateSsa=" + rotateSsa +
                '}';
    }
}