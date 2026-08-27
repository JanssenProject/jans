/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */


package io.jans.shibboleth.plugin.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown =  true)
public class ShibbolethPluginConfiguration implements Configuration {
    
    private String trustRelationshipsDn;
    private String stagedFilesDn;
    private String trustActivationEpisodesDn;
    private String trustActivationLeasesDn;
    private String trustActivationWorkItemsDn;
    private String trustActivationWorkersDn;

    
    public String getTrustRelationshipsDn() {

        return trustRelationshipsDn;
    }

    public void setTrustRelationshipsDn(String trustRelationshipsDn) {

        this.trustRelationshipsDn = trustRelationshipsDn;
    }

    public String getStagedFilesDn() {

        return stagedFilesDn;
    }

    public void setStagedFilesDn(String stagedFilesDn) {

        this.stagedFilesDn = stagedFilesDn;
    }

    public String getTrustActivationEpisodesDn() {

        return trustActivationEpisodesDn;
    }

    public void setTrustActivationEpisodesDn(String trustActivationEpisodesDn) {

        this.trustActivationEpisodesDn = trustActivationEpisodesDn;
    }

    public String getTrustActivationLeasesDn() {

        return trustActivationLeasesDn;
    }

    public void setTrustActivationLeasesDn(String trustActivationLeasesDn) {

        this.trustActivationLeasesDn = trustActivationLeasesDn;
    }

    public String getTrustActivationWorkItemsDn() {

        return trustActivationWorkItemsDn;
    }

    public void setTrustActivationWorkItemsDn(String trustActivationWorkItemsDn) {

        this.trustActivationWorkItemsDn = trustActivationWorkItemsDn;
    }

    public String getTrustActivationWorkersDn() {

        return trustActivationWorkersDn;
    }

    public void setTrustActivationWorkersDn(String trustActivationWorkersDn) {

        this.trustActivationWorkersDn = trustActivationWorkersDn;
    }

    @Override
    public String toString() {

        return "ShibbolethPluginConfiguration [\n" +
            "trustRelationshipsDn=" + trustRelationshipsDn + ",\n" + 
            "stagedFilesDn=" + stagedFilesDn + ",\n" +
            "trustActivationEpisodesDn=" + trustActivationEpisodesDn + ",\n" +
            "trustActivationLeasesDn=" + trustActivationLeasesDn + ",\n" +
            "trustActivationWorkItemsDn=" + trustActivationWorkItemsDn + ",\n" + 
            "trustActivationWorkersDn=" + trustActivationWorkersDn + "\n" +
        "]";
    }
}
