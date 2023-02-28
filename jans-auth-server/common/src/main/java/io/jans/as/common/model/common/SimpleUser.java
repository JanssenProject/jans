/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.model.common;

import io.jans.as.model.exception.InvalidClaimException;
import org.json.JSONArray;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version May 3, 2019
 */
public class SimpleUser extends io.jans.orm.model.base.SimpleUser {

    private static final long serialVersionUID = -2634191420188575733L;

    public Object getAttribute(String attributeName, boolean optional, boolean multivalued) throws InvalidClaimException {
        Object attribute = null;

        List<Object> values = getAttributeObjectValues(attributeName);
        if (values != null) {
            if (multivalued) {
                JSONArray array = new JSONArray();
                for (Object v : values) {
                    array.put(v);
                }
                attribute = array;
            } else {
                attribute = values.get(0);
            }
        }

        if (attribute != null) {
            return attribute;
        } else if (optional) {
            return attribute;
        } else {
            throw new InvalidClaimException("The claim " + attributeName + " was not found.");
        }
    }

}