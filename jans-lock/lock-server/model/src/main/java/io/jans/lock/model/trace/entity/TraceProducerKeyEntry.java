/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * A registered producer Ed25519 key (design §6, TRACE MVP design decisions T-4, D-4). Keys are
 * create-only; only {@link #getRevokedAt()} may change after creation, via {@code merge}.
 *
 * @author Yuriy Movchan
 */
@DataEntry
@ObjectClass(value = "jansTraceProducerKey")
public class TraceProducerKeyEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 515416078300388861L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansTraceDomainId", ignoreDuringUpdate = true)
    private String domainId;

    @AttributeName(name = "jansTraceProducerId", ignoreDuringUpdate = true)
    private String producerId;

    @AttributeName(name = "jansTraceKid", ignoreDuringUpdate = true)
    private String kid;

    @JsonObject
    @AttributeName(name = "jansTracePublicKeyJwk", ignoreDuringUpdate = true)
    private Map<String, String> publicKeyJwk;

    @AttributeName(name = "jansTraceValidFrom", ignoreDuringUpdate = true)
    private Date validFrom;

    @AttributeName(name = "jansTraceValidUntil", ignoreDuringUpdate = true)
    private Date validUntil;

    @AttributeName(name = "jansTraceRevokedAt")
    private Date revokedAt;

    @AttributeName(name = "jansTraceRegisteredBy", ignoreDuringUpdate = true)
    private String registeredBy;

    @AttributeName(name = "creationDate", ignoreDuringUpdate = true)
    private Date creationDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public Map<String, String> getPublicKeyJwk() {
        return publicKeyJwk;
    }

    public void setPublicKeyJwk(Map<String, String> publicKeyJwk) {
        this.publicKeyJwk = publicKeyJwk;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public Date getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Date revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    // -- millisecond convenience accessors ---------------------------------------------------
    //
    // Not persisted (no @AttributeName; jans-orm's BasicPropertyAnnotationResolver scans
    // declared *fields*, never getters, so these are invisible to persistence). They exist so
    // callers that reason about validity windows in epoch milliseconds (design decision D-4:
    // ProducerKeyRegistry.resolveForVerification) don't have to convert java.util.Date at every
    // call site.

    /**
     * @return {@link #getValidFrom()} in epoch milliseconds, or {@code 0} if not set
     */
    public long getValidFromMs() {
        return validFrom == null ? 0L : validFrom.getTime();
    }

    /**
     * @return {@link #getValidUntil()} in epoch milliseconds, or {@code null} for no expiry
     */
    public Long getValidUntilMs() {
        return validUntil == null ? null : validUntil.getTime();
    }

    /**
     * @return {@link #getRevokedAt()} in epoch milliseconds, or {@code null} if not revoked
     */
    public Long getRevokedAtMs() {
        return revokedAt == null ? null : revokedAt.getTime();
    }

    /**
     * @return {@link #getCreationDate()} in epoch milliseconds, or {@code 0} if not set
     */
    public long getCreatedAtMs() {
        return creationDate == null ? 0L : creationDate.getTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TraceProducerKeyEntry that = (TraceProducerKeyEntry) o;
        return Objects.equals(id, that.id) && Objects.equals(domainId, that.domainId)
                && Objects.equals(producerId, that.producerId) && Objects.equals(kid, that.kid)
                && Objects.equals(publicKeyJwk, that.publicKeyJwk) && Objects.equals(validFrom, that.validFrom)
                && Objects.equals(validUntil, that.validUntil) && Objects.equals(revokedAt, that.revokedAt)
                && Objects.equals(registeredBy, that.registeredBy) && Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, domainId, producerId, kid, publicKeyJwk, validFrom, validUntil, revokedAt,
                registeredBy, creationDate);
    }

    @Override
    public String toString() {
        return "TraceProducerKeyEntry [id=" + id + ", domainId=" + domainId + ", producerId=" + producerId + ", kid="
                + kid + ", validFrom=" + validFrom + ", validUntil=" + validUntil + ", revokedAt=" + revokedAt
                + ", registeredBy=" + registeredBy + ", creationDate=" + creationDate + "]";
    }

}
