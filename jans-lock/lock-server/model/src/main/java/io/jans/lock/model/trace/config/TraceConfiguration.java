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
 * limits .
 *
 * @author Yuriy Movchan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceConfiguration {

    public static final int DEFAULT_LATENESS_THRESHOLD_SECONDS = 300;
    public static final int DEFAULT_POLICY_MISMATCH_GRACE_SECONDS = 300;
    public static final int DEFAULT_POLICY_RELEASE_RETENTION_DAYS = 30;
    public static final int DEFAULT_MAX_FUTURE_SKEW_SECONDS = 300;
    public static final int DEFAULT_MAX_REQUEST_BYTES = 262144;
    public static final int DEFAULT_MAX_JSON_DEPTH = 32;
    public static final int DEFAULT_MAX_STRING_LENGTH = 8192;
    public static final int DEFAULT_MAX_ARRAY_LENGTH = 256;
    public static final int DEFAULT_MAX_OBJECT_MEMBERS = 256;
    public static final int DEFAULT_RECEIPT_ALLOCATION_RETRY_LIMIT = 8;
    public static final int DEFAULT_CORRELATION_RETRY_LIMIT = 3;
    public static final int DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS = 300;
    public static final int DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS = 120;
    public static final int DEFAULT_MAX_BULK_RECORDS = 100;
    public static final int DEFAULT_MAX_BULK_REQUEST_BYTES = 4_194_304;
    public static final String DEFAULT_CHAIN_REGISTRATION_MODE = "PRE_REGISTERED";
    public static final String DEFAULT_CRYPTO_PROVIDER = "JAVA";
    /** Ceiling {@link #getMaxBulkRequestBytes()} clamps to regardless of configuration, so a
     *  careless value can never make the bulk endpoint buffer an unbounded amount of memory per
     *  request (CWE-770). Not itself configurable. */
    public static final int HARD_MAX_BULK_REQUEST_BYTES = 16_777_216;

    @DocProperty(description = "Enable TRACE evidence ingestion endpoints", defaultValue = "true")
    @Schema(description = "Enable TRACE evidence ingestion endpoints")
    private boolean enabled = true;

    @DocProperty(description = "Evidence domain id used when a submitting client has no explicit binding")
    @Schema(description = "Evidence domain id used when a submitting client has no explicit binding")
    private String defaultEvidenceDomainId;

    @DocProperty(description = "Producer ids a client without an explicit binding may submit records for in the default evidence domain; empty means none")
    @Schema(description = "Producer ids a client without an explicit binding may submit records for in the default evidence domain; empty means none")
    private List<String> defaultAllowedProducerIds = new ArrayList<>();

    @DocProperty(description = "OAuth client to evidence-domain bindings")
    @Schema(description = "OAuth client to evidence-domain bindings")
    private List<TraceClientDomainBinding> clientDomainBindings = new ArrayList<>();

    @DocProperty(description = "Seconds after signed_at a record is accepted before being flagged late", defaultValue = "300")
    @Schema(description = "Seconds after signed_at a record is accepted before being flagged late")
    private int latenessThresholdSeconds = DEFAULT_LATENESS_THRESHOLD_SECONDS;

    @DocProperty(description = "Seconds after the last Lock node stops serving a policy store release during which records judged under it are not flagged policy_mismatch_flag; the check uses the larger of this and latenessThresholdSeconds, plus about 130 seconds for update and cache lag", defaultValue = "300")
    @Schema(description = "Seconds after the last Lock node stops serving a policy store release during which records judged under it are not flagged policy_mismatch_flag; the check uses the larger of this and latenessThresholdSeconds, plus about 130 seconds for update and cache lag")
    private int policyMismatchGraceSeconds = DEFAULT_POLICY_MISMATCH_GRACE_SECONDS;

    @DocProperty(description = "Days a policy store release no node serves any more is kept for the policy release check before it is deleted; never less than the check's acceptance window", defaultValue = "30")
    @Schema(description = "Days a policy store release no node serves any more is kept for the policy release check before it is deleted; never less than the check's acceptance window")
    private int policyReleaseRetentionDays = DEFAULT_POLICY_RELEASE_RETENTION_DAYS;

    @DocProperty(description = "Seconds a signed_at may be ahead of Lock's clock before the record is rejected", defaultValue = "300")
    @Schema(description = "Seconds a signed_at may be ahead of Lock's clock before the record is rejected")
    private int maxFutureSkewSeconds = DEFAULT_MAX_FUTURE_SKEW_SECONDS;

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

    @DocProperty(description = "Maximum retries when a correlation neighbor update hits a version conflict", defaultValue = "3")
    @Schema(description = "Maximum retries when a correlation neighbor update hits a version conflict")
    private int correlationRetryLimit = DEFAULT_CORRELATION_RETRY_LIMIT;

    @DocProperty(description = "Interval in seconds between receipt repair timer runs", defaultValue = "300")
    @Schema(description = "Interval in seconds between receipt repair timer runs")
    private int receiptRepairIntervalSeconds = DEFAULT_RECEIPT_REPAIR_INTERVAL_SECONDS;

    @DocProperty(description = "Seconds a PENDING receipt may stay unresolved before the repair timer settles it", defaultValue = "120")
    @Schema(description = "Seconds a PENDING receipt may stay unresolved before the repair timer settles it")
    private int pendingReceiptTimeoutSeconds = DEFAULT_PENDING_RECEIPT_TIMEOUT_SECONDS;

    @DocProperty(description = "Maximum number of assertions accepted in one POST /audit/trace/bulk request", defaultValue = "100")
    @Schema(description = "Maximum number of assertions accepted in one POST /audit/trace/bulk request")
    private int maxBulkRecords = DEFAULT_MAX_BULK_RECORDS;

    @DocProperty(description = "Maximum total body size, in bytes, buffered for one POST /audit/trace/bulk request; clamped to 16777216 regardless of this value", defaultValue = "4194304")
    @Schema(description = "Maximum total body size, in bytes, buffered for one POST /audit/trace/bulk request; clamped to 16777216 regardless of this value")
    private int maxBulkRequestBytes = DEFAULT_MAX_BULK_REQUEST_BYTES;

    @DocProperty(description = "How producer chains come to exist: PRE_REGISTERED (admin API only) or FIRST_OBSERVED (a genesis record of an unregistered chain registers it); an unknown value is treated as PRE_REGISTERED", defaultValue = "PRE_REGISTERED")
    @Schema(description = "How producer chains come to exist: PRE_REGISTERED (admin API only) or FIRST_OBSERVED (a genesis record of an unregistered chain registers it); an unknown value is treated as PRE_REGISTERED")
    private String chainRegistrationMode = DEFAULT_CHAIN_REGISTRATION_MODE;

    @DocProperty(description = "TRACE cryptography implementation: JAVA (BouncyCastle, in-process) or NATIVE (jans-trace-core through the jans-trace-java binding); an unknown value is treated as JAVA", defaultValue = "JAVA")
    @Schema(description = "TRACE cryptography implementation: JAVA (BouncyCastle, in-process) or NATIVE (jans-trace-core through the jans-trace-java binding); an unknown value is treated as JAVA", defaultValue = "JAVA")
    private String cryptoProvider = DEFAULT_CRYPTO_PROVIDER;

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

    public List<String> getDefaultAllowedProducerIds() {
        return defaultAllowedProducerIds;
    }

    public void setDefaultAllowedProducerIds(List<String> defaultAllowedProducerIds) {
        this.defaultAllowedProducerIds = defaultAllowedProducerIds;
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

    public int getPolicyMismatchGraceSeconds() {
        return policyMismatchGraceSeconds >= 0 ? policyMismatchGraceSeconds : DEFAULT_POLICY_MISMATCH_GRACE_SECONDS;
    }

    public void setPolicyMismatchGraceSeconds(int policyMismatchGraceSeconds) {
        this.policyMismatchGraceSeconds = policyMismatchGraceSeconds;
    }

    public int getPolicyReleaseRetentionDays() {
        return policyReleaseRetentionDays > 0 ? policyReleaseRetentionDays : DEFAULT_POLICY_RELEASE_RETENTION_DAYS;
    }

    public void setPolicyReleaseRetentionDays(int policyReleaseRetentionDays) {
        this.policyReleaseRetentionDays = policyReleaseRetentionDays;
    }

    public int getMaxFutureSkewSeconds() {
        return maxFutureSkewSeconds > 0 ? maxFutureSkewSeconds : DEFAULT_MAX_FUTURE_SKEW_SECONDS;
    }

    public void setMaxFutureSkewSeconds(int maxFutureSkewSeconds) {
        this.maxFutureSkewSeconds = maxFutureSkewSeconds;
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

    public int getCorrelationRetryLimit() {
        return correlationRetryLimit > 0 ? correlationRetryLimit : DEFAULT_CORRELATION_RETRY_LIMIT;
    }

    public void setCorrelationRetryLimit(int correlationRetryLimit) {
        this.correlationRetryLimit = correlationRetryLimit;
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

    public int getMaxBulkRecords() {
        return maxBulkRecords > 0 ? maxBulkRecords : DEFAULT_MAX_BULK_RECORDS;
    }

    public void setMaxBulkRecords(int maxBulkRecords) {
        this.maxBulkRecords = maxBulkRecords;
    }

    /**
     * @return the configured value (falling back to the default when {@code <= 0}), clamped to
     *         {@link #HARD_MAX_BULK_REQUEST_BYTES} so misconfiguration can never make the bulk
     *         endpoint buffer an unbounded amount of memory for one request
     */
    public int getMaxBulkRequestBytes() {
        int configured = maxBulkRequestBytes > 0 ? maxBulkRequestBytes : DEFAULT_MAX_BULK_REQUEST_BYTES;
        return Math.min(configured, HARD_MAX_BULK_REQUEST_BYTES);
    }

    public void setMaxBulkRequestBytes(int maxBulkRequestBytes) {
        this.maxBulkRequestBytes = maxBulkRequestBytes;
    }

    public String getChainRegistrationMode() {
        return chainRegistrationMode == null || chainRegistrationMode.trim().isEmpty()
                ? DEFAULT_CHAIN_REGISTRATION_MODE
                : chainRegistrationMode;
    }

    public void setChainRegistrationMode(String chainRegistrationMode) {
        this.chainRegistrationMode = chainRegistrationMode;
    }

    public String getCryptoProvider() {
        return cryptoProvider == null || cryptoProvider.trim().isEmpty()
                ? DEFAULT_CRYPTO_PROVIDER
                : cryptoProvider;
    }

    public void setCryptoProvider(String cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    @Override
    public String toString() {
        return "TraceConfiguration [enabled=" + enabled + ", defaultEvidenceDomainId=" + defaultEvidenceDomainId
                + ", defaultAllowedProducerIds=" + defaultAllowedProducerIds + ", clientDomainBindings=" + clientDomainBindings + ", latenessThresholdSeconds="
                + getLatenessThresholdSeconds() + ", policyMismatchGraceSeconds=" + getPolicyMismatchGraceSeconds()
                + ", policyReleaseRetentionDays=" + getPolicyReleaseRetentionDays()
                + ", maxFutureSkewSeconds=" + getMaxFutureSkewSeconds()
                + ", maxRequestBytes=" + getMaxRequestBytes() + ", maxJsonDepth="
                + getMaxJsonDepth() + ", maxStringLength=" + getMaxStringLength() + ", maxArrayLength="
                + getMaxArrayLength() + ", maxObjectMembers=" + getMaxObjectMembers()
                + ", receiptAllocationRetryLimit=" + getReceiptAllocationRetryLimit()
                + ", correlationRetryLimit=" + getCorrelationRetryLimit()
                + ", receiptRepairIntervalSeconds=" + getReceiptRepairIntervalSeconds()
                + ", pendingReceiptTimeoutSeconds=" + getPendingReceiptTimeoutSeconds() + ", maxBulkRecords="
                + getMaxBulkRecords() + ", maxBulkRequestBytes=" + getMaxBulkRequestBytes()
                + ", chainRegistrationMode=" + getChainRegistrationMode()
                + ", cryptoProvider=" + getCryptoProvider() + "]";
    }

}
