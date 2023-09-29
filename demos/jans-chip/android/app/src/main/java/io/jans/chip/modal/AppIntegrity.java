package io.jans.chip.modal;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.annotations.SerializedName;

import io.jsonwebtoken.lang.Strings;

public class AppIntegrity {

    @SerializedName("response")
    private JSONObject response;
    @SerializedName("error")
    private String error;

    public JSONObject getResponse() {
        return response;
    }

    public void setResponseString(String responseString) {
        if (responseString != null && responseString.length() > 0) {
            try {
                JSONObject jo = new JSONObject(responseString);
                setResponse(jo);
            } catch (JSONException e) {
                setResponse(null);
                //throw new RuntimeException(e);
            }
        }
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "AppIntegrity{" +
                "response=" + response +
                ", error='" + error + '\'' +
                '}';
    }
}
