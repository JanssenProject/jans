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
import io.jans.orm.model.base.DeletableEntity;

/**
 * Immutable attestation evidence log entry: one appraisal attempt. Never updated after insert; the
 * {@code exp} attribute is the retention deadline, not the authorization validity ({@code expiresAt}).
 */
@DataEntry
@ObjectClass(value = "jansAttest")
public class AttestationAssessment extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -2006335113468790416L;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "clnId")
    private String clientId;

    @AttributeName(name = "jansJkt")
    private String jkt;

    @AttributeName(name = "jansStatus")
    private String status;

    @AttributeName(name = "creationDate")
    private Date appraisedAt;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private AttestationAssessmentAttributes attributes;

    public AttestationAssessment() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getAppraisedAt() {
        return appraisedAt;
    }

    public void setAppraisedAt(Date appraisedAt) {
        this.appraisedAt = appraisedAt;
    }

    public AttestationAssessmentAttributes getAttributes() {
        if (attributes == null) attributes = new AttestationAssessmentAttributes();
        return attributes;
    }

    public void setAttributes(AttestationAssessmentAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "AttestationAssessment{id='" + id + "', clientId='" + clientId + "', jkt='" + jkt + "', status='" + status
                + "', appraisedAt=" + appraisedAt + ", attributes=" + attributes + "}";
    }
}
