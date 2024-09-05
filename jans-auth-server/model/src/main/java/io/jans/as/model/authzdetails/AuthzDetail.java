package io.jans.as.model.authzdetails;

import org.json.JSONObject;

/**
 * @author Yuriy Z
 */
public class AuthzDetail {

    private final JSONObject jsonObject;
    private String uiRepresentation;

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

    public String getUiRepresentation() {
        return uiRepresentation;
    }

    public void setUiRepresentation(String uiRepresentation) {
        this.uiRepresentation = uiRepresentation;
    }

    @Override
    public String toString() {
        return "AuthzDetail{" +
                "jsonObject=" + jsonObject +
                "uiRepresentation=" + uiRepresentation +
                '}';
    }
}
