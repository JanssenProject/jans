/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.userinfo.UserInfoErrorResponseType;

import jakarta.ws.rs.core.Response;

/**
 * Represents an client info response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
public class ClientInfoResponse extends BaseResponseWithErrors<UserInfoErrorResponseType> {

    /**
     * Constructs a Client Info response.
     *
     * @param clientResponse The response status code.
     */
    public ClientInfoResponse(Response clientResponse) {
        super(clientResponse);
    }

    @Override
    public UserInfoErrorResponseType fromString(String str) {
        return UserInfoErrorResponseType.fromString(str);
    }
}