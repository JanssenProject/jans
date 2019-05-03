/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.gluu.oxauth.model.exception.InvalidClaimException;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @version May 3, 2019
 */
public class SimpleUser extends org.gluu.persist.model.base.SimpleUser {

    private static final long serialVersionUID = -2634191420188575733L;

    public Object getAttribute(String userAttribute, boolean optional, boolean multivalued) throws InvalidClaimException {
        Object attribute = null;

        for (org.gluu.persist.model.base.CustomAttribute customAttribute : customAttributes) {
            if (customAttribute.getName().equals(userAttribute)) {
                List<String> values = customAttribute.getValues();
                if (values != null) {
                    if (multivalued) {
                        JSONArray array = new JSONArray();
                        for (String v : values) {
                            array.put(v);
                        }
                        attribute = array;
                    } else {
                        attribute = values.get(0);
                    }
                }

                break;
            }
        }

        if (attribute != null) {
            return attribute;
        } else if (optional) {
            return attribute;
        } else {
            throw new InvalidClaimException("The claim " + userAttribute + " was not found.");
        }
    }

    public List<String> getAttributeValues(String ldapAttribute) {
        List<String> attributes = null;
        if (ldapAttribute != null && !ldapAttribute.isEmpty()) {
            for (CustomAttribute customAttribute : customAttributes) {
                if (StringHelper.equalsIgnoreCase(ldapAttribute, customAttribute.getName())) {
                    attributes = customAttribute.getValues();
                    break;
                }
            }
        }

        return attributes;
    }

}