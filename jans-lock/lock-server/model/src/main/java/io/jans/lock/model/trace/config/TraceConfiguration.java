/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * TRACE evidence-ingestion configuration: evidence-domain bindings, lateness policy and request
 * limits (design references TRACE MVP design decisions D-1, D-3, D-11, D-12).
 *
 * @author Yuriy Movchan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceConfiguration {

    public static final int DEFAULT_LATENESS_THRESHOLD_SECONDS = 300;
    public static final int DEFAULT_MAX_REQUEST_BYTES = 262144;
    public static final int DEFAULT_MAX_JSON_DEPTH = 32;
    public static final int DEFAULT_MAX_STRING_LENGTH = 8192;
    public static final int DEFAULT_MAX_ARRAY_LENGTH = 256;
    public static final int DEFAULT_MAX_OBJECT_MEMBERS = 256;
    public static final int DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT = 8;
    public static final int DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS = 300;
    public static final int DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS = 120;

    @DocProperty(description = "Enable TRACE evidence ingestion endpoints", defaultValue = "true")
    @Schema(description = "Enable TRACE evidence ingestion endpoints")
    private boolean enabled = true;

    @DocProperty(description = "Evidence domain id used when a submitting client has no explicit binding")
    @Schema(description = "Evidence domain id used when a submitting client has no explicit binding")
    private String defaultEvidenceDomainId;

    @DocProperty(description = "OAuth client to evidence-domain bindings")
    @Schema(description = "OAuth client to evidence-domain bindings")
    private List<TraceClientDomainBinding> clientDomainBindings = new ArrayList<>();

    @DocProperty(description = "Seconds after signed_at a record is accepted before being flagged late", defaultValue = "300")
    @Schema(description = "Seconds after signed_at a record is accepted before being flagged late")
    private int latenessThresholdSeconds = DEFAULT_LATENESS_THRESHOLD_SECONDS;

    @DocProperty(description = "Maximum accepted TRACE request body size in bytes", defaultValue = "262144")
    @Schema(description = "Maximum accepted TRACE request body size in bytes")
    private int maxRequestBytes = DEFAULT_MAX_REQUEST_BYTES;

    @DocProperty(description = "Maximum accepted JSON nesting depth of a TRACE assertion", defaultValue = "32")
    @Schema(description = "Maximum accepted JSON nesting depth of a TRACE assertion")
    private int maxJsonDepth = DEFAULT_MAX_JSON_DEPTH;

    @DocProperty(description = "Maximum accepted JSON string length (UTF-16 units) in a TRACE assertion", defaultValue = "8192")
    @Schema(description = "Maximum accepted JSON string length (UTF-16 units) in a TRACE assertion")
    private int maxStringLength = DEFAULT_MAX_STRING_LENGTH;

    @DocProperty(description = "Maximum accepted JSON array length in a TRACE assertion", defaultValue = "256")
    @Schema(description = "Maximum accepted JSON array length in a TRACE assertion")
    private int maxArrayLength = DEFAULT_MAX_ARRAY_LENGTH;

    @DocProperty(description = "Maximum accepted JSON object member count in a TRACE assertion", defaultValue = "256")
    @Schema(description = "Maximum accepted JSON object member count in a TRACE assertion")
    private int maxObjectMembers = DEFAULT_MAX_OBJECT_MEMBERS;

    @DocProperty(description = "Maximum retries when allocating a receipt-chain sequence number", defaultValue = "8")
    @Schema(description = "Maximum retries when allocating a receipt-chain sequence number")
    private int receiptAllocationRetryLimit = DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT;

    @DocProperty(description = "Interval in seconds between receipt repair timer runs", defaultValue = "300")
    @Schema(description = "Interval in seconds between receipt repair timer runs")
    private int receiptRepairIntervalSeconds = DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS;

    @DocProperty(description = "Seconds a PENDING receipt may stay unresolved before the repair timer settles it", defaultValue = "120")
    @Schema(description = "Seconds a PENDING receipt may stay unresolved before the repair timer settles it")
    private int pendingReceiptTimeoutSeconds = DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultEvidenceDomainId() {
        return defaultEvidenceDomainId;
    }

    public void setDefaultEvidenceDomainId(String defaultEvidenceDomainId) {
        this.defaultEvidenceDomainId = defaultEvidenceDomainId;
    }

    public List<TraceClientDomainBinding> getClientDomainBindings() {
        return clientDomainBindings;
    }

    public void setClientDomainBindings(List<TraceClientDomainBinding> clientDomainBindings) {
        this.clientDomainBindings = clientDomainBindings;
    }

    public int getLatenessThresholdSeconds() {
        return latenessThresholdSeconds > 0 ? latenessThresholdSeconds : DEFAULT_LATENESS_THRESHOLD_SECONDS;
    }

    public void setLatenessThresholdSeconds(int latenessThresholdSeconds) {
        this.latenessThresholdSeconds = latenessThresholdSeconds;
    }

    public int getMaxRequestBytes() {
        return maxRequestBytes > 0 ? maxRequestBytes : DEFAULT_MAX_REQUEST_BYTES;
    }

    public void setMaxRequestBytes(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    public int getMaxJsonDepth() {
        return maxJsonDepth > 0 ? maxJsonDepth : DEFAULT_MAX_JSON_DEPTH;
    }

    public void setMaxJsonDepth(int maxJsonDepth) {
        this.maxJsonDepth = maxJsonDepth;
    }

    public int getMaxStringLength() {
        return maxStringLength > 0 ? maxStringLength : DEFAULT_MAX_STRING_LENGTH;
    }

    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    public int getMaxArrayLength() {
        return maxArrayLength > 0 ? maxArrayLength : DEFAULT_MAX_ARRAY_LENGTH;
    }

    public void setMaxArrayLength(int maxArrayLength) {
        this.maxArrayLength = maxArrayLength;
    }

    public int getMaxObjectMembers() {
        return maxObjectMembers > 0 ? maxObjectMembers : DEFAULT_MAX_OBJECT_MEMBERS;
    }

    public void setMaxObjectMembers(int maxObjectMembers) {
        this.maxObjectMembers = maxObjectMembers;
    }

    public int getReceiptAllocationRetryLimit() {
        return receiptAllocationRetryLimit > 0 ? receiptAllocationRetryLimit : DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT;
    }

    public void setReceiptAllocationRetryLimit(int receiptAllocationRetryLimit) {
        this.receiptAllocationRetryLimit = receiptAllocationRetryLimit;
    }

    public int getReceiptRepairIntervalSeconds() {
        return receiptRepairIntervalSeconds > 0 ? receiptRepairIntervalSeconds
                : DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS;
    }

    public void setReceiptRepairIntervalSeconds(int receiptRepairIntervalSeconds) {
        this.receiptRepairIntervalSeconds = receiptRepairIntervalSeconds;
    }

    public int getPendingReceiptTimeoutSeconds() {
        return pendingReceiptTimeoutSeconds > 0 ? pendingReceiptTimeoutSeconds
                : DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS;
    }

    public void setPendingReceiptTimeoutSeconds(int pendingReceiptTimeoutSeconds) {
        this.pendingReceiptTimeoutSeconds = pendingReceiptTimeoutSeconds;
    }

    @Override
    public String toString() {
        return "TraceConfiguration [enabled=" + enabled + ", defaultEvidenceDomainId=" + defaultEvidenceDomainId
                + ", clientDomainBindings=" + clientDomainBindings + ", latenessThresholdSeconds="
                + getLatenessThresholdSeconds() + ", maxRequestBytes=" + getMaxRequestBytes() + ", maxJsonDepth="
                + getMaxJsonDepth() + ", maxStringLength=" + getMaxStringLength() + ", maxArrayLength="
                + getMaxArrayLength() + ", maxObjectMembers=" + getMaxObjectMembers()
                + ", receiptAllocationRetryLimit=" + getReceiptAllocationRetryLimit()
                + ", receiptRepairIntervalSeconds=" + getReceiptRepairIntervalSeconds()
                + ", pendingReceiptTimeoutSeconds=" + getPendingReceiptTimeoutSeconds() + "]";
    }

}
