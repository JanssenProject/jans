package io.jans.kc.api.config.client.model;

import io.jans.config.api.client.model.TrustRelationship;
import io.jans.config.api.client.model.SAMLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class JansTrustRelationship {

    private static final Pattern  ATTRIBUTE_DN_PATTERN = Pattern.compile("^inum=([A-F0-9]+),ou=attributes,o=jans$");
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

    public List<String> getReleasedAttributesInums() {

        List<String> ret = new ArrayList<String>();
        for(String attributedn : tr.getReleasedAttributes()) {
            Matcher matcher = ATTRIBUTE_DN_PATTERN.matcher(attributedn);
            if(matcher.matches()) {
                try {
                    ret.add(matcher.group(1));
                }catch(IllegalStateException | IndexOutOfBoundsException ignored) {
                    
                } 
            }
        }
        return ret;
    }
}
