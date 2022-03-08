/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.userinfo.UserInfoErrorResponseType;

import jakarta.ws.rs.core.Response;

/**
 * Represents an user info response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 11.30.2011
 */
public class UserInfoResponse extends BaseResponseWithErrors<UserInfoErrorResponseType> {

    /**
     * Constructs a User Info response.
     *
     * @param clientResponse The response status code.
     */
    public UserInfoResponse(Response clientResponse) {
        super(clientResponse);
    }

    @Override
    public UserInfoErrorResponseType fromString(String str) {
        return UserInfoErrorResponseType.fromString(str);
    }

    @Override
    public String toString() {
        return "UserInfoResponse{" +
                "status=" + status +
                "entity=" + entity +
                "headers=" + headers +
                ", super=" + super.toString() +
                '}';
    }
}