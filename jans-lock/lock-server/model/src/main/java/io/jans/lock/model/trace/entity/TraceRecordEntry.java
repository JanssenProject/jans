/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * One accepted TRACE record (design §10, TRACE MVP design decision T-1). Every attribute is
 * immutable once persisted, except the three Lock-derived correlation flags
 * ({@link #getCoverageGap()}, {@link #getChainLinkFailure()}, {@link #getEquivocation()}), which
 * neighbor updates may set later (design decision D-9).
 *
 * @author Yuriy Movchan
 */
@DataEntry(sortByName = "jansTraceReceiptSeq")
@ObjectClass(value = "jansTraceRecord")
public class TraceRecordEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 4402843210702268320L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansTraceDomainId", ignoreDuringUpdate = true)
    private String domainId;

    @AttributeName(name = "jansTraceProducerId", ignoreDuringUpdate = true)
    private String producerId;

    @AttributeName(name = "jansTraceRecordId", ignoreDuringUpdate = true)
    private String recordId;

    @AttributeName(name = "jansTraceEventKind", ignoreDuringUpdate = true)
    private String eventKind;

    @AttributeName(name = "jansTraceSignedAt", ignoreDuringUpdate = true)
    private Long signedAt;

    @AttributeName(name = "jansTraceExecKey", ignoreDuringUpdate = true)
    private String execKey;

    @AttributeName(name = "jansTraceExecId", ignoreDuringUpdate = true)
    private String execId;

    @AttributeName(name = "jansTraceExecAuthority", ignoreDuringUpdate = true)
    private String execAuthority;

    @AttributeName(name = "jansTraceChainKey", ignoreDuringUpdate = true)
    private String chainKey;

    @AttributeName(name = "jansTraceChainPosKey", ignoreDuringUpdate = true)
    private String chainPosKey;

    @AttributeName(name = "jansTraceSeqNum", ignoreDuringUpdate = true)
    private Long seqNum;

    @AttributeName(name = "jansTracePrevRecordHash", ignoreDuringUpdate = true)
    private String prevRecordHash;

    @AttributeName(name = "jansTraceCapKeys", ignoreDuringUpdate = true)
    private List<String> capKeys;

    @AttributeName(name = "jansTraceTokenKeys", ignoreDuringUpdate = true)
    private List<String> tokenKeys;

    @AttributeName(name = "jansTraceAssertion", ignoreDuringUpdate = true)
    private String assertion;

    @AttributeName(name = "jansTraceContentDigest", ignoreDuringUpdate = true)
    private String contentDigest;

    @JsonObject
    @AttributeName(name = "jansTraceVerification", ignoreDuringUpdate = true)
    private TraceVerification verification;

    @AttributeName(name = "jansTraceReceiptSeq", ignoreDuringUpdate = true)
    private Long receiptSeq;

    @AttributeName(name = "jansTraceReceivedAt", ignoreDuringUpdate = true)
    private Date receivedAt;

    @AttributeName(name = "jansTraceReceivedAtMs", ignoreDuringUpdate = true)
    private Long receivedAtMs;

    @AttributeName(name = "jansTracePrevReceiptHash", ignoreDuringUpdate = true)
    private String prevReceiptHash;

    @AttributeName(name = "jansTraceReceiptHash", ignoreDuringUpdate = true)
    private String receiptHash;

    @AttributeName(name = "jansTraceCoverageGap")
    private Boolean coverageGap;

    @AttributeName(name = "jansTraceChainLinkFailure")
    private Boolean chainLinkFailure;

    @AttributeName(name = "jansTraceEquivocation")
    private Boolean equivocation;

    @AttributeName(name = "jansTraceLate", ignoreDuringUpdate = true)
    private Boolean late;

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

    public String getEventKind() {
        return eventKind;
    }

    public void setEventKind(String eventKind) {
        this.eventKind = eventKind;
    }

    public Long getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Long signedAt) {
        this.signedAt = signedAt;
    }

    public String getExecKey() {
        return execKey;
    }

    public void setExecKey(String execKey) {
        this.execKey = execKey;
    }

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

    public String getExecAuthority() {
        return execAuthority;
    }

    public void setExecAuthority(String execAuthority) {
        this.execAuthority = execAuthority;
    }

    public String getChainKey() {
        return chainKey;
    }

    public void setChainKey(String chainKey) {
        this.chainKey = chainKey;
    }

    public String getChainPosKey() {
        return chainPosKey;
    }

    public void setChainPosKey(String chainPosKey) {
        this.chainPosKey = chainPosKey;
    }

    public Long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(Long seqNum) {
        this.seqNum = seqNum;
    }

    public String getPrevRecordHash() {
        return prevRecordHash;
    }

    public void setPrevRecordHash(String prevRecordHash) {
        this.prevRecordHash = prevRecordHash;
    }

    public List<String> getCapKeys() {
        return capKeys;
    }

    public void setCapKeys(List<String> capKeys) {
        this.capKeys = capKeys;
    }

    public List<String> getTokenKeys() {
        return tokenKeys;
    }

    public void setTokenKeys(List<String> tokenKeys) {
        this.tokenKeys = tokenKeys;
    }

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public TraceVerification getVerification() {
        return verification;
    }

    public void setVerification(TraceVerification verification) {
        this.verification = verification;
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

    public Boolean getCoverageGap() {
        return coverageGap;
    }

    public void setCoverageGap(Boolean coverageGap) {
        this.coverageGap = coverageGap;
    }

    public Boolean getChainLinkFailure() {
        return chainLinkFailure;
    }

    public void setChainLinkFailure(Boolean chainLinkFailure) {
        this.chainLinkFailure = chainLinkFailure;
    }

    public Boolean getEquivocation() {
        return equivocation;
    }

    public void setEquivocation(Boolean equivocation) {
        this.equivocation = equivocation;
    }

    public Boolean getLate() {
        return late;
    }

    public void setLate(Boolean late) {
        this.late = late;
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
        TraceRecordEntry that = (TraceRecordEntry) o;
        return Objects.equals(id, that.id) && Objects.equals(domainId, that.domainId)
                && Objects.equals(producerId, that.producerId) && Objects.equals(recordId, that.recordId)
                && Objects.equals(eventKind, that.eventKind) && Objects.equals(signedAt, that.signedAt)
                && Objects.equals(execKey, that.execKey) && Objects.equals(execId, that.execId)
                && Objects.equals(execAuthority, that.execAuthority) && Objects.equals(chainKey, that.chainKey)
                && Objects.equals(chainPosKey, that.chainPosKey) && Objects.equals(seqNum, that.seqNum)
                && Objects.equals(prevRecordHash, that.prevRecordHash) && Objects.equals(capKeys, that.capKeys)
                && Objects.equals(tokenKeys, that.tokenKeys) && Objects.equals(assertion, that.assertion)
                && Objects.equals(contentDigest, that.contentDigest)
                && Objects.equals(verification, that.verification) && Objects.equals(receiptSeq, that.receiptSeq)
                && Objects.equals(receivedAt, that.receivedAt) && Objects.equals(receivedAtMs, that.receivedAtMs)
                && Objects.equals(prevReceiptHash, that.prevReceiptHash)
                && Objects.equals(receiptHash, that.receiptHash) && Objects.equals(coverageGap, that.coverageGap)
                && Objects.equals(chainLinkFailure, that.chainLinkFailure)
                && Objects.equals(equivocation, that.equivocation) && Objects.equals(late, that.late)
                && Objects.equals(nodeId, that.nodeId) && Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, domainId, producerId, recordId, eventKind, signedAt, execKey, execId, execAuthority,
                chainKey, chainPosKey, seqNum, prevRecordHash, capKeys, tokenKeys, assertion, contentDigest,
                verification, receiptSeq, receivedAt, receivedAtMs, prevReceiptHash, receiptHash, coverageGap,
                chainLinkFailure, equivocation, late, nodeId, creationDate);
    }

    @Override
    public String toString() {
        return "TraceRecordEntry [id=" + id + ", domainId=" + domainId + ", producerId=" + producerId + ", recordId="
                + recordId + ", eventKind=" + eventKind + ", signedAt=" + signedAt + ", execKey=" + execKey
                + ", execId=" + execId + ", execAuthority=" + execAuthority + ", chainKey=" + chainKey
                + ", chainPosKey=" + chainPosKey + ", seqNum=" + seqNum + ", prevRecordHash=" + prevRecordHash
                + ", capKeys=" + capKeys + ", tokenKeys=" + tokenKeys + ", contentDigest=" + contentDigest
                + ", verification=" + verification + ", receiptSeq=" + receiptSeq + ", receivedAt=" + receivedAt
                + ", receivedAtMs=" + receivedAtMs + ", prevReceiptHash=" + prevReceiptHash + ", receiptHash="
                + receiptHash + ", coverageGap=" + coverageGap + ", chainLinkFailure=" + chainLinkFailure
                + ", equivocation=" + equivocation + ", late=" + late + ", nodeId=" + nodeId + ", creationDate="
                + creationDate + "]";
    }

}
