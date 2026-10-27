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
 * Binds an OAuth client to a TRACE evidence domain and the producer ids it may submit records for.
 *
 * @author Yuriy Movchan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceClientDomainBinding {

    @DocProperty(description = "OAuth client id bound to the evidence domain")
    @Schema(description = "OAuth client id bound to the evidence domain")
    private String clientId;

    @DocProperty(description = "Evidence domain id the client is bound to")
    @Schema(description = "Evidence domain id the client is bound to")
    private String evidenceDomainId;

    @DocProperty(description = "Producer ids the client is allowed to submit records for; \"*\" allows any producer")
    @Schema(description = "Producer ids the client is allowed to submit records for; \"*\" allows any producer")
    private List<String> allowedProducerIds = new ArrayList<>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getEvidenceDomainId() {
        return evidenceDomainId;
    }

    public void setEvidenceDomainId(String evidenceDomainId) {
        this.evidenceDomainId = evidenceDomainId;
    }

    public List<String> getAllowedProducerIds() {
        return allowedProducerIds;
    }

    public void setAllowedProducerIds(List<String> allowedProducerIds) {
        this.allowedProducerIds = allowedProducerIds;
    }

    @Override
    public String toString() {
        return "TraceClientDomainBinding [clientId=" + clientId + ", evidenceDomainId=" + evidenceDomainId
                + ", allowedProducerIds=" + allowedProducerIds + "]";
    }

}
