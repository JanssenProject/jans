package io.jans.kc.api.config.client.model;

import io.jans.config.api.client.model.TrustRelationship;

public class JansTrustRelationship {
    
    private TrustRelationship tr;
    private InputStream metadata;

    public JansTrustRelationship(TrustRelationship tr, InputStream metadata) {

        this.tr = tr;
        this.metadata = metadata;
    }
}
