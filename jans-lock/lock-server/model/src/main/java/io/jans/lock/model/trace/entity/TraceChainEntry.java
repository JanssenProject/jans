/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * A registered producer chain (design §8, TRACE MVP design decision T-3). Immutable once
 * persisted.
 *
 * @author Yuriy Movchan
 */
@DataEntry
@ObjectClass(value = "jansTraceChain")
public class TraceChainEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -1748469212323484330L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansTraceDomainId", ignoreDuringUpdate = true)
    private String domainId;

    @AttributeName(name = "jansTraceProducerId", ignoreDuringUpdate = true)
    private String producerId;

    @AttributeName(name = "jansTraceProducerInstanceId", ignoreDuringUpdate = true)
    private String producerInstanceId;

    @AttributeName(name = "jansTraceProducerChainId", ignoreDuringUpdate = true)
    private String producerChainId;

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

    public String getProducerInstanceId() {
        return producerInstanceId;
    }

    public void setProducerInstanceId(String producerInstanceId) {
        this.producerInstanceId = producerInstanceId;
    }

    public String getProducerChainId() {
        return producerChainId;
    }

    public void setProducerChainId(String producerChainId) {
        this.producerChainId = producerChainId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TraceChainEntry that = (TraceChainEntry) o;
        return Objects.equals(id, that.id) && Objects.equals(domainId, that.domainId)
                && Objects.equals(producerId, that.producerId)
                && Objects.equals(producerInstanceId, that.producerInstanceId)
                && Objects.equals(producerChainId, that.producerChainId)
                && Objects.equals(registeredBy, that.registeredBy)
                && Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, domainId, producerId, producerInstanceId, producerChainId, registeredBy,
                creationDate);
    }

    @Override
    public String toString() {
        return "TraceChainEntry [id=" + id + ", domainId=" + domainId + ", producerId=" + producerId
                + ", producerInstanceId=" + producerInstanceId + ", producerChainId=" + producerChainId
                + ", registeredBy=" + registeredBy + ", creationDate=" + creationDate + "]";
    }

}
