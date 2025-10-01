package io.jans.as.client.ssa.get;

import org.json.JSONObject;

public class SsaGetJson {

    private String jti;

    private JSONObject jsonObject;

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @Override
    public String toString() {
        return "SsaDTO{" +
                "jti='" + jti + '\'' +
                ", jsonObject=" + jsonObject +
                '}';
    }
}
