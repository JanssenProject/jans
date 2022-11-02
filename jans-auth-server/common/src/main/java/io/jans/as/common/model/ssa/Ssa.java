/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.ssa;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.common.CreatorType;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.Date;

@DataEntry(sortBy = {"creationDate"})
@ObjectClass(value = "jansSsa")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ssa extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -6832496019942067971L;

    @JsonProperty("inum")
    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private SsaAttributes attributes;

    @AttributeName(name = "o")
    private String orgId;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "jansState")
    private SsaState state;

    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();

    @AttributeName(name = "creatorId")
    private String creatorId;

    @AttributeName(name = "creatorTyp")
    private CreatorType creatorType;

    @Expiration
    private Integer ttl;

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public SsaAttributes getAttributes() {
        if (attributes == null) {
            attributes = new SsaAttributes();
        }
        return attributes;
    }

    public void setAttributes(SsaAttributes attributes) {
        this.attributes = attributes;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public CreatorType getCreatorType() {
        return creatorType;
    }

    public void setCreatorType(CreatorType creatorType) {
        this.creatorType = creatorType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SsaState getState() {
        return state;
    }

    public void setState(SsaState state) {
        this.state = state;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        return "Ssa{" +
                "id='" + id + '\'' +
                ", attributes=" + attributes +
                ", orgId='" + orgId + '\'' +
                ", description='" + description + '\'' +
                ", state='" + state + '\'' +
                ", creationDate=" + creationDate +
                ", creatorId='" + creatorId + '\'' +
                ", creatorType=" + creatorType +
                "} " + super.toString();
    }
}