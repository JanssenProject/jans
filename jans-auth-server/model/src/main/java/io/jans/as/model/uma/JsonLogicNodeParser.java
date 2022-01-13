/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import io.jans.as.model.util.Util;

/**
 * @author yuriyz
 */
public class JsonLogicNodeParser {

    private JsonLogicNodeParser() {
    }

    public static JsonLogicNode parseNode(String json) {
        try {
            return Util.createJsonMapper().readValue(json, JsonLogicNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isNodeValid(String json) {
        JsonLogicNode node = parseNode(json);
        return node != null && node.isValid();
    }
}
