/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;
import io.jans.orm.model.base.BaseEntry;

/**
 * The single authoritative attestation state of one {@code (client_id, jkt)}. Every write is a
 * compare-and-set on {@code revision} ({@code jansVersion}); {@code appraisalGeneration} orders evidence.
 */
@DataEntry
@ObjectClass(value = "jansKeyAttestState")
public class KeyAttestationState extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 2405311566409286353L;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "clnId")
    private String clientId;

    @AttributeName(name = "jansJkt")
    private String jkt;

    @AttributeName(name = "jansStatus")
    private String status;

    @Version
    @AttributeName(name = "jansVersion", ignoreDuringUpdate = true)
    private Long revision;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private KeyAttestationStateAttributes attributes;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }

    public KeyAttestationStateAttributes getAttributes() {
        if (attributes == null) attributes = new KeyAttestationStateAttributes();
        return attributes;
    }

    public void setAttributes(KeyAttestationStateAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "KeyAttestationState{id='" + id + "', clientId='" + clientId + "', jkt='" + jkt + "', status='" + status
                + "', revision=" + revision + ", attributes=" + attributes + "}";
    }
}
