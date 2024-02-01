package io.jans.kc.api.config.client.model;

import io.jans.config.api.client.model.TrustRelationship;

public class JansTrustRelationship {
    
    private TrustRelationship tr;

    public JansTrustRelationship(TrustRelationship tr) {

        this.tr = tr;
    }

    public String getInum() {

        return this.tr.getInum();
    }
}
