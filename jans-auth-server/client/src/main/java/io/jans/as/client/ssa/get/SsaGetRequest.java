/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.get;

import io.jans.as.client.BaseRequest;
import io.jans.as.model.common.AuthorizationMethod;
import jakarta.ws.rs.core.MediaType;

public class SsaGetRequest extends BaseRequest {

    private String accessToken;

    public SsaGetRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String getQueryString() {
        return null;
    }
}
