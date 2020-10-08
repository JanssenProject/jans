package io.jans.as.rp.ciba;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CibaCallback {

    @JsonProperty("auth_req_id")
    private String authReqId;

    public CibaCallback() {

    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    @Override
    public String toString() {
        return "CibaCallback{" +
                "authReqId='" + authReqId + '\'' +
                '}';
    }

}
