package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("authorization_code")
    private String authorizationCode;

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
}
