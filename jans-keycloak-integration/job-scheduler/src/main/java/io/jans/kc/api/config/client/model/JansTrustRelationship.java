package io.jans.kc.api.config.client.model;

import io.jans.config.api.client.model.TrustRelationship;
import io.jans.config.api.client.model.SAMLMetadata;

public class JansTrustRelationship {
    
    private TrustRelationship tr;

    public JansTrustRelationship(TrustRelationship tr) {

        this.tr = tr;
    }

    public String getInum() {

        return tr.getInum();
    }

    public boolean metadataIsFile() {

        return tr.getSpMetaDataSourceType() == TrustRelationship.SpMetaDataSourceTypeEnum.FILE;
    }

    public boolean metadataIsManual() {
        
        return tr.getSpMetaDataSourceType() == TrustRelationship.SpMetaDataSourceTypeEnum.MANUAL;
    }

    public SAMLMetadata getManualSamlMetadata() {

        return tr.getSamlMetadata();
    }
}
