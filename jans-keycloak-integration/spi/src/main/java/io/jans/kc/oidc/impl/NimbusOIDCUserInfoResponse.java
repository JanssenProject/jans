package io.jans.kc.oidc.impl;

import com.nimbusds.openid.connect.sdk.UserInfoResponse;

import io.jans.kc.oidc.OIDCUserInfoError;
import io.jans.kc.oidc.OIDCUserInfoResponse;

public class NimbusOIDCUserInfoResponse implements OIDCUserInfoResponse {

    private static final String USERNAME_CLAIM_NAME = "user_name";

    private UserInfoResponse userInfoResponse;

    public NimbusOIDCUserInfoResponse(UserInfoResponse userInfoResponse) {
        this.userInfoResponse = userInfoResponse;
    }

    public String username() {

        return (String) userInfoResponse.toSuccessResponse().getUserInfo().getClaim(USERNAME_CLAIM_NAME);
    }

    public String email() {

        return userInfoResponse.toSuccessResponse().getUserInfo().getEmailAddress();
    }

    @Override
    public boolean indicatesSuccess() {

        return userInfoResponse.indicatesSuccess();
    }

    @Override
    public OIDCUserInfoError error() {

        if(userInfoResponse.indicatesSuccess()) {
            return null;
        }

        return new OIDCUserInfoError(
            userInfoResponse.toErrorResponse().getErrorObject().getCode(),
            userInfoResponse.toErrorResponse().getErrorObject().getDescription(),
            userInfoResponse.toErrorResponse().getErrorObject().getHTTPStatusCode()
        );
    }
}