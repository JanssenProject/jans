/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.entity;

import java.io.Serializable;
import java.util.Objects;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;
import io.jans.orm.model.base.BaseEntry;

/**
 * A policy store archive release some Lock node has served, shared by every node. Not scoped to
 * an evidence domain, because policy stores are configured for the whole Lock deployment.
 */
@DataEntry
@ObjectClass(value = "jansTracePolicyRelease")
public class TracePolicyReleaseEntry extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 4218760953155427103L;

    @AttributeName(name = "jansId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "jansTracePolicyStoreId", ignoreDuringUpdate = true)
    private String policyStoreId;

    @AttributeName(name = "jansTraceBundleHash", ignoreDuringUpdate = true)
    private String bundleHash;

    @AttributeName(name = "jansTracePolicyStoreVersion")
    private String policyStoreVersion;

    @AttributeName(name = "jansTraceFirstServedAtMs")
    private Long firstServedAtMs;

    @AttributeName(name = "jansTraceLastServedAtMs")
    private Long lastServedAtMs;

    @Version
    @AttributeName(name = "jansVersion", ignoreDuringUpdate = true)
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyStoreId() {
        return policyStoreId;
    }

    public void setPolicyStoreId(String policyStoreId) {
        this.policyStoreId = policyStoreId;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public String getPolicyStoreVersion() {
        return policyStoreVersion;
    }

    public void setPolicyStoreVersion(String policyStoreVersion) {
        this.policyStoreVersion = policyStoreVersion;
    }

    public Long getFirstServedAtMs() {
        return firstServedAtMs;
    }

    public void setFirstServedAtMs(Long firstServedAtMs) {
        this.firstServedAtMs = firstServedAtMs;
    }

    public Long getLastServedAtMs() {
        return lastServedAtMs;
    }

    public void setLastServedAtMs(Long lastServedAtMs) {
        this.lastServedAtMs = lastServedAtMs;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TracePolicyReleaseEntry)) {
            return false;
        }
        TracePolicyReleaseEntry that = (TracePolicyReleaseEntry) o;
        return Objects.equals(id, that.id) && Objects.equals(policyStoreId, that.policyStoreId)
                && Objects.equals(bundleHash, that.bundleHash)
                && Objects.equals(policyStoreVersion, that.policyStoreVersion)
                && Objects.equals(firstServedAtMs, that.firstServedAtMs)
                && Objects.equals(lastServedAtMs, that.lastServedAtMs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, policyStoreId, bundleHash, policyStoreVersion, firstServedAtMs, lastServedAtMs);
    }

    @Override
    public String toString() {
        return "TracePolicyReleaseEntry [id=" + id + ", policyStoreId=" + policyStoreId + ", bundleHash=" + bundleHash
                + ", policyStoreVersion=" + policyStoreVersion + ", firstServedAtMs=" + firstServedAtMs
                + ", lastServedAtMs=" + lastServedAtMs + "]";
    }

}
