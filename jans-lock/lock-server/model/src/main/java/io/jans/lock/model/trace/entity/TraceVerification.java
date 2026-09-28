/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Signature verification outcome stored alongside a TRACE record (design §10, TRACE MVP design
 * decisions T-1). {@code verifiedAtMs} is stored as epoch milliseconds; conversion to an RFC 3339
 * string happens in the REST layer.
 *
 * @author Yuriy Movchan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceVerification implements Serializable {

    private static final long serialVersionUID = -3406103023802549259L;

    @JsonProperty("signature_valid")
    private boolean signatureValid;

    @JsonProperty("key_id")
    private String keyId;

    @JsonProperty("verified_at")
    private long verifiedAtMs;

    @JsonProperty("algorithm")
    private String algorithm;

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public long getVerifiedAtMs() {
        return verifiedAtMs;
    }

    public void setVerifiedAtMs(long verifiedAtMs) {
        this.verifiedAtMs = verifiedAtMs;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TraceVerification that = (TraceVerification) o;
        return signatureValid == that.signatureValid && verifiedAtMs == that.verifiedAtMs
                && Objects.equals(keyId, that.keyId) && Objects.equals(algorithm, that.algorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signatureValid, keyId, verifiedAtMs, algorithm);
    }

    @Override
    public String toString() {
        return "TraceVerification [signatureValid=" + signatureValid + ", keyId=" + keyId + ", verifiedAtMs="
                + verifiedAtMs + ", algorithm=" + algorithm + "]";
    }

}
