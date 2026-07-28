/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A configured MDS endpoint, as reported by the MDS health endpoint. Carries only whether a
 * per-endpoint trust anchor is configured — the certificate itself is never exposed.
 *
 * @author Janssen Project
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataServerStatus {

    private String url;

    private boolean rootCertConfigured;

    public MetadataServerStatus() {
    }

    public MetadataServerStatus(String url, boolean rootCertConfigured) {
        this.url = url;
        this.rootCertConfigured = rootCertConfigured;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isRootCertConfigured() {
        return rootCertConfigured;
    }

    public void setRootCertConfigured(boolean rootCertConfigured) {
        this.rootCertConfigured = rootCertConfigured;
    }

    @Override
    public String toString() {
        return "MetadataServerStatus [url=" + url + ", rootCertConfigured=" + rootCertConfigured + "]";
    }
}
