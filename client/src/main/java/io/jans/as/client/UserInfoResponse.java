/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.userinfo.UserInfoErrorResponseType;
import org.jboss.resteasy.client.ClientResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an user info response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 11.30.2011
 */
public class UserInfoResponse extends BaseResponseWithErrors<UserInfoErrorResponseType> {

    private Map<String, List<String>> claims;

    /**
     * Constructs a User Info response.
     *
     * @param clientResponse The response status code.
     */
    public UserInfoResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
        claims = new HashMap<>();
    }

    @Override
    public UserInfoErrorResponseType fromString(String str) {
        return UserInfoErrorResponseType.fromString(str);
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public List<String> getClaim(String claimName) {
        if (claims.containsKey(claimName)) {
            return claims.get(claimName);
        }

        return null;
    }

    @Override
    public String toString() {
        return "UserInfoResponse{" +
                "status=" + status +
                "entity=" + entity +
                "headers=" + headers +
                "claims=" + claims +
                ", super=" + super.toString() +
                '}';
    }
}