package io.jans.kc.api.config.client.model;

import io.jans.config.api.client.model.JansAttribute;


public class JansAttributeRepresentation {
    
    private JansAttribute jansAttribute;

    public JansAttributeRepresentation(JansAttribute jansAttribute) {

        this.jansAttribute = jansAttribute;
    }

    public String getInum() {

        return jansAttribute.getInum();
    }

    public String getSourceAttribute() {

        return jansAttribute.getSourceAttribute();
    }

    public String getNameIdType() {

        return jansAttribute.getNameIdType();
    }

    public String getName() {

        return jansAttribute.getName();
    }

    public String getDisplayName() {

        return jansAttribute.getDisplayName();
    }

    public String getDescription() {

        return jansAttribute.getDescription();
    }

    public String getSaml2Uri() {

        return jansAttribute.getSaml2Uri();
    }

}
