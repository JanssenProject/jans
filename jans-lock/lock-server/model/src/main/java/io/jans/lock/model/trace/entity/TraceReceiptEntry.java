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
 * A receipt-chain position allocation claim (design §9, TRACE MVP design decisions T-2, D-8).
 * Only {@link #getReceiptState()} may change after creation, via {@code merge}.
 *
 * @author Yuriy Movchan
 */
@DataEntry
@ObjectClass(value = "jansTraceReceipt")
public class TraceReceiptEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 2333377125328866578L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansTraceDomainId", ignoreDuringUpdate = true)
    private String domainId;

    @AttributeName(name = "jansTraceReceiptSeq", ignoreDuringUpdate = true)
    private Long receiptSeq;

    @AttributeName(name = "jansTraceReceivedAt", ignoreDuringUpdate = true)
    private Date receivedAt;

    @AttributeName(name = "jansTraceReceivedAtMs", ignoreDuringUpdate = true)
    private Long receivedAtMs;

    @AttributeName(name = "jansTraceProducerId", ignoreDuringUpdate = true)
    private String producerId;

    @AttributeName(name = "jansTraceRecordId", ignoreDuringUpdate = true)
    private String recordId;

    @AttributeName(name = "jansTraceRecordKey", ignoreDuringUpdate = true)
    private String recordKey;

    @AttributeName(name = "jansTraceContentDigest", ignoreDuringUpdate = true)
    private String contentDigest;

    @AttributeName(name = "jansTracePrevReceiptHash", ignoreDuringUpdate = true)
    private String prevReceiptHash;

    @AttributeName(name = "jansTraceReceiptHash", ignoreDuringUpdate = true)
    private String receiptHash;

    @AttributeName(name = "jansTraceReceiptState")
    private String receiptState;

    @AttributeName(name = "jansTraceNodeId", ignoreDuringUpdate = true)
    private String nodeId;

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

    public Long getReceiptSeq() {
        return receiptSeq;
    }

    public void setReceiptSeq(Long receiptSeq) {
        this.receiptSeq = receiptSeq;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Long getReceivedAtMs() {
        return receivedAtMs;
    }

    public void setReceivedAtMs(Long receivedAtMs) {
        this.receivedAtMs = receivedAtMs;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public String getPrevReceiptHash() {
        return prevReceiptHash;
    }

    public void setPrevReceiptHash(String prevReceiptHash) {
        this.prevReceiptHash = prevReceiptHash;
    }

    public String getReceiptHash() {
        return receiptHash;
    }

    public void setReceiptHash(String receiptHash) {
        this.receiptHash = receiptHash;
    }

    public String getReceiptState() {
        return receiptState;
    }

    public void setReceiptState(String receiptState) {
        this.receiptState = receiptState;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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
        TraceReceiptEntry that = (TraceReceiptEntry) o;
        return Objects.equals(id, that.id) && Objects.equals(domainId, that.domainId)
                && Objects.equals(receiptSeq, that.receiptSeq) && Objects.equals(receivedAt, that.receivedAt)
                && Objects.equals(receivedAtMs, that.receivedAtMs) && Objects.equals(producerId, that.producerId)
                && Objects.equals(recordId, that.recordId) && Objects.equals(recordKey, that.recordKey)
                && Objects.equals(contentDigest, that.contentDigest)
                && Objects.equals(prevReceiptHash, that.prevReceiptHash)
                && Objects.equals(receiptHash, that.receiptHash) && Objects.equals(receiptState, that.receiptState)
                && Objects.equals(nodeId, that.nodeId) && Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, domainId, receiptSeq, receivedAt, receivedAtMs, producerId, recordId, recordKey,
                contentDigest, prevReceiptHash, receiptHash, receiptState, nodeId, creationDate);
    }

    @Override
    public String toString() {
        return "TraceReceiptEntry [id=" + id + ", domainId=" + domainId + ", receiptSeq=" + receiptSeq
                + ", receivedAt=" + receivedAt + ", receivedAtMs=" + receivedAtMs + ", producerId=" + producerId
                + ", recordId=" + recordId + ", recordKey=" + recordKey + ", contentDigest=" + contentDigest
                + ", prevReceiptHash=" + prevReceiptHash + ", receiptHash=" + receiptHash + ", receiptState="
                + receiptState + ", nodeId=" + nodeId + ", creationDate=" + creationDate + "]";
    }

}
