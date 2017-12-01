package org.xdi.oxauth.model.uma;

import org.xdi.oxauth.model.util.Util;

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
