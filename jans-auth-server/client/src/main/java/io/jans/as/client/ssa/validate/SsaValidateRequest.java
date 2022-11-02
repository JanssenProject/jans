/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ssa.validate;

import io.jans.as.client.BaseRequest;
import io.jans.as.model.common.AuthorizationMethod;
import jakarta.ws.rs.core.MediaType;

public class SsaValidateRequest extends BaseRequest {

    private String jti;

    public SsaValidateRequest() {
        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @Override
    public String getQueryString() {
        return null;
    }
}
