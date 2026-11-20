/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.annotation.Version;
import io.jans.orm.model.base.BaseEntry;

/**
 * Monotonic per-key appraisal generation counter. Incremented by compare-and-set on {@code jansVersion}.
 */
@DataEntry
@ObjectClass(value = "jansKeyGeneration")
public class KeyGeneration extends BaseEntry implements Serializable {

    private static final long serialVersionUID = 7350546870451446906L;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "jansJkt")
    private String jkt;

    @AttributeName(name = "jansGeneration")
    private Long generation;

    @Version
    @AttributeName(name = "jansVersion", ignoreDuringUpdate = true)
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJkt() {
        return jkt;
    }

    public void setJkt(String jkt) {
        this.jkt = jkt;
    }

    public long getGeneration() {
        return generation == null ? 0L : generation;
    }

    public void setGeneration(Long generation) {
        this.generation = generation;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "KeyGeneration{id='" + id + "', jkt='" + jkt + "', generation=" + generation + ", version=" + version + "}";
    }
}
