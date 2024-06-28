package io.jans.kc.model;

import io.jans.kc.model.internal.JansPerson;
import io.jans.model.GluuStatus;
import io.jans.model.JansAttribute;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

public class JansUserAttributeModel {
    
    private final JansAttribute jansAttribute;
    private final JansPerson jansPerson;

    public JansUserAttributeModel(final JansAttribute jansAttribute, final JansPerson jansPerson) {

        this.jansAttribute = jansAttribute;
        this.jansPerson = jansPerson;
    }

    public boolean isActive() {

        return jansAttribute.getStatus() == GluuStatus.ACTIVE;
    }

    public AttributeType asSamlKeycloakAttribute() {

        List<String> values = jansPerson.customAttributeValues(jansAttribute.getName());
        if(values == null) {

            return null;
        }
        String samlAttributeName = jansAttribute.getSaml2Uri();
        String samlAttributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get();
        if(StringUtils.isEmpty(samlAttributeName)) {
            samlAttributeName = jansAttribute.getName();
            samlAttributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get();
        }
        AttributeType ret = new AttributeType(samlAttributeName);
        ret.setNameFormat(samlAttributeNameFormat);
        if(!StringUtils.isEmpty(jansAttribute.getDisplayName())) {
            ret.setFriendlyName(jansAttribute.getDisplayName());
        }else {
            ret.setFriendlyName(jansAttribute.getName());
        }

        values.forEach(ret::addAttributeValue);
        return ret;
    }
}
