package io.jans.as.common.model.authzdetails;

import org.json.JSONObject;

/**
 * @author Yuriy Z
 */
public class AuthzDetail {

    private final JSONObject jsonObject;

    public AuthzDetail(String json) {
        this(new JSONObject(json));
    }

    public AuthzDetail(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public String getType() {
        return jsonObject.optString("type");
    }

    @Override
    public String toString() {
        return "AuthzDetail{" +
                "jsonObject=" + jsonObject +
                '}';
    }
}
