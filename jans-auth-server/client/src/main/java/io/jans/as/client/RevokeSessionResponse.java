/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.session.EndSessionErrorResponseType;

import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RevokeSessionResponse extends BaseResponseWithErrors<EndSessionErrorResponseType> {

    public RevokeSessionResponse() {
    }

    public RevokeSessionResponse(Response clientResponse) {
        super(clientResponse);
        injectDataFromJson();
    }

    @Override
    public EndSessionErrorResponseType fromString(String params) {
        return EndSessionErrorResponseType.fromString(params);
    }

    public void injectDataFromJson() {
        injectDataFromJson(entity);
    }
}
