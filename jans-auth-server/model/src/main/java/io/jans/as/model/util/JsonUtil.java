package io.jans.as.model.util;

import org.json.JSONObject;

/**
 * @author Yuriy Z
 */
public class JsonUtil {

    private JsonUtil() {
    }

    /**
     * Returns a json object representation of the given JSONObject,
     * filtering out any keys whose value is null (i.e. JSONObject.NULL).
     *
     * @param json the original JSONObject
     * @return filtered JSONObject
     */
    public static JSONObject filterOutNulls(JSONObject json) {
        JSONObject filtered = new JSONObject();
        // Iterate over the keys and copy only non-null values
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (!JSONObject.NULL.equals(value)) {
                filtered.put(key, value);
            }
        }
        return filtered;
    }

    /**
     * Returns a string representation of the given JSONObject,
     * filtering out any keys whose value is null (i.e. JSONObject.NULL).
     *
     * @param json the original JSONObject
     * @return the string representation of the filtered JSONObject
     */
    public static String toStringWithoutNulls(JSONObject json) {
        return filterOutNulls(json).toString();
    }
}
