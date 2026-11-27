/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;
import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;
import io.jans.orm.model.base.DeletableEntity;

/**
 * Persistent attestation challenge (nonce). State transitions are compare-and-set on {@code jansVersion}.
 */
@DataEntry
@ObjectClass(value = "jansAttestChallenge")
public class AttestationChallenge extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = 6113460587911377742L;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "clnId")
    private String clientId;

    @AttributeName(name = "jansJkt")
    private String jkt;

    @AttributeName(name = "jansState")
    private String state;

    @AttributeName(name = "creationDate")
    private Date creationDate;

    @Version
    @AttributeName(name = "jansVersion", ignoreDuringUpdate = true)
    private Long version;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private AttestationChallengeAttributes attributes;

    public AttestationChallenge() {
        setDeletable(true);
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

    public String getJkt() {
        return jkt;
    }

    public void setJkt(String jkt) {
        this.jkt = jkt;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public AttestationChallengeAttributes getAttributes() {
        if (attributes == null) attributes = new AttestationChallengeAttributes();
        return attributes;
    }

    public void setAttributes(AttestationChallengeAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "AttestationChallenge{id='" + id + "', clientId='" + clientId + "', jkt='" + jkt + "', state='" + state
                + "', version=" + version + ", attributes=" + attributes + "}";
    }
}
